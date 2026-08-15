package io.saha.yoga.illustration;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pose reduced to the fewest strokes that still teach it: one spine, two
 * arms, two legs, and an unfilled head circle.
 *
 * The figure is derived from the same landmark frame the analyzer coaches
 * against, so unlike a hand-drawn illustration it cannot drift from the
 * target it is supposed to depict. Shoulders and hips collapse to their
 * midpoints (neck and pelvis); each arm hangs from the neck and each leg
 * from the pelvis. Coordinates stay in the frame's normalized 0..1 space,
 * y increasing downward.
 *
 * A limb whose landmarks are partly missing keeps its reachable prefix
 * (e.g. neck to elbow) rather than failing, so low-visibility camera frames
 * can still render.
 */
public record GlyphFigure(List<Stroke> strokes, Optional<Point> head, double headRadius) {

    public enum Limb { SPINE, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }

    public record Point(double x, double y) { }

    public record Stroke(Limb limb, List<Point> points) { }

    /** Head circle radius in normalized units, centred on the nose landmark. */
    static final double HEAD_RADIUS = .052;

    public static GlyphFigure of(LandmarkFrame frame) {
        var p = frame.landmarks();
        var strokes = new ArrayList<Stroke>();
        var neck = midpoint(p, LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER);
        var pelvis = midpoint(p, LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP);
        if (neck != null && pelvis != null) strokes.add(new Stroke(Limb.SPINE, List.of(neck, pelvis)));
        stroke(strokes, Limb.LEFT_ARM, neck, p, LandmarkName.LEFT_ELBOW, LandmarkName.LEFT_WRIST, LandmarkName.LEFT_HAND);
        stroke(strokes, Limb.RIGHT_ARM, neck, p, LandmarkName.RIGHT_ELBOW, LandmarkName.RIGHT_WRIST, LandmarkName.RIGHT_HAND);
        stroke(strokes, Limb.LEFT_LEG, pelvis, p, LandmarkName.LEFT_KNEE, LandmarkName.LEFT_ANKLE, LandmarkName.LEFT_TOE);
        stroke(strokes, Limb.RIGHT_LEG, pelvis, p, LandmarkName.RIGHT_KNEE, LandmarkName.RIGHT_ANKLE, LandmarkName.RIGHT_TOE);
        var nose = p.get(LandmarkName.NOSE);
        return new GlyphFigure(List.copyOf(strokes),
                Optional.ofNullable(nose == null ? null : new Point(nose.x(), nose.y())), HEAD_RADIUS);
    }

    private static void stroke(List<Stroke> strokes, Limb limb, Point root, Map<LandmarkName, Landmark> p, LandmarkName... joints) {
        if (root == null) return;
        var points = new ArrayList<Point>();
        points.add(root);
        for (var joint : joints) {
            var mark = p.get(joint);
            if (mark == null) break;
            points.add(new Point(mark.x(), mark.y()));
        }
        if (points.size() > 1) strokes.add(new Stroke(limb, List.copyOf(points)));
    }

    private static Point midpoint(Map<LandmarkName, Landmark> p, LandmarkName left, LandmarkName right) {
        var a = p.get(left);
        var b = p.get(right);
        if (a == null || b == null) return null;
        return new Point((a.x() + b.x()) / 2, (a.y() + b.y()) / 2);
    }
}
