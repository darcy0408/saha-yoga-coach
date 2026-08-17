package io.saha.yoga.illustration;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

/**
 * Renders the teaching card for an illustrated pose and a written-only pose
 * side by side, so the coaching visual can be reviewed without running a
 * whole practice session.
 */
public final class TeachingCardSnapshot extends Application {
    @Override public void start(Stage stage) throws Exception {
        var catalog = new TeachingAssetCatalog();
        var row = new HBox(20, card(catalog, "chair", "Chair",
                        "Sit the hips back, bend the knees, and keep the chest lifted."),
                card(catalog, "warrior_two", "Warrior II",
                        "Stack the front knee over the ankle and extend through both arms."),
                card(catalog, "downward_dog", "Downward Dog",
                        "Press the floor away and lift your hips up and back."));
        row.setPadding(new Insets(24));
        row.setStyle("-fx-background-color: #102523;");
        var scene = new Scene(row, 1840, 340, Color.web("#102523"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        row.applyCss(); row.layout();
        var snapshot = row.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/teaching-card.png" : getParameters().getRaw().getFirst());
        var parent = destination.getParentFile(); if (parent != null) parent.mkdirs();
        ImageIO.write(toBufferedImage(snapshot), "png", destination);
        System.out.println(destination.getAbsolutePath());
        Platform.exit();
    }

    private VBox card(TeachingAssetCatalog catalog, String poseId, String displayName, String instruction) {
        var icon = new PoseIconCatalog().forPose(poseId);
        var asset = icon.isPresent() ? java.util.Optional.<TeachingAsset>empty() : catalog.enabledForCoaching(poseId);
        var heading = new Label("TEACHING GUIDE"); heading.getStyleClass().add("badge");
        var title = new Label(displayName); title.getStyleClass().add("teaching-pose-name");
        var text = new Label(instruction); text.setWrapText(true); text.setMinHeight(Region.USE_PREF_SIZE); text.getStyleClass().add("teaching-instruction");
        boolean illustrated = icon.isPresent() || asset.isPresent();
        var boundary = new Label(illustrated
                ? "License-verified reference illustration"
                : "Illustration under review. Follow the written setup or skip this pose.");
        boundary.setWrapText(true); boundary.setMinHeight(Region.USE_PREF_SIZE);
        boundary.getStyleClass().add(illustrated ? "visual-approved" : "visual-review-warning");
        var column = new VBox(10, title, text, boundary);
        HBox.setHgrow(column, Priority.ALWAYS);
        var body = new HBox(14, column);
        icon.ifPresent(value -> {
            var view = new PoseIconView();
            view.show(value);
            view.setMinSize(180, 180); view.setPrefSize(200, 200);
            var credit = new Label(PoseIconCatalog.CREDIT); credit.getStyleClass().add("support-label");
            var iconColumn = new VBox(4, view, credit); iconColumn.setAlignment(Pos.CENTER);
            body.getChildren().add(iconColumn);
        });
        asset.ifPresent(value -> {
            var stream = Objects.requireNonNull(getClass().getResourceAsStream(value.resourcePath()));
            var art = new ImageView(new Image(stream));
            art.setPreserveRatio(true); art.setFitWidth(220); art.setFitHeight(185);
            var artPane = new StackPane(art); artPane.getStyleClass().add("licensed-art-canvas");
            var credit = new Label("CC0 · " + value.creator()); credit.getStyleClass().add("support-label");
            var artColumn = new VBox(4, artPane, credit); artColumn.setAlignment(Pos.CENTER);
            body.getChildren().add(artColumn);
        });
        VBox.setVgrow(body, Priority.ALWAYS);
        // The illustration draws the floor it actually rests on; a second rule
        // across the whole card sat at a different height and contradicted it.
        var view = new VBox(10, heading, body);
        view.getStyleClass().add("teaching-view");
        view.setPrefSize(560, 280);
        return view;
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
