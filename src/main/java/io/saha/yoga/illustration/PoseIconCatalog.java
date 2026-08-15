package io.saha.yoga.illustration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Line-art pose icons from the Atlas Icons yoga pack.
 *
 * The pack is MIT licensed, which permits redistribution inside this
 * repository and modification, so the geometry travels with the source
 * instead of being fetched at runtime. Each icon is a set of SVG sub-paths
 * drawn as strokes inside a 24x24 box; {@link PoseIconView} renders them.
 *
 * Icons are mapped to catalog poses only where the drawn figure genuinely
 * depicts that asana. A pose with no honest match is absent, and the
 * teaching card falls back to written guidance rather than showing a
 * near-miss under the wrong name.
 */
public final class PoseIconCatalog {
    /** Attribution kept although MIT does not require it. */
    public static final String CREDIT = "Atlas Icons · Ramy Wafaa · MIT";
    private static final String RESOURCE = "/io/saha/yoga/illustrations/atlas/atlas-yoga.properties";

    // Mapped only where the drawn figure really is that asana. Deliberately
    // absent: mountain (the pack's standing figures all raise or extend the
    // arms), warrior_two (its wide-stance figure keeps both legs straight, so
    // it misses the bent front knee that defines the pose), and bird dog
    // (nothing in the pack extends an opposite arm and leg from all fours).
    // Warrior I and low lunge share one icon because both are that same high
    // lunge shape.
    private static final Map<String, String> ICON_BY_POSE = Map.ofEntries(
            Map.entry("easy_seat", "SeatedPose"),
            Map.entry("seated_side_reach", "SittingArmRaisePose"),
            Map.entry("cat_cow", "CatPose"),
            Map.entry("downward_dog", "DownwardFacingDog"),
            Map.entry("standing_fold", "StandingForwardBendPose"),
            Map.entry("upward_salute", "StraightAnglePose"),
            Map.entry("chair", "SquatPose"),
            Map.entry("warrior_one", "LungePose"),
            Map.entry("triangle", "TrianglePose"),
            Map.entry("goddess", "GoddessSquatPose"),
            Map.entry("tree", "TreePose"),
            Map.entry("low_lunge", "LungePose"),
            Map.entry("plank", "PlankPose"),
            Map.entry("locust", "LocustPose"),
            Map.entry("bridge", "BridgeYogaPose"),
            Map.entry("seated_fold", "SeatedForwardBendPose"),
            Map.entry("seated_twist", "TwistPose"),
            Map.entry("head_to_knee", "LegStretchSittingPose"),
            Map.entry("rest", "RestPose"));

    /** One icon: stroked sub-paths plus the head circles, all in the 24x24 box. */
    public record Icon(List<String> paths, List<Head> circles) { }

    /** A head, drawn as an outlined circle. */
    public record Head(double centreX, double centreY, double radius) { }

    private final Map<String, Icon> iconsByName;

    public PoseIconCatalog() {
        var properties = new Properties();
        try (InputStream stream = PoseIconCatalog.class.getResourceAsStream(RESOURCE)) {
            properties.load(Objects.requireNonNull(stream, RESOURCE));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + RESOURCE, e);
        }
        iconsByName = properties.stringPropertyNames().stream()
                .filter(name -> !name.endsWith(".circles"))
                .collect(Collectors.toUnmodifiableMap(name -> name, name -> new Icon(
                        List.of(properties.getProperty(name).split("\\|")),
                        heads(properties.getProperty(name + ".circles", "")))));
    }

    private static List<Head> heads(String value) {
        if (value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("\\|")).map(entry -> {
            var parts = entry.split(",");
            return new Head(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        }).toList();
    }

    /** The icon for a catalog pose, empty when no icon honestly depicts it. */
    public Optional<Icon> forPose(String poseId) {
        return Optional.ofNullable(ICON_BY_POSE.get(poseId)).map(iconsByName::get);
    }

    /** A named icon in the pack, used by the review gallery. */
    public Optional<Icon> forIcon(String iconName) {
        return Optional.ofNullable(iconsByName.get(iconName));
    }

    public List<String> iconNames() {
        return iconsByName.keySet().stream().sorted().toList();
    }
}
