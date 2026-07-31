package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkFrame;

public interface LandmarkSource extends AutoCloseable {
    LandmarkFrame nextFrame();
    String description();
    @Override default void close() {}
}

