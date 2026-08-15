package io.saha.yoga.illustration;

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
import java.util.Objects;

/** Renders every Atlas pose icon so the set can be reviewed as one image. */
public final class IconGallerySnapshot extends Application {
    @Override public void start(Stage stage) throws Exception {
        var catalog = new PoseIconCatalog();
        var gallery = new TilePane();
        gallery.setHgap(12); gallery.setVgap(12); gallery.setPrefColumns(5);
        gallery.setPadding(new Insets(20));
        gallery.setStyle("-fx-background-color: #102523;");
        for (var name : catalog.iconNames()) {
            var view = new PoseIconView();
            view.show(catalog.forIcon(name).orElseThrow());

            view.setMinSize(180, 180); view.setPrefSize(180, 180);
            var label = new Label(name); label.getStyleClass().add("teaching-review");
            var card = new VBox(6, view, label);
            card.getStyleClass().add("pose-draft-card");
            card.setPrefSize(210, 230);
            gallery.getChildren().add(card);
        }
        var scene = new Scene(gallery, 1140, 760, Color.web("#102523"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        gallery.applyCss(); gallery.layout();
        var snapshot = gallery.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/icon-gallery.png" : getParameters().getRaw().getFirst());
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
