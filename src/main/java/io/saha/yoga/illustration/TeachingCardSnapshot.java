package io.saha.yoga.illustration;

import io.saha.yoga.domain.RoutineItem;
import io.saha.yoga.routine.PoseCatalog;
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
import java.util.List;
import java.util.Objects;

/**
 * Renders the coaching teaching card so it can be reviewed without running a
 * whole practice session.
 *
 * The card is built by the application class and driven here reflectively,
 * rather than rebuilt from a copy. A copy is worse than no snapshot at all: it
 * reviewed a card with fewer labels and ninety more pixels of height than the
 * one practice actually draws, and so reported a clean layout while the real
 * card was spilling its illustration past its own background.
 *
 * The box below each card stands in for the observation panel that follows it
 * on the coaching page, so anything overflowing lands somewhere visible
 * instead of off the bottom of the image.
 */
public final class TeachingCardSnapshot extends Application {
    /** The height coaching gives this card; see showCoach(). */
    private static final double CARD_WIDTH = 560, CARD_PREF_HEIGHT = 215, CARD_MIN_HEIGHT = 200, CARD_MAX_HEIGHT = 230, CARD_SPACING = 8;

    @Override public void start(Stage stage) throws Exception {
        var appClass = Class.forName("io.saha.yoga.SahaApp");
        var app = appClass.getDeclaredConstructor().newInstance();
        Method update = appClass.getDeclaredMethod("updateTeachingView", RoutineItem.class);
        update.setAccessible(true);
        var teachingField = appClass.getDeclaredField("teachingView");
        teachingField.setAccessible(true);
        var catalog = new PoseCatalog();

        var row = new HBox(20);
        row.setPadding(new Insets(24));
        row.setStyle("-fx-background-color: #102523;");
        for (var poseId : List.of("rest", "chair", "warrior_two", "downward_dog")) {
            var card = new VBox(CARD_SPACING);
            card.getStyleClass().add("teaching-view");
            card.setPrefSize(CARD_WIDTH, CARD_PREF_HEIGHT);
            card.setMinHeight(CARD_MIN_HEIGHT); card.setMaxHeight(CARD_MAX_HEIGHT);
            teachingField.set(app, card);
            update.invoke(app, new RoutineItem(catalog.require(poseId), 50, "STANDING", "review snapshot"));

            var below = new Label("OBSERVATION PANEL BELOW THE CARD");
            below.getStyleClass().add("observation-title");
            var panel = new VBox(5, below);
            panel.getStyleClass().add("camera-observation");
            panel.setPrefSize(CARD_WIDTH, 110); panel.setMinHeight(110);
            var column = new VBox(12, card, panel);
            VBox.setVgrow(card, Priority.NEVER);
            row.getChildren().add(column);
        }
        var scene = new Scene(row, 2440, 420, Color.web("#102523"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        row.applyCss(); row.layout();
        var snapshot = row.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/teaching-card.png" : getParameters().getRaw().getFirst());
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
