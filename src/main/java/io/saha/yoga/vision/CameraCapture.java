package io.saha.yoga.vision;

import java.util.function.Consumer;

public interface CameraCapture extends AutoCloseable {
    void start(Consumer<CameraFrame> frames, Consumer<String> status, Consumer<String> failures);
    boolean isOpen();
    @Override void close();
}
