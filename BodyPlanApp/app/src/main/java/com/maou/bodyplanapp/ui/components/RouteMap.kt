package com.maou.bodyplanapp.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.maou.bodyplanapp.data.RoutePoint

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RouteMap(points: List<RoutePoint>, sessionKey: String, finished: Boolean, modifier: Modifier = Modifier) {
    var web by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }
    var renderedKey by remember { mutableStateOf<String?>(null) }
    var count by remember { mutableIntStateOf(0) }
    var mapLost by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { web?.destroy(); web = null } }
    Box(modifier) {
        if (mapLost) Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("地图已暂停，运动记录仍在继续")
            TextButton(onClick = { mapLost = false }) { Text("重新加载地图") }
        } else AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.userAgentString = "BodyPlan/1.0 (Android fitness route viewer)"
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                            Log.e("BodyPlan", "scenario=map-script sessionKey=$sessionKey line=${message.lineNumber()} message=${message.message()}")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("BodyPlan", "scenario=map-page sessionKey=$sessionKey width=${view?.width} height=${view?.height}")
                        ready = true
                    }
                    @android.annotation.TargetApi(26)
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        Log.e("BodyPlan", "scenario=map-renderer sessionKey=$sessionKey didCrash=${detail?.didCrash()} priority=${detail?.rendererPriorityAtExit()}")
                        ready = false
                        renderedKey = null
                        count = 0
                        web = null
                        view?.destroy()
                        mapLost = true
                        return true
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url
                        if (uri?.scheme == "https" && uri.host == "www.openstreetmap.org") {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            catch (exception: Exception) { Log.e("BodyPlan", "scenario=map-attribution sessionKey=$sessionKey", exception) }
                        }
                        return true
                    }
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        Log.e("BodyPlan", "scenario=map-resource sessionKey=$sessionKey errorCode=${error?.errorCode} description=${error?.description}")
                    }
                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                        Log.e("BodyPlan", "scenario=map-resource sessionKey=$sessionKey httpStatus=${response?.statusCode}")
                    }
                }
                val html = context.assets.open("route-map.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val css = context.assets.open("leaflet/leaflet.css").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val js = context.assets.open("leaflet/leaflet.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
                loadDataWithBaseURL("https://appassets.androidplatform.net/", html.replace("/* LEAFLET_CSS */", css).replace("/* LEAFLET_JS */", js),
                    "text/html", "UTF-8", null)
                web = this
            }
        }, update = { view ->
            if (ready && (renderedKey != sessionKey || count != points.size)) {
                val reset = renderedKey != sessionKey || count > points.size
                val delta = if (reset) points else points.drop(count)
                val gson = Gson()
                view.evaluateJavascript("window.updateRoute(${gson.toJson(sessionKey)},${gson.toJson(delta)},$finished,$reset)", null)
                renderedKey = sessionKey
                count = points.size
            }
        })
        FilledTonalIconButton(onClick = { web?.evaluateJavascript("window.recenter()", null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp)) {
            Icon(Icons.Outlined.LocationOn, "回到当前位置")
        }
    }
}
