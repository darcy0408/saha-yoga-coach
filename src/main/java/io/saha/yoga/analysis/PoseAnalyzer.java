package io.saha.yoga.analysis;

import io.saha.yoga.domain.*;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public final class PoseAnalyzer {
    public static final double RELIABILITY_THRESHOLD = 0.70;
    /** Enough to know a person is present and upright. */
    private static final List<LandmarkName> CORE = List.of(LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER,
            LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP);

    public AnalysisResult analyze(Pose pose, LandmarkFrame frame) {
        double confidence = visibility(pose, frame);
        if (confidence < RELIABILITY_THRESHOLD) {
            return new AnalysisResult.Unreliable(framingHint(pose, frame), confidence);
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
    /**
     * How well the camera can see what this pose actually needs.
     *
     * A pose is not blocked by a joint it never measures: a seated pose with no
     * alignment rules only needs to see a torso, so feet outside the frame stop
     * mattering. Where a rule does apply, either side satisfies it, because the
     * rules already evaluate both sides and choose one — demanding both would
     * refuse to coach anyone standing side-on to the camera, which is exactly
     * how most of these poses are best viewed.
     */
    private double visibility(Pose pose, LandmarkFrame frame) {
        double core = frame.minimumConfidence(CORE);
        if (pose.alignmentRules().isEmpty()) return core;
        double measurable = 1;
        for (var rule : pose.alignmentRules()) {
            double left = frame.minimumConfidence(List.of(rule.first(), rule.vertex(), rule.third()));
            double right = frame.minimumConfidence(List.of(mirror(rule.first()), mirror(rule.vertex()), mirror(rule.third())));
            measurable = Math.min(measurable, Math.max(left, right));
        }
        return Math.min(core, measurable);
    }

    /** Names the body parts the camera cannot see, rather than guessing at the cause. */
    private String framingHint(Pose pose, LandmarkFrame frame) {
        var needed = new LinkedHashSet<>(CORE);
        for (var rule : pose.alignmentRules()) {
            needed.add(rule.first()); needed.add(rule.vertex()); needed.add(rule.third());
            needed.add(mirror(rule.first())); needed.add(mirror(rule.vertex())); needed.add(mirror(rule.third()));
        }
        var weak = needed.stream()
                .filter(name -> {
                    var point = frame.landmarks().get(name);
                    return point == null || point.confidence() < RELIABILITY_THRESHOLD;
                })
                .map(PoseAnalyzer::readable)
                .distinct()
                .toList();
        if (weak.isEmpty()) return "Hold still for a moment so the view can settle.";
        String parts = weak.size() == 1 ? weak.getFirst()
                : String.join(", ", weak.subList(0, weak.size() - 1)) + " and " + weak.getLast();
        return "Your " + parts + " are out of view. Step back or tilt the camera until your whole body fits, then this resumes on its own.";
    }

    private static String readable(LandmarkName name) {
        return switch (name) {
            case LEFT_SHOULDER, RIGHT_SHOULDER -> "shoulders";
            case LEFT_ELBOW, RIGHT_ELBOW -> "elbows";
            case LEFT_WRIST, RIGHT_WRIST, LEFT_HAND, RIGHT_HAND -> "hands";
            case LEFT_HIP, RIGHT_HIP -> "hips";
            case LEFT_KNEE, RIGHT_KNEE -> "knees";
            case LEFT_ANKLE, RIGHT_ANKLE, LEFT_TOE, RIGHT_TOE -> "feet";
            case NOSE -> "head";
        };
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
