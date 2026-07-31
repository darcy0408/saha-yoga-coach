package io.saha.yoga.domain;

import java.util.List;

public record Pose(
        String id, String displayName, Difficulty difficulty, int defaultDurationSeconds,
        List<String> instructions, List<String> modifications,
        List<LandmarkName> requiredLandmarks, List<AlignmentRule> alignmentRules,
        String contraindicationNotice) {
    public Pose {
        instructions = List.copyOf(instructions);
        modifications = List.copyOf(modifications);
        requiredLandmarks = List.copyOf(requiredLandmarks);
        alignmentRules = List.copyOf(alignmentRules);
    }
}

