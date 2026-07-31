package io.saha.yoga.domain;

public record Landmark(double x, double y, double confidence) {
    public Landmark {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Coordinates must be finite");
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("Confidence must be in [0,1]");
    }
}

