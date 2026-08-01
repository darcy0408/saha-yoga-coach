package io.saha.yoga.illustration;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

public final class PoseGallerySnapshot extends Application {
    @Override public void start(Stage stage) throws Exception {
        var gallery = new HBox(18);
        gallery.setPadding(new Insets(24));
        gallery.setStyle("-fx-background-color: #102523;");
        for (var draft : new TeachingPoseDraftCatalog().all()) {
            var title = new Label(draft.displayName()); title.getStyleClass().add("teaching-pose-name");
            var subtitle = new Label(draft.view() + " · gaze: " + draft.gaze()); subtitle.getStyleClass().add("teaching-review");
            var art = new TeachingPoseDraftView(draft); art.setPrefSize(360,330); art.getStyleClass().add("pose-draft-canvas");
            var card = new VBox(7,title,subtitle,art); card.setPrefSize(380,390); card.getStyleClass().add("pose-draft-card");
            gallery.getChildren().add(card);
        }
        var scene = new Scene(gallery, 1240, 450, Color.web("#102523"));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        gallery.applyCss(); gallery.layout();
        var snapshot = gallery.snapshot(new SnapshotParameters(), new WritableImage(1240,450));
        var destination = new File(getParameters().getRaw().isEmpty() ? "build/review/pose-gallery.png" : getParameters().getRaw().getFirst());
        var parent = destination.getParentFile(); if (parent != null) parent.mkdirs();
        ImageIO.write(toBufferedImage(snapshot), "png", destination);
        System.out.println(destination.getAbsolutePath());
        Platform.exit();
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width=(int)image.getWidth(),height=(int)image.getHeight();
        var output=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        var reader=image.getPixelReader();
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) output.setRGB(x,y,reader.getArgb(x,y));
        return output;
    }

    public static void main(String[] args) { launch(args); }
}
