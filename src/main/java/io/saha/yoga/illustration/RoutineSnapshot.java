package io.saha.yoga.illustration;

import io.saha.yoga.routine.PoseCatalog;
import io.saha.yoga.routine.RoutineGenerator;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Renders the whole generated practice in order, so the arc can be reviewed at a glance. */
public final class RoutineSnapshot extends Application {
    @Override public void start(Stage stage) throws Exception {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of(), List.of());
        var icons = new PoseIconCatalog();
        var assets = new TeachingAssetCatalog();
        var grid = new TilePane();
        grid.setHgap(10); grid.setVgap(10); grid.setPrefColumns(6);
        grid.setPadding(new Insets(22));
        grid.setStyle("-fx-background-color: #102523;");
        int index = 1;
        for (var item : routine.items()) {
            var card = new VBox(4);
            card.getStyleClass().add("pose-draft-card");
            card.setPrefSize(196, 250);
            card.setAlignment(Pos.CENTER);
            var phase = new Label(index + " · " + item.phase().toUpperCase()); phase.getStyleClass().add("badge");
            var icon = icons.forPose(item.pose().id());
            var asset = icon.isPresent() ? java.util.Optional.<TeachingAsset>empty() : assets.enabledForCoaching(item.pose().id());
            if (icon.isPresent()) {
                var view = new PoseIconView();
                view.show(icon.get());
                view.setMinSize(140, 140); view.setPrefSize(140, 140);
                card.getChildren().addAll(phase, view);
            } else if (asset.isPresent()) {
                var stream = Objects.requireNonNull(getClass().getResourceAsStream(asset.get().resourcePath()));
                var art = new ImageView(new Image(stream));
                art.setPreserveRatio(true); art.setFitWidth(130); art.setFitHeight(130);
                var pane = new StackPane(art); pane.setPrefSize(140, 140);
                card.getChildren().addAll(phase, pane);
            } else {
                var written = new Label("written\nguidance"); written.setPrefSize(140, 140); written.setAlignment(Pos.CENTER);
                written.getStyleClass().add("teaching-review");
                card.getChildren().addAll(phase, written);
            }
            var name = new Label(item.pose().displayName()); name.setWrapText(true); name.getStyleClass().add("teaching-pose-name");
            name.setStyle("-fx-font-size: 15px;");
            var time = new Label(item.durationSeconds() + "s"); time.getStyleClass().add("support-label");
            card.getChildren().addAll(name, time);
            grid.getChildren().add(card);
            index++;
        }
        var header = new Label(routine.name() + "  ·  " + routine.items().size() + " poses  ·  "
                + routine.totalSeconds() / 60 + " min " + routine.totalSeconds() % 60 + " s of holds");
        header.getStyleClass().add("hero-small");
        header.setPadding(new Insets(18, 0, 0, 22));
        var page = new VBox(header, grid);
        page.setStyle("-fx-background-color: #102523;");
        var scene = new Scene(page, 1290, 1160, Color.web("#102523"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        page.applyCss(); page.layout();
        var snapshot = page.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/routine.png" : getParameters().getRaw().getFirst());
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
