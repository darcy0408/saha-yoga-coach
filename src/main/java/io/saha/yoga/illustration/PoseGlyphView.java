package io.saha.yoga.illustration;

import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.vision.FaceDirection;
import io.saha.yoga.vision.LandmarkSource;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.Set;

/**
 * Draws a {@link GlyphFigure} in Saha's minimal teaching style: uniform ink
 * strokes with round caps, an unfilled head, and a short ground line.
 *
 * The vertical mapping pins {@code LandmarkSource.FLOOR_Y} to the bottom of
 * the pane, so a grounded figure stands on the pane's lower edge at any size.
 */
public final class PoseGlyphView extends Pane {
    private static final Color INK = Color.web("#173532");
    private static final Color CUE = Color.web("#c88b32");
    private static final Color GROUND = Color.web("#d6c79e");
    /** Matches the cream teaching card; the filled head knocks out strokes passing behind it. */
    private static final Color PAPER = Color.web("#f5efe1");

    private GlyphFigure figure;
    private FaceDirection gaze = FaceDirection.FRONT;
    private Set<GlyphFigure.Limb> flagged = Set.of();

    public PoseGlyphView() {
        setMinSize(150, 130);
        setPrefSize(230, 195);
        widthProperty().addListener((ignored, oldValue, newValue) -> redraw());
        heightProperty().addListener((ignored, oldValue, newValue) -> redraw());
    }

    public void show(LandmarkFrame frame) { show(frame, FaceDirection.FRONT); }

    public void show(LandmarkFrame frame, FaceDirection direction) {
        figure = GlyphFigure.of(frame);
        gaze = direction;
        redraw();
    }

    /** Limbs drawn in the cue colour; the correction flow highlights one at a time. */
    public void flag(Set<GlyphFigure.Limb> limbs) {
        flagged = Set.copyOf(limbs);
        redraw();
    }

    private void redraw() {
        getChildren().clear();
        if (figure == null) return;
        double w = Math.max(150, getWidth()), h = Math.max(130, getHeight());
        double scale = Math.min(w, h / LandmarkSource.FLOOR_Y);
        double offsetX = (w - scale) / 2;
        double offsetY = h - scale * LandmarkSource.FLOOR_Y;
        double stroke = Math.max(3, scale * .030);

        var ground = new Line(offsetX + scale * .07, h - stroke * .3, offsetX + scale * .93, h - stroke * .3);
        ground.setStroke(GROUND);
        ground.setStrokeWidth(Math.max(2, stroke * .5));
        ground.setStrokeLineCap(StrokeLineCap.ROUND);
        getChildren().add(ground);

        for (var glyphStroke : figure.strokes()) {
            var line = new Polyline();
            for (var point : glyphStroke.points()) line.getPoints().addAll(offsetX + point.x() * scale, offsetY + point.y() * scale);
            line.setFill(Color.TRANSPARENT);
            line.setStroke(flagged.contains(glyphStroke.limb()) ? CUE : INK);
            line.setStrokeWidth(stroke);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            line.setStrokeLineJoin(StrokeLineJoin.ROUND);
            getChildren().add(line);
        }
        figure.head().ifPresent(head -> {
            double headX = offsetX + head.x() * scale, headY = offsetY + head.y() * scale;
            figure.strokes().stream().filter(s -> s.limb() == GlyphFigure.Limb.SPINE).findFirst().ifPresent(spine -> {
                var neckPoint = spine.points().getFirst();
                // runs to the head centre; the filled circle trims it at the rim
                var neck = new Line(offsetX + neckPoint.x() * scale, offsetY + neckPoint.y() * scale, headX, headY);
                neck.setStroke(INK);
                neck.setStrokeWidth(stroke);
                neck.setStrokeLineCap(StrokeLineCap.ROUND);
                getChildren().add(neck);
            });
            var circle = new Circle(headX, headY, figure.headRadius() * scale);
            circle.setFill(PAPER);
            circle.setStroke(INK);
            circle.setStrokeWidth(stroke);
            getChildren().add(circle);
            if (gaze != FaceDirection.FRONT) {
                // a short nose tick on the rim showing which way the face points
                double vx = gaze == FaceDirection.LEFT ? -1 : gaze == FaceDirection.RIGHT ? 1 : 0;
                double vy = gaze == FaceDirection.UP ? -1 : gaze == FaceDirection.DOWN ? 1 : 0;
                double rim = figure.headRadius() * scale;
                var tick = new Line(headX + vx * rim, headY + vy * rim, headX + vx * rim * 1.5, headY + vy * rim * 1.5);
                tick.setStroke(INK);
                tick.setStrokeWidth(stroke * .8);
                tick.setStrokeLineCap(StrokeLineCap.ROUND);
                getChildren().add(tick);
            }
        });
    }
}
