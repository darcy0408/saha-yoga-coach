package io.saha.yoga.illustration;

public record IllustrationPoint(double x, double y) {
    public IllustrationPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("coordinates must be finite");
        if (x < 0 || x > 1 || y < 0 || y > 1) throw new IllegalArgumentException("coordinates must be normalized");
    }
}
