package io.saha.yoga.analysis;

import io.saha.yoga.domain.Landmark;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    private Landmark p(double x, double y) { return new Landmark(x, y, 1); }
    @Test void calculatesRightAngle() { assertEquals(90, Geometry.angleDegrees(p(1,0), p(0,0), p(0,1)), 0.001); }
    @Test void calculatesStraightAngle() { assertEquals(180, Geometry.angleDegrees(p(-1,0), p(0,0), p(1,0)), 0.001); }
    @Test void rejectsCoincidentPoints() { assertThrows(IllegalArgumentException.class, () -> Geometry.angleDegrees(p(0,0), p(0,0), p(1,0))); }
}

