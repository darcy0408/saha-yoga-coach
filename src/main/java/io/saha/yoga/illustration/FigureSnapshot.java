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
        Method draw = appClass.getDeclaredMethod("drawFrame", io.saha.yoga.domain.LandmarkFrame.class, boolean.class);
        draw.setAccessible(true);
        var demoField = appClass.getDeclaredField("demoSource");
        demoField.setAccessible(true);
        var demo = (DemoLandmarkSource) demoField.get(app);
        var bodyField = appClass.getDeclaredField("bodyView");
        bodyField.setAccessible(true);
        var sourceField = appClass.getDeclaredField("landmarks");
        sourceField.setAccessible(true);

        var gallery = new TilePane();
        gallery.setHgap(14); gallery.setVgap(14); gallery.setPrefColumns(8);
        gallery.setPadding(new Insets(22));
        gallery.setStyle("-fx-background-color: #091817;");
        // the poses whose shape has been wrong at some point: lunge depth,
        // squat depth, which way a body faces and whether its hands and feet
        // reach the floor are all things numbers hide and a picture does not
        for (var pose : List.of("seated_side_reach", "cat_cow", "warrior_one", "warrior_two", "goddess", "seated_fold", "tree", "downward_dog")) {
            var source = new DemoLandmarkSource();
            source.selectPose(pose);
            sourceField.set(app, source);
            demo.selectPose(pose);
            var pane = new javafx.scene.layout.Pane();
            pane.setMinSize(300, 300); pane.setPrefSize(300, 300);
            bodyField.set(app, pane);
            // a body that is close but not right, so the guide underneath has
            // something to differ from - which is the whole point of drawing it
            draw.invoke(app, offBy(source.targetFrame()), true);
            var label = new Label(pose.replace('_', ' ') + " · you vs target"); label.getStyleClass().add("observation-title");
            var card = new VBox(6, pane, label);
            card.setStyle("-fx-background-color: #091817;");
            gallery.getChildren().add(card);
        }
        var scene = new Scene(gallery, 2580, 380, Color.web("#091817"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        gallery.applyCss(); gallery.layout();
        var snapshot = gallery.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/figure.png" : getParameters().getRaw().getFirst());
        var parent = destination.getParentFile(); if (parent != null) parent.mkdirs();
        ImageIO.write(toBufferedImage(snapshot), "png", destination);
        System.out.println(destination.getAbsolutePath());
        Platform.exit();
    }

    /** Nudges the limbs off target and gives every point camera-like confidence. */
    private io.saha.yoga.domain.LandmarkFrame offBy(io.saha.yoga.domain.LandmarkFrame frame) {
        var moved = new java.util.EnumMap<io.saha.yoga.domain.LandmarkName, io.saha.yoga.domain.Landmark>(io.saha.yoga.domain.LandmarkName.class);
        // the torso stays put so the guide fits to the same hips, and only the
        // limbs differ - which is what a real correction looks like
        frame.landmarks().forEach((name, point) -> {
            double dx = 0, dy = 0;
            switch (name) {
                case LEFT_KNEE, RIGHT_KNEE -> { dx = .05; dy = -.05; }
                case LEFT_ANKLE, RIGHT_ANKLE, LEFT_TOE, RIGHT_TOE -> { dx = .09; dy = -.03; }
                case LEFT_WRIST, RIGHT_WRIST, LEFT_HAND, RIGHT_HAND -> { dx = -.06; dy = .09; }
                case LEFT_ELBOW, RIGHT_ELBOW -> { dx = -.03; dy = .05; }
                default -> { }
            }
            moved.put(name, new io.saha.yoga.domain.Landmark(
                    Math.min(1, Math.max(0, point.x() + dx)), Math.min(1, Math.max(0, point.y() + dy)), .9));
        });
        return new io.saha.yoga.domain.LandmarkFrame(java.time.Instant.now(), moved);
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
