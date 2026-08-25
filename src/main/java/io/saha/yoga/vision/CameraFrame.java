package io.saha.yoga.vision;

import java.util.Objects;

/** One transient BGRA camera frame. It is never persisted by Saha. */
public record CameraFrame(int width, int height, byte[] bgra) {
    public CameraFrame {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Frame dimensions must be positive");
        Objects.requireNonNull(bgra, "bgra");
        if (bgra.length != Math.multiplyExact(Math.multiplyExact(width, height), 4)) {
            throw new IllegalArgumentException("BGRA payload does not match frame dimensions");
        }
        bgra = bgra.clone();
    }

    @Override public byte[] bgra() { return bgra.clone(); }

    /**
     * The frame's own pixels, for reading only. Never write into this array.
     *
     * <p>{@link #bgra()} hands out a copy so that a caller holding a frame
     * cannot have it changed underneath them, and that is the right default.
     * But every frame is already copied once out of OpenCV and once more by the
     * constructor, and the two consumers on the live path - the estimator and
     * the preview - only ever read. Copying for them as well put four copies of
     * every frame through the collector, which at a useful capture size is
     * hundreds of megabytes a second and shows up as stutter in a coach that
     * has to keep time.
     */
    public byte[] bgraView() { return bgra; }
}
