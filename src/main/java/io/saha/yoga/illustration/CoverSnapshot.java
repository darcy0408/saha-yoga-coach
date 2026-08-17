package io.saha.yoga.illustration;

import io.saha.yoga.domain.RoutineItem;
import io.saha.yoga.routine.PoseCatalog;
import io.saha.yoga.vision.DemoLandmarkSource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Renders the cover image: what the practice screen actually shows.
 *
 * The observed figure on the left with the target shape behind it, the teaching
 * card on the right. Both are drawn by the application class itself rather than
 * rebuilt here, so the cover cannot flatter the app by showing something the
 * app does not draw.
 */
public final class CoverSnapshot extends Application {
    private static final double WIDTH = 1200, HEIGHT = 900;

    @Override public void start(Stage stage) throws Exception {
        var appClass = Class.forName("io.saha.yoga.SahaApp");
        var app = appClass.getDeclaredConstructor().newInstance();
        Method draw = appClass.getDeclaredMethod("drawFrame", io.saha.yoga.domain.LandmarkFrame.class, boolean.class);
        draw.setAccessible(true);
        Method update = appClass.getDeclaredMethod("updateTeachingView", RoutineItem.class);
        update.setAccessible(true);
        var demoField = appClass.getDeclaredField("demoSource");
        demoField.setAccessible(true);
        var demo = (DemoLandmarkSource) demoField.get(app);
        var bodyField = appClass.getDeclaredField("bodyView");
        bodyField.setAccessible(true);
        var sourceField = appClass.getDeclaredField("landmarks");
        sourceField.setAccessible(true);
        var teachingField = appClass.getDeclaredField("teachingView");
        teachingField.setAccessible(true);

        var catalog = new PoseCatalog();
        var pose = catalog.require("warrior_two");
        var source = new DemoLandmarkSource();
        source.selectPose(pose.id());
        sourceField.set(app, source);
        demo.selectPose(pose.id());

        var figure = new javafx.scene.layout.Pane();
        figure.setMinSize(640, 780); figure.setPrefSize(640, 780);
        bodyField.set(app, figure);

        var card = new VBox(8);
        card.getStyleClass().add("teaching-view");
        // taller than the practice screen allows: this card is 90px narrower
        // here, so its text wraps further and needs the room
        card.setPrefSize(470, 275); card.setMinHeight(275); card.setMaxHeight(310);
        teachingField.set(app, card);
        update.invoke(app, new RoutineItem(pose, 50, "STANDING", "cover"));

        var observationTitle = new Label("YOU, AS THE COACH SEES YOU · NO ROOM, NO FURNITURE");
        observationTitle.getStyleClass().add("observation-title");
        var left = new VBox(6, observationTitle, figure);
        VBox.setVgrow(figure, Priority.ALWAYS);

        var spoken = new Label("“Great! Now hold that pose.”");
        spoken.getStyleClass().add("hero-small");
        spoken.setWrapText(true);
        var caption = new Label("Spoken aloud, so it works from inside the pose. Landmarks are estimated on this machine and no video leaves it.");
        caption.setWrapText(true);
        var right = new VBox(24, card, spoken, caption);
        right.setPrefWidth(470); right.setMaxWidth(470);

        var row = new HBox(24, left, right);
        row.setPadding(new Insets(28));
        row.setStyle("-fx-background-color: #091817;");
        var scene = new Scene(row, WIDTH, HEIGHT, Color.web("#091817"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        // laid out before the figure is drawn: drawFrame sizes itself from the
        // pane, and a pane that has not been laid out yet is still zero wide
        row.applyCss(); row.layout();
        draw.invoke(app, offBy(source.targetFrame()), true);
        row.applyCss(); row.layout();
        var snapshot = row.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/cover.png" : getParameters().getRaw().getFirst());
        var parent = destination.getParentFile(); if (parent != null) parent.mkdirs();
        ImageIO.write(toBufferedImage(snapshot), "png", destination);
        System.out.println(destination.getAbsolutePath());
        Platform.exit();
    }

    /** Nudges the limbs off target so the guide has something to differ from. */
    private io.saha.yoga.domain.LandmarkFrame offBy(io.saha.yoga.domain.LandmarkFrame frame) {
        var moved = new java.util.EnumMap<io.saha.yoga.domain.LandmarkName, io.saha.yoga.domain.Landmark>(io.saha.yoga.domain.LandmarkName.class);
        frame.landmarks().forEach((name, point) -> {
            double dx = 0, dy = 0;
            switch (name) {
                case LEFT_KNEE, RIGHT_KNEE -> { dx = .03; dy = -.03; }
                case LEFT_ANKLE, RIGHT_ANKLE, LEFT_TOE, RIGHT_TOE -> { dx = .05; dy = -.02; }
                case LEFT_WRIST, RIGHT_WRIST, LEFT_HAND, RIGHT_HAND -> { dx = -.03; dy = .05; }
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
