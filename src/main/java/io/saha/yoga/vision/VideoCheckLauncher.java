package io.saha.yoga.vision;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runs a recorded clip through the production vision pipeline and reports
 * what the model saw, frame by frame.
 *
 * <p>This answers the live-session questions - do overhead wrists survive,
 * do seated legs clear the reliability gate, does the crop hold or hunt -
 * from a recording instead of a person, which makes the answer repeatable.
 * The stack is the production one: the same estimator, the same person crop
 * inside it, the same smoother running alongside (its output is exercised but
 * not reported, because raw scores are what judge the model). Only the pacing
 * differs: frames are processed as fast as inference allows, so the rate
 * printed here measures this machine's pipeline, not the clip's clock.
 *
 * <p>The summary prints one line per second of video, which is coarse enough
 * to read as a story - "wrists held until the twelfth second, then fell" -
 * and the full per-frame trace goes to a CSV beside the build for anything
 * finer. 0.35 is {@code PoseAnalyzer.RELIABILITY_THRESHOLD}, the gate a joint
 * must clear before it can earn a correction or a chime.
 */
public final class VideoCheckLauncher {
    private VideoCheckLauncher() { }

    private static final double RELIABILITY_GATE = 0.35;
    /** Joints the live questions are about, in the order the report prints them. */
    private static final List<LandmarkName> REPORTED = List.of(
            LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER,
            LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP,
            LandmarkName.LEFT_WRIST, LandmarkName.RIGHT_WRIST,
            LandmarkName.LEFT_KNEE, LandmarkName.RIGHT_KNEE,
            LandmarkName.LEFT_ANKLE, LandmarkName.RIGHT_ANKLE);

