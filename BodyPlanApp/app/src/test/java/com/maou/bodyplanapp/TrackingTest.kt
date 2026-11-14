package com.maou.bodyplanapp

import com.google.gson.Gson
import com.maou.bodyplanapp.data.RoutePoint
import com.maou.bodyplanapp.tracking.LocalRun
import com.maou.bodyplanapp.tracking.TrackMath
import org.junit.Assert.*
import org.junit.Test

class TrackingTest {
    private fun point(lon: Double, time: Long, segment: Int = 0) = RoutePoint(0.0,lon,time,segment,5.0)
    @Test fun usesSameWgs84DistanceAsServer() {
        assertEquals(111.32,TrackMath.distance(point(0.0,1000),point(0.001,11000),"RUN"),0.2)
    }
    @Test fun neverConnectsPauseOrMissingGps() {
        assertEquals(0.0,TrackMath.distance(point(0.0,1000),point(0.001,11000,1),"RUN"),0.0)
        assertEquals(0.0,TrackMath.distance(point(0.0,1000),point(0.001,60000),"RUN"),0.0)
    }
    @Test fun rejectsTeleportationAndStationaryNoise() {
        assertEquals(0.0,TrackMath.distance(point(0.0,1000),point(1.0,3000),"RUN"),0.0)
        assertEquals(0.0,TrackMath.distance(point(0.0,1000),point(0.000001,3000),"RUN"),0.0)
    }
    @Test fun rideAndWalkHaveDifferentPlausibleSpeedLimits() {
        assertTrue(TrackMath.distance(point(0.0,1000),point(0.001,11000),"RIDE") > 100)
        assertEquals(0.0,TrackMath.distance(point(0.0,1000),point(0.001,11000),"WALK"),0.0)
    }
    @Test fun localDraftSurvivesJsonWithoutLosingGoalAndPause() {
        val run = LocalRun(42,"abcdefghijklmnop","RUN","INTERVAL",3000,360,true,1000,
            durationMs=60000,points=listOf(point(0.0,2000)),status="PAUSED",segment=2,signal="运动已暂停")
        val json = Gson().toJson(run)
        assertTrue(json.contains("运动已暂停"))
        assertFalse(json.contains("\\u8fd0"))
        assertEquals(run,Gson().fromJson(json,LocalRun::class.java))
    }
    @Test fun uploadUsesActiveTimeAndMarksSimulatedData() {
        val run = LocalRun(42,"abcdefghijklmnop","RUN","AEROBIC",100,360,true,1000,endedAt=121000,
            durationMs=60000,status="FINISHED",simulated=true)
        assertEquals(60,run.request().durationSec)
        assertEquals("模拟定位验证记录",run.request().remark)
        assertEquals(121000L,run.request().endedAt)
    }
    @Test fun formatsClockAndPace() {
        assertEquals("01:02:03",TrackMath.clock(3723))
        assertEquals("6′05″",TrackMath.pace(365))
        assertEquals("--",TrackMath.pace(null))
    }
}
