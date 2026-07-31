package io.saha.yoga.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public record LandmarkFrame(Instant capturedAt, Map<LandmarkName, Landmark> landmarks) {
    public LandmarkFrame {
        landmarks = Map.copyOf(new EnumMap<>(landmarks));
    }
    public double minimumConfidence(Iterable<LandmarkName> required) {
        double min = 1;
        for (var name : required) {
            var point = landmarks.get(name);
            if (point == null) return 0;
            min = Math.min(min, point.confidence());
        }
        return min;
    }
}

