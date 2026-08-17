package io.saha.yoga.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.concurrent.TimeUnit;

/**
 * Reports which camera devices and backends this machine can actually open.
 *
 * Practice can only say "the camera did not open", which is true and useless:
 * a device busy in another application, a Windows privacy setting, an index
 * that is a virtual or infrared camera rather than the webcam, and a backend
 * that hangs rather than failing all look identical from there.
 *
 * Each attempt runs on its own daemon thread and is given a deadline, because
 * the failure being diagnosed is a native call that never returns - waiting on
 * it here would reproduce the hang rather than report it.
 */
public final class CameraCheckLauncher {
    private CameraCheckLauncher() { }

    private static final int LAST_INDEX = 3;
    private static final long OPEN_DEADLINE_SECONDS = 6;

    public static void main(String[] args) {
        // The real capture class runs first, and deliberately before OpenCV is
        // loaded here, so it loads the library itself exactly as practice does.
        // Pre-loading hid a capture loop that built a Mat before its native
        // library existed: the check passed while the application died on
        // UnsatisfiedLinkError at the first frame.
        //
        // Running it first also protects the answer that matters. A backend
        // that hangs keeps holding the device, so probing backends beforehand
        // would sabotage the one question worth asking - can practice get video.
        practiceCanGetVideo();
        System.out.println("\nLoading the local OpenCV camera library...");
        OpenCV.loadLocally();
        // Media Foundation is probed only when asked for. Its open() never
        // returns on some hardware, and a hung native call left behind by this
        // process can leave the device unusable for the next launch - which is
        // exactly the failure this tool exists to diagnose, so doing it by
        // default would mean the check causing the fault it then reports.
        boolean everyBackend = args.length > 0 && args[0].equals("--all");
        var backends = everyBackend
                ? new Backend[]{new Backend("DirectShow", Videoio.CAP_DSHOW),
                        new Backend("Media Foundation", Videoio.CAP_MSMF),
                        new Backend("automatic", Videoio.CAP_ANY)}
                : new Backend[]{new Backend("DirectShow", Videoio.CAP_DSHOW)};
        System.out.println("\nDevice by device, for detail. Camera indices 0 to " + LAST_INDEX + ", each backend given "
                + OPEN_DEADLINE_SECONDS + " seconds before it is called hung.");
        System.out.println(everyBackend
                ? "Probing every backend. If a camera stops working afterwards, that is this flag: reboot or unplug it.\n"
                : "DirectShow only, which is the backend practice uses. Pass --all to probe the others too.\n");
        boolean anyOpened = false;
        for (int index = 0; index <= LAST_INDEX; index++) {
            for (var backend : backends) {
                anyOpened |= report(index, backend);
            }
        }
        System.out.println();
        System.out.println(anyOpened
                ? "At least one device opened. Practice should use the first index and backend reported as OPENED."
                : "No device opened on any backend. The usual causes, in order: another application is holding "
                + "the camera (close Teams, Zoom, or a browser tab), or Windows is blocking it - check Settings, "
                + "Privacy & security, Camera, and make sure 'Let desktop apps access your camera' is on.");
        // a hung native open leaves its thread stuck, so leaving normally would hang too
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }

    /** Exercises the class practice actually uses, rather than something that resembles it. */
    private static void practiceCanGetVideo() {
        System.out.println("Starting the camera the way practice does, on camera 0:");
        var capture = new OpenCvCameraCapture(0);
        var frames = new java.util.concurrent.atomic.AtomicInteger();
        var first = new java.util.concurrent.atomic.AtomicReference<String>();
        try {
            capture.start(
                    frame -> { if (frames.incrementAndGet() == 1) first.set(frame.width() + "x" + frame.height()); },
                    line -> System.out.println("    " + line),
                    failure -> System.out.println("    FAILED: " + failure));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
            while (System.nanoTime() < deadline && frames.get() < 30) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            capture.close();
        }
        int count = frames.get();
        System.out.println(count > 0
                ? "  VIDEO IS REACHING PRACTICE: " + count + " frames, first one " + first.get()
                : "  NO VIDEO REACHED PRACTICE. The reason should be in the lines above.");
    }

    private record Backend(String name, int id) { }

    private static boolean report(int index, Backend backend) {
        var label = "camera " + index + " · " + backend.name();
        var result = new Result();
        var attempt = new Thread(() -> {
            VideoCapture camera = null;
            try {
                camera = new VideoCapture();
                if (!camera.open(index, backend.id()) || !camera.isOpened()) { result.detail = "did not open"; return; }
                var frame = new Mat();
                try {
                    result.opened = camera.read(frame) && !frame.empty();
                    result.detail = result.opened ? frame.cols() + "x" + frame.rows() + " frame read" : "opened but read no frame";
                } finally {
                    frame.release();
                }
            } catch (Throwable error) {
                result.detail = error.getClass().getSimpleName() + ": " + error.getMessage();
            } finally {
                if (camera != null) camera.release();
            }
        }, "saha-camera-check-" + index + "-" + backend.id());
        attempt.setDaemon(true);
        long started = System.nanoTime();
        attempt.start();
        try {
            attempt.join(TimeUnit.SECONDS.toMillis(OPEN_DEADLINE_SECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        if (attempt.isAlive()) {
            System.out.printf("  %-30s HUNG      (still inside open after %d ms)%n", label, millis);
            return false;
        }
        System.out.printf("  %-30s %-9s (%d ms) %s%n", label, result.opened ? "OPENED" : "failed", millis, result.detail);
        return result.opened;
    }

    private static final class Result {
        volatile boolean opened;
        volatile String detail = "";
    }
}
