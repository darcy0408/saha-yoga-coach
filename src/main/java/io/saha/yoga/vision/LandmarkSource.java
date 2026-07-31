package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkFrame;

public interface LandmarkSource extends AutoCloseable {
    LandmarkFrame nextFrame();
    default void selectPose(String poseId) {}
    default boolean isTransitioning() { return false; }
    default FaceDirection faceDirection() { return FaceDirection.FRONT; }
    default String transitionGuidance() { return "Move slowly into the next position."; }
    String description();
    @Override default void close() {}
}
