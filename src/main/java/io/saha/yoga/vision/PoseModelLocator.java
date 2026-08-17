package io.saha.yoga.vision;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Finds the pose model on disk, and says where it looked when it cannot.
 *
 * <p>The path used to be {@code models/movenet-singlepose-lightning.onnx}
 * resolved against the process working directory. That is the project root
 * under {@code gradlew run} and something else under every other launch, so an
 * installed copy of the app found no model and dropped to demo mode - silently,
 * and indistinguishably from a clone that had simply never run the fetch
 * script. Searching beside the code as well as beside the working directory
 * makes both launches work, and returning the list of places tried makes the
 * failure explainable instead of mysterious.
 */
public final class PoseModelLocator {
    public static final String FILE_NAME = "movenet-singlepose-lightning.onnx";
    /** Full path to a model file, for an install that keeps weights elsewhere. */
    public static final String PROPERTY = "saha.model";
    private static final String DIRECTORY = "models";
    /** How far up from the code to look; enough to climb out of build/classes/java/main. */
    private static final int ANCESTORS = 4;

    private PoseModelLocator() {}

    /** The first candidate that is actually a readable file. */
    public static Optional<Path> locate() {
        return candidates().stream().filter(Files::isRegularFile).findFirst();
    }

    /**
     * Every path that {@link #locate()} will try, in order.
     *
     * Public so a failure message can name them: "no model at any of these"
     * is actionable in a way that "demo mode" is not.
     */
    public static List<Path> candidates() {
        var found = new LinkedHashSet<Path>();
        var override = System.getProperty(PROPERTY);
        if (override != null && !override.isBlank()) add(found, Path.of(override));
        add(found, Path.of(System.getProperty("user.dir", "."), DIRECTORY, FILE_NAME));
        codeDirectory().ifPresent(directory -> {
            var walk = directory;
            for (int step = 0; step <= ANCESTORS && walk != null; step++) {
                add(found, walk.resolve(DIRECTORY).resolve(FILE_NAME));
                walk = walk.getParent();
            }
        });
        return List.copyOf(new ArrayList<>(found));
    }

    private static void add(LinkedHashSet<Path> into, Path candidate) {
        try {
            into.add(candidate.toAbsolutePath().normalize());
        } catch (RuntimeException ignored) {
            // an unrepresentable path is simply not a candidate
        }
    }

    /** The directory holding this class, whether it ships as classes or a jar. */
    private static Optional<Path> codeDirectory() {
        try {
            var source = PoseModelLocator.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) return Optional.empty();
            var path = Path.of(source.getLocation().toURI());
            return Optional.ofNullable(Files.isDirectory(path) ? path : path.getParent());
        } catch (Exception ignored) {
            // a classloader with no file-backed code source: the working
            // directory candidate above still applies
            return Optional.empty();
        }
    }
}
