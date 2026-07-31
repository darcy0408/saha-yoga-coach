package io.saha.yoga.domain;

public record AlignmentRule(
        String id, LandmarkName first, LandmarkName vertex, LandmarkName third,
        double minimumDegrees, double maximumDegrees, String suggestion, int priority) {
    public AlignmentRule {
        if (minimumDegrees > maximumDegrees) throw new IllegalArgumentException("Invalid angle range");
    }
}

