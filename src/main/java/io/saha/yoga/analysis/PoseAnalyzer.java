package io.saha.yoga.analysis;

import io.saha.yoga.domain.*;
import java.util.Comparator;

public final class PoseAnalyzer {
    public static final double RELIABILITY_THRESHOLD = 0.70;
    public AnalysisResult analyze(Pose pose, LandmarkFrame frame) {
        double confidence = frame.minimumConfidence(pose.requiredLandmarks());
        if (confidence < RELIABILITY_THRESHOLD) {
            return new AnalysisResult.Unreliable("Step back so your full body is visible, and check the lighting.", confidence);
        }
        if (pose.alignmentRules().isEmpty()) {
            return new AnalysisResult.InstructionOnly(
                    "Follow the written setup. Camera alignment checks are not available for this pose yet.", confidence);
        }
        var misses = pose.alignmentRules().stream().map(rule -> {
            var points = frame.landmarks();
            var primary = Geometry.angleDegrees(points.get(rule.first()), points.get(rule.vertex()), points.get(rule.third()));
            var mirrored = Geometry.angleDegrees(points.get(mirror(rule.first())), points.get(mirror(rule.vertex())), points.get(mirror(rule.third())));
            var angle = select(rule, primary, mirrored);
            return new Evaluation(rule, angle);
        }).filter(e -> e.angle < e.rule.minimumDegrees() || e.angle > e.rule.maximumDegrees())
                .sorted(Comparator.comparingInt(e -> e.rule.priority())).limit(2).map(e -> e.rule.suggestion()).toList();
        String status = misses.isEmpty() ? "Steady — keep breathing" : "Almost aligned";
        return new AnalysisResult.Reliable(status, misses, confidence);
    }
    private double select(AlignmentRule rule, double primary, double mirrored) {
        return switch (rule.bilateralStrategy()) {
            case FIXED_SIDE -> primary;
            case MOST_BENT -> Math.min(primary, mirrored);
            case STRAIGHTEST -> Math.max(primary, mirrored);
            case WORST_MATCH -> distanceFromRange(rule, primary) >= distanceFromRange(rule, mirrored) ? primary : mirrored;
        };
    }
    private double distanceFromRange(AlignmentRule rule, double angle) {
        if (angle < rule.minimumDegrees()) return rule.minimumDegrees() - angle;
        if (angle > rule.maximumDegrees()) return angle - rule.maximumDegrees();
        return 0;
    }
    private LandmarkName mirror(LandmarkName name) {
        return switch (name) {
            case LEFT_SHOULDER -> LandmarkName.RIGHT_SHOULDER;
            case RIGHT_SHOULDER -> LandmarkName.LEFT_SHOULDER;
            case LEFT_ELBOW -> LandmarkName.RIGHT_ELBOW;
            case RIGHT_ELBOW -> LandmarkName.LEFT_ELBOW;
            case LEFT_WRIST -> LandmarkName.RIGHT_WRIST;
            case RIGHT_WRIST -> LandmarkName.LEFT_WRIST;
            case LEFT_HAND -> LandmarkName.RIGHT_HAND;
            case RIGHT_HAND -> LandmarkName.LEFT_HAND;
            case LEFT_HIP -> LandmarkName.RIGHT_HIP;
            case RIGHT_HIP -> LandmarkName.LEFT_HIP;
            case LEFT_KNEE -> LandmarkName.RIGHT_KNEE;
            case RIGHT_KNEE -> LandmarkName.LEFT_KNEE;
            case LEFT_ANKLE -> LandmarkName.RIGHT_ANKLE;
            case RIGHT_ANKLE -> LandmarkName.LEFT_ANKLE;
            case LEFT_TOE -> LandmarkName.RIGHT_TOE;
            case RIGHT_TOE -> LandmarkName.LEFT_TOE;
            case NOSE -> LandmarkName.NOSE;
        };
    }
    private record Evaluation(AlignmentRule rule, double angle) {}
}
