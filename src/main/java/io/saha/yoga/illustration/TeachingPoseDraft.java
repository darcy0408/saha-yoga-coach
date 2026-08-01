package io.saha.yoga.illustration;

import java.util.Map;

public record TeachingPoseDraft(
        String poseId,
        String displayName,
        String view,
        String gaze,
        double floorY,
        Map<BodyAnchor, IllustrationPoint> anchors
) {
    public TeachingPoseDraft {
        anchors = Map.copyOf(anchors);
        for (var required : BodyAnchor.values()) {
            if (!anchors.containsKey(required)) throw new IllegalArgumentException("Missing anchor: " + required);
        }
    }

    public IllustrationPoint point(BodyAnchor anchor) { return anchors.get(anchor); }
}
