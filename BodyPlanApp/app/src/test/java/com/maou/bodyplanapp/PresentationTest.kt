package com.maou.bodyplanapp

import com.maou.bodyplanapp.data.TrainingPlan
import com.maou.bodyplanapp.data.PlanDay
import com.maou.bodyplanapp.data.PlanItem
import com.maou.bodyplanapp.ui.*
import org.junit.Assert.*
import org.junit.Test

class PresentationTest {
    @Test fun repeatedWeekdayIndexesAreGroupedByRealDatesAndRestDaysAreFilled() {
        val first = PlanDay(1, "2026-09-14", "周一", listOf(PlanItem(id = 1)))
        val second = PlanDay(1, "2026-09-21", "周一", listOf(PlanItem(id = 2)))
        val days = displayPlanDays(TrainingPlan(startDate = "2026-09-14", endDate = "2026-10-11",
            days = listOf(second, first)))
        assertEquals(28, days.size)
        assertEquals(first, days[0])
        assertEquals(second, days[7])
        assertTrue(days[1].items!!.isEmpty())
        assertEquals("周二", days[1].weekdayLabel)
        assertEquals(4, days.chunked(7).size)
    }

    @Test fun activePlanAfterEndDateIsShownAsEnded() {
        val plan = TrainingPlan(status = "ACTIVE", endDate = "2026-10-11")
        assertTrue(planExpired(plan, "2026-11-13"))
        assertEquals("已结束", planStatusLabel(plan, "2026-11-13"))
    }

    @Test fun lastTrainingDateIsStillCurrent() {
        val plan = TrainingPlan(status = "ACTIVE", startDate = "2026-11-01", endDate = "2026-11-13")
        assertFalse(planExpired(plan, "2026-11-13"))
        assertEquals("进行中", planStatusLabel(plan, "2026-11-13"))
    }

    @Test fun historyPreservesReplacementStatusAndFutureStart() {
        assertEquals("已更新", planStatusLabel(TrainingPlan(status = "REPLACED", endDate = "2026-10-11"), "2026-11-13"))
        assertEquals("即将开始", planStatusLabel(TrainingPlan(status = "ACTIVE", startDate = "2026-11-14"), "2026-11-13"))
    }

    @Test fun absentValuesDoNotLeakInternalCodesOrInvalidDates() {
        assertFalse(planExpired(null))
        assertEquals("日期待定", shortDate(null))
        assertEquals("11月13日", shortDate("2026-11-13T10:30:00"))
        assertEquals("综合训练", goalLabel("FUTURE_GOAL"))
        assertEquals("稳步进阶", levelLabel("L2"))
        assertEquals("3 组 × 30 秒 · 休息 45 秒", planVolumeText(3, null, 30, 45))
    }
}
