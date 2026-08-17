package io.saha.yoga.vision;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A model that is missing and a model that is broken must not look the same.
 *
 * They did: both returned an empty Optional, so a corrupt download presented
 * to the user as "no model installed" and the fetch script it pointed at would
 * happily report success.
 */
class CameraStartupTest {
    @Test void weightsThatDoNotLoadAreReportedAsBrokenRatherThanMissing(@TempDir Path temporary) throws Exception {
        var rubbish = temporary.resolve("movenet-singlepose-lightning.onnx");
        Files.writeString(rubbish, "this is not a serialised ONNX graph");
        System.setProperty(PoseModelLocator.PROPERTY, rubbish.toString());
        try {
            var startup = CameraLandmarkSource.open(0);
            var unusable = assertInstanceOf(CameraLandmarkSource.Startup.Unusable.class, startup,
                    "a file that exists but will not load is a broken install, not an absent one");
            assertEquals(rubbish.toAbsolutePath().normalize(), unusable.model(), "the message should name the file it tried");
            assertFalse(unusable.reason().isBlank(), "the reason is the whole point of this branch");
        } finally {
            System.clearProperty(PoseModelLocator.PROPERTY);
        }
    }

    @Test void nothingOnDiskIsReportedAsMissingWithThePlacesTried(@TempDir Path temporary) {
        System.setProperty(PoseModelLocator.PROPERTY, temporary.resolve("absent.onnx").toString());
        try {
            // the working directory is also searched, so this only asserts the
            // missing branch when this machine genuinely has no weights
            var startup = CameraLandmarkSource.open(0);
            if (startup instanceof CameraLandmarkSource.Startup.NoModel none) {
                assertFalse(none.searched().isEmpty(), "a missing model must still say where it looked");
            } else {
                assertInstanceOf(CameraLandmarkSource.Startup.Ready.class, startup,
                        "with real weights on this machine the working directory should still resolve");
            }
        } finally {
            System.clearProperty(PoseModelLocator.PROPERTY);
        }
    }

    @Test void openingDoesNotTouchTheCameraUntilItIsStarted() {
        // building the source must not turn the webcam on: the calibration
        // screen promises no camera is opened until you ask for one
        var startup = CameraLandmarkSource.open(0);
        if (startup instanceof CameraLandmarkSource.Startup.Ready ready) {
            assertFalse(ready.source().isOpen(), "open() should build the source, not start capture");
            assertFalse(ready.source().hasLandmarks(), "nothing has been observed yet");
            ready.source().close();
        }
    }
}
