package com.bodyplan.module.nutrition;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static com.bodyplan.module.nutrition.NutritionModels.*;
import static org.junit.jupiter.api.Assertions.*;

class NutritionServiceTest {
    @Test void scalesPortionAndPreservesUnknown() {
        assertEquals(new BigDecimal("174.00"), NutritionService.amount(new BigDecimal("116"), new BigDecimal("1.5")));
        assertNull(NutritionService.amount(null, new BigDecimal("1.5")));
    }
    @Test void unknownFiberIsNotReportedAsComplete() {
        var e = new Entry("1",LocalDate.now(),"LUNCH","2","食物",new BigDecimal("100"),
                new BigDecimal("100"),new BigDecimal("10"),BigDecimal.ONE,BigDecimal.TEN,null,"测试");
        var totals = NutritionService.totals(List.of(e));
        assertFalse(totals.fiberComplete());
        assertEquals(new BigDecimal("100"), totals.kcal());
    }
    @Test void emptyDayHasZeroTotals() {
        assertEquals(BigDecimal.ZERO, NutritionService.totals(List.of()).kcal());
        assertTrue(NutritionService.totals(List.of()).fiberComplete());
    }
}