    private record FrameRow(int index, double[] scores, double wristHighestY, double noseY,
                            double regionX, double regionY, double regionSize, double estimateMillis) { }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            System.out.println("Usage: gradlew videoCheck \"-Pvideo=C:\\path\\to\\clip.mp4\"");
            return;
        }
        var video = Path.of(args[0]);
        var model = PoseModelLocator.locate();
        if (model.isEmpty()) {
            System.out.println("No verified model on disk; run scripts/fetch-model.ps1 first.");
            return;
        }
        var csv = args.length > 1 ? Path.of(args[1]) : Path.of("build", "review", "video-check.csv");
        Files.createDirectories(csv.toAbsolutePath().getParent());
        System.out.println("Model: " + model.get());

        var rows = new ArrayList<FrameRow>();
        var smoother = new LandmarkSmoother();
        try (var estimator = new PoseEstimator(model.get());
             var capture = new VideoFileCapture(video, false, false)) {
            long startedAt = System.nanoTime();
            capture.start(frame -> {
                try {
                    long before = System.nanoTime();
                    var raw = estimator.estimate(frame);
                    double millis = (System.nanoTime() - before) / 1_000_000.0;
                    smoother.smooth(raw);
                    rows.add(row(rows.size(), raw, estimator.lastShownRegion(), millis));
                } catch (Exception e) {
                    // mirrors the live source: one bad frame is a gap, not the end
                    System.out.println("Frame " + rows.size() + " could not be estimated: " + e.getMessage());
                }
            }, message -> System.out.println(message), message -> System.out.println(message));
            // a clip is finite; an hour means something is wedged, not slow
            if (!capture.join(60 * 60 * 1000L)) {
                System.out.println("Playback did not finish; results below cover what was processed.");
            }
            if (rows.isEmpty()) {
                System.out.println("No frames were estimated, so there is nothing to report.");
                return;
            }
            double wallSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
            writeCsv(csv, rows);
            report(rows, capture.declaredFramesPerSecond(), wallSeconds, csv);
        }
    }

    private static FrameRow row(int index, LandmarkFrame raw, PoseEstimator.ShownRegion region, double millis) {
        var marks = raw.landmarks();
        var scores = new double[REPORTED.size()];
        for (int i = 0; i < scores.length; i++) scores[i] = confidence(marks.get(REPORTED.get(i)));
        var leftWrist = marks.get(LandmarkName.LEFT_WRIST);
        var rightWrist = marks.get(LandmarkName.RIGHT_WRIST);
        // the higher wrist on screen is the smaller y; missing joints sink so
        // they can never register as raised
        double wristHighestY = Math.min(leftWrist == null ? Double.MAX_VALUE : leftWrist.y(),
                rightWrist == null ? Double.MAX_VALUE : rightWrist.y());
        var nose = marks.get(LandmarkName.NOSE);
        return new FrameRow(index, scores, wristHighestY, nose == null ? -Double.MAX_VALUE : nose.y(),
                region.x(), region.y(), region.size(), millis);
    }

    private static double confidence(Landmark mark) { return mark == null ? 0 : mark.confidence(); }

    private static void writeCsv(Path csv, List<FrameRow> rows) throws IOException {
        try (var out = new PrintWriter(Files.newBufferedWriter(csv))) {
            var header = new StringBuilder("frame");
            for (var name : REPORTED) header.append(',').append(name.name().toLowerCase(Locale.ROOT));
            header.append(",wrist_highest_y,nose_y,region_x,region_y,region_size,estimate_ms");
            out.println(header);
            for (var row : rows) {
                var line = new StringBuilder().append(row.index());
                for (var score : row.scores()) line.append(',').append(format(score));
                line.append(',').append(format(row.wristHighestY() == Double.MAX_VALUE ? -1 : row.wristHighestY()))
                        .append(',').append(format(row.noseY() == -Double.MAX_VALUE ? -1 : row.noseY()))
                        .append(',').append(format(row.regionX()))
                        .append(',').append(format(row.regionY()))
                        .append(',').append(format(row.regionSize()))
                        .append(',').append(format(row.estimateMillis()));
                out.println(line);
            }
        }
    }

    private static String format(double value) { return String.format(Locale.ROOT, "%.3f", value); }

    private static void report(List<FrameRow> rows, double fps, double wallSeconds, Path csv) {
        System.out.printf(Locale.ROOT, "%n%d frames at %.0f fps declared · processed in %.1f s (%.0f frames/s on this machine)%n",
                rows.size(), fps, wallSeconds, rows.size() / wallSeconds);
        System.out.println("Raw scores are the model's own, before smoothing. Gate: " + RELIABILITY_GATE + ".\n");
        System.out.println("sec | wrist L|R  | knee L|R   | ankle L|R  | overhead | crop size | whole-frame");
        int perSecond = (int) Math.max(1, Math.round(fps));
        for (int start = 0; start < rows.size(); start += perSecond) {
            var second = rows.subList(start, Math.min(start + perSecond, rows.size()));
            System.out.printf(Locale.ROOT, "%3d | %.2f %.2f  | %.2f %.2f  | %.2f %.2f  |   %3.0f%%   |   %.2f    | %3.0f%%%n",
                    start / perSecond,
                    median(second, r -> r.scores()[4]), median(second, r -> r.scores()[5]),
                    median(second, r -> r.scores()[6]), median(second, r -> r.scores()[7]),
                    median(second, r -> r.scores()[8]), median(second, r -> r.scores()[9]),
                    100.0 * second.stream().filter(r -> r.wristHighestY() < r.noseY()).count() / second.size(),
                    median(second, FrameRow::regionSize),
                    100.0 * second.stream().filter(r -> r.regionSize() >= .999).count() / second.size());
        }
        System.out.println();
        for (int i = 0; i < REPORTED.size(); i++) {
            final int joint = i;
            System.out.printf(Locale.ROOT, "%-15s median %.2f · above gate in %3.0f%% of frames%n",
                    REPORTED.get(i).name().toLowerCase(Locale.ROOT),
                    median(rows, r -> r.scores()[joint]),
                    100.0 * rows.stream().filter(r -> r.scores()[joint] >= RELIABILITY_GATE).count() / rows.size());
        }
        long resets = 0;
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).regionSize() >= .999 && rows.get(i - 1).regionSize() < .999) resets++;
        }
        System.out.printf(Locale.ROOT, "%nCrop: whole-frame in %.0f%% of frames · %d resets back to whole frame · estimate median %.1f ms%n",
                100.0 * rows.stream().filter(r -> r.regionSize() >= .999).count() / rows.size(), resets,
                median(rows, FrameRow::estimateMillis));
        System.out.println("Overhead means the higher wrist sits above the nose in that frame.");
        System.out.println("Per-frame trace: " + csv.toAbsolutePath());
    }

    private static double median(List<FrameRow> rows, java.util.function.ToDoubleFunction<FrameRow> value) {
        var sorted = rows.stream().mapToDouble(value).sorted().toArray();
        return sorted.length == 0 ? 0 : sorted[sorted.length / 2];
    }
}
