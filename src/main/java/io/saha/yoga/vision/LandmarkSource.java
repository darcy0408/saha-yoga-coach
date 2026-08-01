package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkFrame;

public interface LandmarkSource extends AutoCloseable {
    /**
     * The floor plane, in the same normalised 0..1 coordinates as landmark y.
     * The view draws its "floor reference" line here, so a source that
     * SYNTHESISES poses must rest their contact points on it or the body
     * appears to float. A source reading a real camera cannot know where the
     * floor is and is not expected to honour this.
     */
    double FLOOR_Y = .96;

    LandmarkFrame nextFrame();
    default void selectPose(String poseId) {}
    default boolean isTransitioning() { return false; }
    default FaceDirection faceDirection() { return FaceDirection.FRONT; }
    default String transitionGuidance() { return "Move slowly into the next position."; }
    default double spineBend() { return 0; }
    String description();
    @Override default void close() {}
}
