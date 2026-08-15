package io.saha.yoga.illustration;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Draws an Atlas pose icon as strokes, scaled to fill this pane.
 *
 * The icons are authored in a 24x24 box, so the whole group is scaled by
 * one factor and centred; stroke width is divided back out so the line
 * keeps a constant on-screen weight at any size.
 */
public final class PoseIconView extends Pane {
    private static final double BOX = 24;
    private static final Color INK = Color.web("#173532");

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
        double side = Math.min(Math.max(90, getWidth()), Math.max(90, getHeight()));
        double scale = side / BOX;
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
        group.setScaleX(scale);
        group.setScaleY(scale);
        // Group scaling pivots on the centre of its bounds, so translate the
        // scaled centre to the pane centre rather than to the origin.
        group.setTranslateX((getWidth() - side) / 2 + side / 2 - BOX / 2);
        group.setTranslateY((getHeight() - side) / 2 + side / 2 - BOX / 2);
        getChildren().add(group);
    }
}
