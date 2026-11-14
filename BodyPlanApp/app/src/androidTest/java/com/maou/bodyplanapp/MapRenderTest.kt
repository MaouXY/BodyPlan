package com.maou.bodyplanapp

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.maou.bodyplanapp.data.RoutePoint
import com.maou.bodyplanapp.ui.components.RouteMap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MapRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun web(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) web(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun basicWebViewCanPaintInCompose() {
        compose.setContent {
            AndroidView(modifier=Modifier.fillMaxSize(),factory={ context ->
                WebView(context).apply {
                    loadDataWithBaseURL("https://appassets.androidplatform.net/",
                        "<html><body style='margin:0;background:#16775d;color:white;font-size:30px'>地图渲染检查</body></html>","text/html","UTF-8",null)
                }
            })
        }
        Thread.sleep(5000)
        val bitmap=requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val pixel=bitmap.getPixel(bitmap.width/2,bitmap.height/2)
        File(compose.activity.getExternalFilesDir(null),"wellness-map-basic.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
        assertTrue("基础 WebView 没有绘制，color=$pixel",android.graphics.Color.red(pixel)<70 && android.graphics.Color.green(pixel) in 80..170)
    }
    @Test fun localMapInitializesAndDrawsRouteEvenBeforeTilesLoad() {
        val points = listOf(
            RoutePoint(39.9900,116.3200,1000,0,5.0),
            RoutePoint(39.9905,116.3200,5000,0,5.0),
            RoutePoint(39.9905,116.3205,9000,0,5.0))
        compose.setContent { MaterialTheme { RouteMap(points,"render-test",true,Modifier.fillMaxSize()) } }
        compose.waitForIdle()
        var result = ""
        for (attempt in 0 until 20) {
            val latch = CountDownLatch(1)
            compose.runOnUiThread {
                val view = web(compose.activity.window.decorView)
                if (view == null) { result="missing-webview"; latch.countDown() }
                else view.evaluateJavascript("""JSON.stringify({width:innerWidth,height:innerHeight,ready:document.readyState,
                    leaflet:typeof L,update:typeof window.updateRoute,paths:document.querySelectorAll('path.leaflet-interactive').length,
                    tiles:document.querySelectorAll('img.leaflet-tile-loaded').length,
                    bounds:document.getElementById('map')?.getBoundingClientRect().toJSON(),
                    css:document.getElementById('map')?[getComputedStyle(document.getElementById('map')).height,getComputedStyle(document.getElementById('map')).position]:[],
                    sheets:[...document.styleSheets].map(s=>s.cssRules.length),
                    body:document.body?.innerText.substring(0,250)})""") { result=it; latch.countDown() }
            }
            assertTrue("地图脚本响应超时", latch.await(5,TimeUnit.SECONDS))
            Log.i("BodyPlan","scenario=test-map-render result=$result")
            if (result.contains("\\\"paths\\\":3")) break
            Thread.sleep(500)
        }
        assertTrue(result, result.contains("\\\"leaflet\\\":\\\"object\\\""))
        assertTrue(result, result.contains("\\\"update\\\":\\\"function\\\""))
        assertTrue(result, result.contains("\\\"paths\\\":3"))
        Thread.sleep(3000)
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap=requireNotNull(automation.takeScreenshot())
        File(compose.activity.getExternalFilesDir(null),"wellness-map-render.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        var routePixels=0
        for (y in 150 until bitmap.height-150 step 3) for (x in 0 until bitmap.width step 3) {
            val pixel=bitmap.getPixel(x,y)
            val r=android.graphics.Color.red(pixel)
            val g=android.graphics.Color.green(pixel)
            val b=android.graphics.Color.blue(pixel)
            if(r<70 && g in 80..170 && b in 50..140) routePixels++
        }
        bitmap.recycle()
        assertTrue("地图 DOM 有轨迹，但截图未绘制轨迹，pixels=$routePixels result=$result",routePixels>20)
    }
}
