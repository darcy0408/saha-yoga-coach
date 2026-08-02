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
}
