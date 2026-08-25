package io.saha.yoga.vision;

import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Runs MoveNet SinglePose Lightning on a camera frame and returns the body
 * landmarks the coaching pipeline already speaks.
 *
 * <p>Everything happens on this machine. No frame is written to disk and
 * nothing leaves the process, which for a camera pointed at someone's living
 * room is the whole point.
 *
 * <p>The model sees a square patch cropped to the person rather than the whole
 * frame; {@link PersonCrop} chooses it from the previous frame's answer, which
 * makes this stateful. One estimator therefore follows one body through one
 * practice, and is not thread-safe: {@link OrtSession#run} and the crop are
 * both driven from a single inference thread.
 */
public final class PoseEstimator implements AutoCloseable {
    /** MoveNet emits COCO keypoints in this order; only the ones Saha uses are named. */
    private static final Map<Integer, LandmarkName> KEYPOINTS = Map.ofEntries(
            Map.entry(0, LandmarkName.NOSE),
            Map.entry(5, LandmarkName.LEFT_SHOULDER), Map.entry(6, LandmarkName.RIGHT_SHOULDER),
            Map.entry(7, LandmarkName.LEFT_ELBOW), Map.entry(8, LandmarkName.RIGHT_ELBOW),
            Map.entry(9, LandmarkName.LEFT_WRIST), Map.entry(10, LandmarkName.RIGHT_WRIST),
            Map.entry(11, LandmarkName.LEFT_HIP), Map.entry(12, LandmarkName.RIGHT_HIP),
            Map.entry(13, LandmarkName.LEFT_KNEE), Map.entry(14, LandmarkName.RIGHT_KNEE),
            Map.entry(15, LandmarkName.LEFT_ANKLE), Map.entry(16, LandmarkName.RIGHT_ANKLE));

    /** Letterbox padding colour; grey is neutral for a model trained on natural images. */
    private static final Scalar PAD = new Scalar(114, 114, 114);
    /** How far past the wrist and ankle to place the hand and toe the model does not emit. */
    private static final double EXTENSION = .055;

    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final PersonCrop crop = new PersonCrop();
    private final OrtSession session;
    private final String inputName;
    private final OnnxJavaType inputType;
    private final int side;

    public PoseEstimator(Path modelPath) throws Exception {
        byte[] model = readModel(modelPath);
        try (var options = new OrtSession.SessionOptions()) {
            // One inference thread: capture, inference and the interface already
            // run concurrently, and letting the runtime spawn its own pool
            // underneath just makes them fight for cores on a laptop.
            options.setIntraOpNumThreads(1);
            this.session = environment.createSession(model, options);
        }
        this.inputName = session.getInputNames().iterator().next();
        var info = (TensorInfo) session.getInputInfo().get(inputName).getInfo();
        this.inputType = info.type;
        long[] shape = info.getShape();
        // Square input; fall back to Lightning's 192 if the axis is dynamic.
        this.side = (int) (shape.length >= 3 && shape[1] > 0 ? shape[1] : 192);
    }

    /**
     * Reads the model through a memory-mapped segment in a confined arena, so
     * the operating system can page it in lazily and release it deterministically
     * instead of leaving a nine-megabyte heap copy for the collector.
     */
    private static byte[] readModel(Path modelPath) throws IOException {
        try (Arena arena = Arena.ofConfined();
             FileChannel channel = FileChannel.open(modelPath, StandardOpenOption.READ)) {
            MemorySegment mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), arena);
            return mapped.toArray(ValueLayout.JAVA_BYTE);
        }
    }

    public int inputSide() { return side; }

    /**
     * The patch of the frame the model was shown, in the landmarks' own space.
     *
     * <p>Same normalization as every landmark - both axes divided by the frame
     * width - so a diagnostic overlay can draw this rectangle with the very
     * transform it already uses for the skeleton, and the two cannot disagree.
     */
    public record ShownRegion(double x, double y, double size) { }

    private volatile ShownRegion lastShown;

    /** What the model saw on the most recent estimate, or null before the first one. */
    public ShownRegion lastShownRegion() { return lastShown; }

    /** Estimates landmarks from a BGRA camera frame. */
    public LandmarkFrame estimate(CameraFrame frame) throws Exception {
        var bgra = new Mat(frame.height(), frame.width(), CvType.CV_8UC4);
        bgra.put(0, 0, frame.bgraView());
        var bgr = new Mat();
        Imgproc.cvtColor(bgra, bgr, Imgproc.COLOR_BGRA2BGR);
        bgra.release();
        try {
            return estimate(bgr);
        } finally {
            bgr.release();
        }
    }

    LandmarkFrame estimate(Mat bgrFrame) throws Exception {
        int sourceWidth = bgrFrame.width();
        int sourceHeight = bgrFrame.height();
        // Nothing below the bottom of the frame was photographed, so nothing
        // may be reported there. Both axes are normalized by the width, so the
        // floor of the picture sits at height/width rather than at 1.
        double lowest = (double) sourceHeight / sourceWidth;

        // Where the body was last seen, or the whole frame until one has been.
        var region = crop.regionFor(sourceWidth, sourceHeight);
        lastShown = new ShownRegion(region.x() / sourceWidth, region.y() / sourceWidth, region.size() / sourceWidth);
        byte[] pixels = render(bgrFrame, region, side);

        try (OnnxTensor tensor = buildTensor(pixels, new long[]{1, side, side, 3});
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            float[][][][] output = (float[][][][]) result.get(0).getValue();
            var points = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
            KEYPOINTS.forEach((index, name) -> {
                float[] triple = output[0][0][index];
                // MoveNet emits (y, x, score) - y first. Swapping these rotates
                // the whole skeleton ninety degrees, the classic bug here.
                // The model answers in fractions of the region it was shown, so
                // undoing the crop is the region's own offset and span.
                double pixelX = region.x() + triple[1] * region.size();
                double pixelY = region.y() + triple[0] * region.size();
                // Both axes are divided by the same number so the body keeps its
                // true proportions; dividing y by the height instead would stretch
                // the figure and quietly corrupt every joint angle.
                points.put(name, new Landmark(clamp(pixelX / sourceWidth, 1), clamp(pixelY / sourceWidth, lowest), triple[2]));
            });
            extend(points, LandmarkName.LEFT_ELBOW, LandmarkName.LEFT_WRIST, LandmarkName.LEFT_HAND, lowest);
            extend(points, LandmarkName.RIGHT_ELBOW, LandmarkName.RIGHT_WRIST, LandmarkName.RIGHT_HAND, lowest);
            extend(points, LandmarkName.LEFT_KNEE, LandmarkName.LEFT_ANKLE, LandmarkName.LEFT_TOE, lowest);
            extend(points, LandmarkName.RIGHT_KNEE, LandmarkName.RIGHT_ANKLE, LandmarkName.RIGHT_TOE, lowest);
            crop.observe(points, sourceWidth, sourceHeight);
            return new LandmarkFrame(Instant.now(), points);
        }
    }

    /**
     * Draws {@code region} of the frame into the square the model expects.
     *
     * <p>The region is scaled uniformly rather than stretched to fit: a
     * stretched body has wrong joint angles, and a ninety-degree knee can read
     * as a hundred. Whatever part of the region lies outside the frame is left
     * as padding, which is what lets the region sit half off the edge and still
     * hold a body standing there.
     */
    static byte[] render(Mat bgrFrame, PersonCrop.Region region, int side) {
        double scale = side / region.size();
        var rgb = new Mat();
        Imgproc.cvtColor(bgrFrame, rgb, Imgproc.COLOR_BGR2RGB);
        var canvas = new Mat(side, side, rgb.type(), PAD);

        // The part of the region that is actually in the frame.
        int left = (int) Math.max(0, Math.floor(region.x()));
        int top = (int) Math.max(0, Math.floor(region.y()));
        int right = (int) Math.min(bgrFrame.width(), Math.ceil(region.x() + region.size()));
        int bottom = (int) Math.min(bgrFrame.height(), Math.ceil(region.y() + region.size()));
        if (right > left && bottom > top) {
            int x = clampToCanvas((int) Math.round((left - region.x()) * scale), side);
            int y = clampToCanvas((int) Math.round((top - region.y()) * scale), side);
            int width = Math.min(side - x, Math.max(1, (int) Math.round((right - left) * scale)));
            int height = Math.min(side - y, Math.max(1, (int) Math.round((bottom - top) * scale)));
            if (width > 0 && height > 0) {
                var patch = rgb.submat(top, bottom, left, right);
                var scaled = new Mat();
                Imgproc.resize(patch, scaled, new Size(width, height));
                scaled.copyTo(canvas.submat(y, y + height, x, x + width));
                scaled.release();
                patch.release();
            }
        }

        var pixels = new byte[(int) (canvas.total() * canvas.channels())];
        canvas.get(0, 0, pixels);
        rgb.release();
        canvas.release();
        return pixels;
    }

    private static int clampToCanvas(int value, int side) { return Math.min(side - 1, Math.max(0, value)); }

    /** MoveNet has no hand or toe keypoint, so continue the limb past its last joint. */
    private static void extend(EnumMap<LandmarkName, Landmark> points, LandmarkName from, LandmarkName through, LandmarkName tip, double lowest) {
        var a = points.get(from);
        var b = points.get(through);
        if (a == null || b == null) return;
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double length = Math.max(.001, Math.hypot(dx, dy));
        points.put(tip, new Landmark(clamp(b.x() + dx / length * EXTENSION, 1),
                clamp(b.y() + dy / length * EXTENSION, lowest), b.confidence()));
    }

    private static double clamp(double value, double highest) { return Math.min(highest, Math.max(0, value)); }

    /** Builds the input tensor in whatever dtype this particular export declared. */
    private OnnxTensor buildTensor(byte[] pixels, long[] shape) throws Exception {
        return switch (inputType) {
            case INT32 -> {
                IntBuffer buffer = IntBuffer.allocate(pixels.length);
                for (byte pixel : pixels) buffer.put(pixel & 0xFF);
                buffer.rewind();
                yield OnnxTensor.createTensor(environment, buffer, shape);
            }
            case UINT8 -> OnnxTensor.createTensor(environment, ByteBuffer.wrap(pixels), shape, OnnxJavaType.UINT8);
            case FLOAT -> {
                FloatBuffer buffer = FloatBuffer.allocate(pixels.length);
                for (byte pixel : pixels) buffer.put((pixel & 0xFF) / 255.0f);
                buffer.rewind();
                yield OnnxTensor.createTensor(environment, buffer, shape);
            }
            default -> throw new IllegalStateException("Unsupported model input type: " + inputType);
        };
    }

    @Override public void close() throws Exception { session.close(); }
}
