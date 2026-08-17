package io.saha.yoga.vision;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The search has to survive being launched from somewhere other than the
 * project root, because that is what an installed copy does.
 */
class PoseModelLocatorTest {
    @Test void looksBesideTheWorkingDirectoryAndBesideTheCode() {
        var candidates = PoseModelLocator.candidates();
        assertFalse(candidates.isEmpty(), "there must always be somewhere to look");
        assertTrue(candidates.stream().allMatch(Path::isAbsolute), "candidates are resolved, so a message can name them: " + candidates);
        assertTrue(candidates.stream().allMatch(path -> path.endsWith(Path.of("models", PoseModelLocator.FILE_NAME))
                        || path.getFileName().toString().equals(PoseModelLocator.FILE_NAME)),
                "every candidate should point at the model file: " + candidates);
        assertEquals(candidates.size(), candidates.stream().distinct().count(), "the same path should not be searched twice");
    }

    @Test void theWorkingDirectoryIsSearchedFirstWithoutAnOverride() {
        assertNull(System.getProperty(PoseModelLocator.PROPERTY), "this test assumes no override is set");
        assertEquals(Path.of(System.getProperty("user.dir"), "models", PoseModelLocator.FILE_NAME).toAbsolutePath().normalize(),
                PoseModelLocator.candidates().getFirst(),
                "the launch directory keeps working exactly as it did");
    }

    @Test void anExplicitPropertyWinsOverEverything(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        var elsewhere = temporary.resolve("weights.onnx");
        Files.writeString(elsewhere, "not a real model, but a real file");
        System.setProperty(PoseModelLocator.PROPERTY, elsewhere.toString());
        try {
            assertEquals(elsewhere.toAbsolutePath().normalize(), PoseModelLocator.candidates().getFirst(),
                    "an install that keeps weights elsewhere must be able to say so");
            assertEquals(elsewhere.toAbsolutePath().normalize(), PoseModelLocator.locate().orElseThrow(),
                    "and that path should be the one chosen");
        } finally {
            System.clearProperty(PoseModelLocator.PROPERTY);
        }
    }

    @Test void aMissingModelReportsWhereItLookedRatherThanFailing() {
        System.setProperty(PoseModelLocator.PROPERTY, "C:\\definitely\\not\\here\\nothing.onnx");
        try {
            // locate() may still succeed from the working directory on a machine
            // that has fetched the weights; what must hold is that the bogus
            // override is reported as tried rather than silently dropped
            assertTrue(PoseModelLocator.candidates().stream().anyMatch(path -> path.toString().contains("nothing.onnx")),
                    "a path that was tried must appear in the list a failure message is built from");
        } finally {
            System.clearProperty(PoseModelLocator.PROPERTY);
        }
    }
}
