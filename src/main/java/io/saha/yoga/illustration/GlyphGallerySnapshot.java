package io.saha.yoga.illustration;

import io.saha.yoga.routine.PoseCatalog;
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
import java.util.Objects;

/** Renders the generated glyph for every catalog pose so the set can be reviewed as one image. */
public final class GlyphGallerySnapshot extends Application {
    @Override public void start(Stage stage) throws Exception {
        var source = new DemoLandmarkSource();
        var gallery = new TilePane();
        gallery.setHgap(14); gallery.setVgap(14); gallery.setPrefColumns(4);
        gallery.setPadding(new Insets(24));
        gallery.setStyle("-fx-background-color: #102523;");
        for (var pose : new PoseCatalog().all()) {
            source.selectPose(pose.id());
            var glyph = new PoseGlyphView();
            glyph.show(source.targetFrame(), source.faceDirection());
            glyph.setMinSize(210, 190); glyph.setPrefSize(210, 190);
            var name = new Label(pose.displayName()); name.getStyleClass().add("teaching-pose-name");
            var caption = new Label("TARGET LANDMARKS"); caption.getStyleClass().add("support-label");
            var card = new VBox(6, name, glyph, caption);
            card.getStyleClass().add("pose-draft-card"); card.setPrefSize(276, 272);
            gallery.getChildren().add(card);
        }
        var scene = new Scene(gallery, 1250, 920, Color.web("#102523"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        gallery.applyCss(); gallery.layout();
        var snapshot = gallery.snapshot(new SnapshotParameters(), null);
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/glyph-gallery.png" : getParameters().getRaw().getFirst());
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
