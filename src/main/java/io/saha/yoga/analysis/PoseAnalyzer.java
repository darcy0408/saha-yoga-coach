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
            var angle = Geometry.angleDegrees(points.get(rule.first()), points.get(rule.vertex()), points.get(rule.third()));
            return new Evaluation(rule, angle);
        }).filter(e -> e.angle < e.rule.minimumDegrees() || e.angle > e.rule.maximumDegrees())
                .sorted(Comparator.comparingInt(e -> e.rule.priority())).limit(2).map(e -> e.rule.suggestion()).toList();
        String status = misses.isEmpty() ? "Steady — keep breathing" : "Almost aligned";
        return new AnalysisResult.Reliable(status, misses, confidence);
    }
    private record Evaluation(AlignmentRule rule, double angle) {}
}
