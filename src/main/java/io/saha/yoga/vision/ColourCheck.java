package io.saha.yoga.vision;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

/**
 * Diagnostic: pushes known bytes through the exact path the preview uses and
 * prints what comes out, so the channel order is settled by evidence rather
 * than by reading documentation about what it ought to be.
 */
public final class ColourCheck extends Application {
    @Override public void start(Stage stage) {
        // one pure-blue and one pure-red pixel, laid out the way
        // OpenCvCameraCapture writes them: blue, green, red, alpha
        byte[] bgra = {
                (byte) 255, 0, 0, (byte) 255,
                0, 0, (byte) 255, (byte) 255
        };
        var image = new WritableImage(2, 1);
        image.getPixelWriter().setPixels(0, 0, 2, 1, PixelFormat.getByteBgraInstance(), bgra, 0, 8);
        var reader = image.getPixelReader();
        System.out.printf("byte[] says pixel0=blue, pixel1=red%n");
        System.out.printf("rendered pixel0 = %08X%n", reader.getArgb(0, 0));
        System.out.printf("rendered pixel1 = %08X%n", reader.getArgb(1, 0));
        System.out.printf("verdict: %s%n",
                reader.getArgb(0, 0) == 0xFF0000FF ? "CORRECT (BGRA in, BGRA read)" : "SWAPPED");
        Platform.exit();
    }

    public static void main(String[] args) { launch(args); }
}
