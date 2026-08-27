package io.saha.yoga.vision;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface CameraCapture extends AutoCloseable {
    void start(Consumer<CameraFrame> frames, Consumer<String> status, Consumer<String> failures);
    boolean isOpen();
    @Override void close();

    /**
     * The capture the application should use for {@code deviceIndex}.
     *
     * <p>Normally the device itself. With the {@code saha.video} system
     * property set to a file path, a looping playback of that file instead:
     * every camera path in the application - the calibration preview, the
     * practice, the diagnostics view - then runs on recorded frames, which is
     * how the pipeline is exercised when no one can stand in front of the
     * camera. Nothing downstream can tell the difference, deliberately.
     */
    static CameraCapture forDevice(int deviceIndex) {
        var video = System.getProperty("saha.video", "").strip();
        return video.isEmpty()
                ? new OpenCvCameraCapture(deviceIndex)
                : new VideoFileCapture(Path.of(video), true, true);
    }
}
