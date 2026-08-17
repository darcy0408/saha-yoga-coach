package io.saha.yoga.illustration;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

/**
 * Draws an Atlas pose icon as strokes, resting on a floor line.
 *
 * The icons are authored in a 24x24 box, so the whole group is scaled by one
 * factor; stroke width is divided back out so the line keeps a constant
 * on-screen weight at any size.
 *
 * <p>The figure is placed by its own ink rather than by that box. These are
 * third-party icons with no shared baseline - a seated figure occupies the
 * middle of its box, a standing one fills it - so centring the box left a
 * seated body hovering with nothing relating it to the floor the card drew
 * separately underneath. Resting the lowest ink on a floor drawn in this same
 * pane makes the relationship real for every icon, whatever it contains.
 */
public final class PoseIconView extends Pane {
    private static final double BOX = 24;
    private static final Color INK = Color.web("#173532");
    private static final Color GROUND = Color.web("#725b25");
    /** Leaves room for the floor line and keeps tall icons off the top edge. */
    private static final double FILL = .86;

    private PoseIconCatalog.Icon icon;

    public PoseIconView() {
        setMinSize(90, 90);
        setPrefSize(190, 190);
        widthProperty().addListener((ignored, oldValue, newValue) -> redraw());
        heightProperty().addListener((ignored, oldValue, newValue) -> redraw());
    }

    public void show(PoseIconCatalog.Icon value) {
        icon = value;
        redraw();
    }

    private void redraw() {
        getChildren().clear();
        if (icon == null) return;
        double width = Math.max(90, getWidth());
        double height = Math.max(90, getHeight());
        double scale = Math.min(width, height) * FILL / BOX;
        double floorY = height - Math.max(9, height * .06);

        var group = new Group();
        for (var data : icon.paths()) {
            var shape = new SVGPath();
            shape.setContent(data);
            shape.setFill(null);
            shape.setStroke(INK);
            shape.setStrokeWidth(1.15 / scale);
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
            group.getChildren().add(shape);
        }
        for (var head : icon.circles()) {
            var circle = new Circle(head.centreX(), head.centreY(), head.radius());
            circle.setFill(null);
            circle.setStroke(INK);
            circle.setStrokeWidth(1.15 / scale);
            group.getChildren().add(circle);
        }
        // A Scale in the transform list pivots on the origin, unlike setScaleX,
        // which pivots on the centre of the bounds and would make the placement
        // below depend on the very bounds it is trying to position.
        var ink = group.getBoundsInLocal();
        group.getTransforms().add(new Scale(scale, scale));
        group.setTranslateX(width / 2 - (ink.getMinX() + ink.getMaxX()) / 2 * scale);
        group.setTranslateY(floorY - ink.getMaxY() * scale);

        var floor = new Line(width * .06, floorY, width * .94, floorY);
        floor.setStroke(GROUND);
        floor.setStrokeWidth(1.5);
        floor.getStrokeDashArray().addAll(7.0, 5.0);
        getChildren().addAll(floor, group);
    }
}
