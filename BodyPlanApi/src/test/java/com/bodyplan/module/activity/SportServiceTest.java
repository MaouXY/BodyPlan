package com.bodyplan.module.activity;

import com.bodyplan.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.bodyplan.module.activity.SportModels.*;
import static org.junit.jupiter.api.Assertions.*;

class SportServiceTest {
    private Point point(double lat, double lon, long time, int segment) { return new Point(lat, lon, time, segment, 5); }
    @Test void calculatesWgs84Distance() {
        assertEquals(111.32, SportService.distance(List.of(point(0,0,1000,0),point(0,0.001,11000,0)), "RUN"), 0.2);
    }
    @Test void excludesPauseGap() {
        assertEquals(0, SportService.distance(List.of(point(0,0,1000,0),point(0,0.001,11000,1)), "RUN"));
    }
    @Test void excludesGpsJumpAndLongSignalLoss() {
        assertEquals(0, SportService.distance(List.of(point(0,0,1000,0),point(0,1,3000,0)), "RUN"));
        assertEquals(0, SportService.distance(List.of(point(0,0,1000,0),point(0,0.001,60000,0)), "RUN"));
    }
    @Test void cyclingAllowsHigherSpeedThanWalking() {
        var points = List.of(point(0,0,1000,0),point(0,0.001,11000,0));
        assertTrue(SportService.distance(points, "RIDE") > 100);
        assertEquals(0, SportService.distance(points, "WALK"));
    }
    @Test void rejectsInconsistentTime() {
        assertThrows(BusinessException.class, () -> SportService.validate(
                new Save("abcdefghijklmnop", "RUN", "AEROBIC", null, null, 1000, 2000, 60, List.of(), null), 1));
    }
    @Test void rejectsOutOfOrderOrInvalidCoordinates() {
        assertThrows(BusinessException.class, () -> SportService.validate(new Save("abcdefghijklmnop", "RUN", "AEROBIC",
                null,null,1000,20000,10,List.of(point(0,0,12000,0),point(0,0,11000,0)),null),1));
        assertThrows(BusinessException.class, () -> SportService.validate(new Save("abcdefghijklmnop", "RUN", "AEROBIC",
                null,null,1000,20000,10,List.of(point(Double.NaN,0,12000,0)),null),1));
    }
    @Test void indoorSessionsHaveNoFakeRoute() {
        SportService.validate(new Save("abcdefghijklmnop", "STRENGTH", "STRENGTH", null,null,1000,20000,10,List.of(),null),1);
        assertThrows(BusinessException.class, () -> SportService.validate(new Save("abcdefghijklmnop", "STRENGTH", "STRENGTH",
                null,null,1000,20000,10,List.of(point(0,0,12000,0)),null),1));
    }
}
