package io.saha.yoga.illustration;

import io.saha.yoga.vision.DemoLandmarkSource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Renders the observed-landmarks figure for a few poses, so its styling can be
 * reviewed without sitting through a practice.
 *
 * The drawing lives in the application class, so this reaches it reflectively
 * rather than duplicating it and reviewing a copy that could drift.
 */
public final class FigureSnapshot extends Application {
    @Override public void start(Stage stage) throws Exception {
        var appClass = Class.forName("io.saha.yoga.SahaApp");
        var app = appClass.getDeclaredConstructor().newInstance();
        Method draw = appClass.getDeclaredMethod("drawFrame", io.saha.yoga.domain.LandmarkFrame.class);
        draw.setAccessible(true);
        var bodyField = appClass.getDeclaredField("bodyView");
        bodyField.setAccessible(true);
        var sourceField = appClass.getDeclaredField("landmarks");
        sourceField.setAccessible(true);

        var gallery = new TilePane();
        gallery.setHgap(14); gallery.setVgap(14); gallery.setPrefColumns(4);
        gallery.setPadding(new Insets(22));
        gallery.setStyle("-fx-background-color: #091817;");
        for (var pose : List.of("easy_seat", "tree", "chair", "downward_dog")) {
            var source = new DemoLandmarkSource();
            source.selectPose(pose);
            sourceField.set(app, source);
            var pane = new javafx.scene.layout.Pane();
            pane.setMinSize(300, 300); pane.setPrefSize(300, 300);
            bodyField.set(app, pane);
            draw.invoke(app, source.targetFrame());
            var label = new Label(pose.replace('_', ' ')); label.getStyleClass().add("observation-title");
            var card = new VBox(6, pane, label);
            card.setStyle("-fx-background-color: #091817;");
            gallery.getChildren().add(card);
        }
        var scene = new Scene(gallery, 1300, 380, Color.web("#091817"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        gallery.applyCss(); gallery.layout();
        var snapshot = gallery.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/figure.png" : getParameters().getRaw().getFirst());
        var parent = destination.getParentFile(); if (parent != null) parent.mkdirs();
        ImageIO.write(toBufferedImage(snapshot), "png", destination);
        System.out.println(destination.getAbsolutePath());
        Platform.exit();
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var reader = image.getPixelReader();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) output.setRGB(x, y, reader.getArgb(x, y));
        return output;
    }

    public static void main(String[] args) { launch(args); }
}
