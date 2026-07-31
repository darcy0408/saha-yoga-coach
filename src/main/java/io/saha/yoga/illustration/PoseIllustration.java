package io.saha.yoga.illustration;

import java.util.List;

public record PoseIllustration(
        String poseId,
        String requiredView,
        String setupSummary,
        List<String> referenceUrls,
        ReviewState reviewState,
        Grounding grounding
) {
    public PoseIllustration {
        if (poseId == null || poseId.isBlank()) throw new IllegalArgumentException("poseId is required");
        if (requiredView == null || requiredView.isBlank()) throw new IllegalArgumentException("requiredView is required");
        if (setupSummary == null || setupSummary.isBlank()) throw new IllegalArgumentException("setupSummary is required");
        referenceUrls = List.copyOf(referenceUrls);
        if (grounding == null) throw new IllegalArgumentException("grounding is required");
    }
}
