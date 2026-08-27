package io.saha.yoga.vision;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The clip these tests read is synthesized on the spot - solid colour frames
 * from a VideoWriter, no person, nothing committed - because the sample-data
 * policy forbids video fixtures and a colour bar proves the same plumbing.
 */
class VideoFileCaptureTest {
    private static final int WIDTH = 64, HEIGHT = 48, FRAMES = 8;

    @BeforeAll static void loadOpenCv() { nu.pattern.OpenCV.loadLocally(); }

    /** Writes a tiny MJPG clip whose first frame is pure blue, and returns its path. */
    private static Path writeClip(Path directory) {
        var path = directory.resolve("clip.avi");
        var writer = new VideoWriter(path.toString(), VideoWriter.fourcc('M', 'J', 'P', 'G'),
                30, new Size(WIDTH, HEIGHT), true);
        assertTrue(writer.isOpened(), "the built-in MJPG encoder should always be available");
        var frame = new Mat(HEIGHT, WIDTH, org.opencv.core.CvType.CV_8UC3);
        for (int i = 0; i < FRAMES; i++) {
            // blue first, then green: BGR order here, so a swapped channel
            // downstream shows up as the wrong dominant byte
            frame.setTo(i == 0 ? new Scalar(255, 0, 0) : new Scalar(0, 255, 0));
            writer.write(frame);
        }
        frame.release();
        writer.release();
        return path;
    }

    @Test void playsEveryFrameOnceThenSaysTheVideoEnded(@TempDir Path temporary) throws Exception {
        var frames = Collections.synchronizedList(new ArrayList<CameraFrame>());
        var statuses = Collections.synchronizedList(new ArrayList<String>());
        var failures = Collections.synchronizedList(new ArrayList<String>());
        try (var capture = new VideoFileCapture(writeClip(temporary), false, false)) {
            capture.start(frames::add, statuses::add, failures::add);
            // a minute, not an instant: the first OpenCV use in a JVM extracts
            // native libraries, which on a busy machine has taken over ten seconds
            assertTrue(capture.join(60_000), () -> "a finite clip must finish on its own; statuses " + statuses + ", failures " + failures);
        }
        assertEquals(FRAMES, frames.size());
        assertEquals(List.of(), failures);
        var first = frames.get(0);
        assertEquals(WIDTH, first.width());
        assertEquals(HEIGHT, first.height());
        assertEquals(WIDTH * HEIGHT * 4, first.bgraView().length);
        // the first frame was written pure blue; in BGRA that is a high first
        // byte and a low third, and MJPG's loss cannot blur that away
        assertTrue((first.bgraView()[0] & 0xFF) > 128, "blue channel should dominate the first frame");
        assertTrue((first.bgraView()[2] & 0xFF) < 100, "red channel should stay quiet in the first frame");
        assertTrue(statuses.stream().anyMatch(s -> s.contains("has ended")),
                "the end of the clip is a status, not a failure: " + statuses);
    }

    @Test void aMissingFileIsAFailureThatNamesThePath(@TempDir Path temporary) throws Exception {
        var missing = temporary.resolve("nothing-here.mp4");
        var frames = Collections.synchronizedList(new ArrayList<CameraFrame>());
        var failures = Collections.synchronizedList(new ArrayList<String>());
        try (var capture = new VideoFileCapture(missing, false, false)) {
            capture.start(frames::add, s -> { }, failures::add);
            assertTrue(capture.join(60_000));
        }
        assertEquals(List.of(), frames);
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains(missing.toString()),
                "the failure must say which path was tried: " + failures.getFirst());
    }

    @Test void loopingKeepsDeliveringPastTheEndOfTheFile(@TempDir Path temporary) throws Exception {
        var frames = Collections.synchronizedList(new ArrayList<CameraFrame>());
        try (var capture = new VideoFileCapture(writeClip(temporary), true, false)) {
            capture.start(frames::add, s -> { }, s -> fail("looping playback should not fail: " + s));
            long deadline = System.nanoTime() + 60_000_000_000L;
            while (frames.size() < FRAMES * 3 && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(frames.size() >= FRAMES * 3, "the clip should loop rather than stop at " + frames.size());
        }
    }

    @Test void theFactoryChoosesPlaybackOnlyWhenAsked() {
        var previous = System.getProperty("saha.video");
        try {
            System.clearProperty("saha.video");
            assertInstanceOf(OpenCvCameraCapture.class, CameraCapture.forDevice(0));
            System.setProperty("saha.video", "  ");
            assertInstanceOf(OpenCvCameraCapture.class, CameraCapture.forDevice(0));
            System.setProperty("saha.video", "C:\\clips\\anything.mp4");
            assertInstanceOf(VideoFileCapture.class, CameraCapture.forDevice(0));
        } finally {
            if (previous == null) System.clearProperty("saha.video");
            else System.setProperty("saha.video", previous);
        }
    }
}
