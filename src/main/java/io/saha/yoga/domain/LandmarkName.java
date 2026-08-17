package io.saha.yoga.domain;

public enum LandmarkName {
    NOSE, LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST, LEFT_HAND, RIGHT_HAND, LEFT_HIP, RIGHT_HIP, LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE, LEFT_TOE, RIGHT_TOE;

    /**
     * The same landmark on the other side of the body, for mirroring a pose.
     *
     * Derived from the name rather than listed, so a landmark added later
     * cannot be quietly left out of the mirror and turn a body inside out.
     */
    public LandmarkName mirrored() {
        var name = name();
        if (name.startsWith("LEFT_")) return valueOf("RIGHT_" + name.substring("LEFT_".length()));
        if (name.startsWith("RIGHT_")) return valueOf("LEFT_" + name.substring("RIGHT_".length()));
        return this;
    }
}
