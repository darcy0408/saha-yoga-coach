package io.saha.yoga.domain;

public record AlignmentRule(
        String id, LandmarkName first, LandmarkName vertex, LandmarkName third,
        BilateralStrategy bilateralStrategy,
        double minimumDegrees, double maximumDegrees, String suggestion, int priority) {
    public AlignmentRule {
        if (minimumDegrees > maximumDegrees) throw new IllegalArgumentException("Invalid angle range");
        if (bilateralStrategy == null) throw new IllegalArgumentException("bilateralStrategy is required");
    }
}
