package io.saha.yoga.analysis;

import io.saha.yoga.domain.Landmark;

public final class Geometry {
    private Geometry() {}
    public static double angleDegrees(Landmark first, Landmark vertex, Landmark third) {
        double ax = first.x() - vertex.x(), ay = first.y() - vertex.y();
        double bx = third.x() - vertex.x(), by = third.y() - vertex.y();
        double denominator = Math.hypot(ax, ay) * Math.hypot(bx, by);
        if (denominator < 1e-9) throw new IllegalArgumentException("Angle points must be distinct");
        double cosine = Math.max(-1, Math.min(1, (ax * bx + ay * by) / denominator));
        return Math.toDegrees(Math.acos(cosine));
    }
}

