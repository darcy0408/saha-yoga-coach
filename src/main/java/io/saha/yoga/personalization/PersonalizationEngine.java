package io.saha.yoga.personalization;

import io.saha.yoga.domain.SessionMetric;
import java.util.*;
import java.util.stream.Collectors;

public final class PersonalizationEngine {
    public Recommendation recommend(List<SessionMetric> history) {
        var adjustments = new HashMap<String, Integer>();
        var reasons = new ArrayList<String>();
        var grouped = history.stream().collect(Collectors.groupingBy(SessionMetric::poseId));
        grouped.forEach((pose, metrics) -> {
            var recent = metrics.stream().sorted(Comparator.comparing(SessionMetric::completedAt).reversed()).limit(3).toList();
            if (recent.stream().anyMatch(m -> !m.comfortable())) {
                adjustments.put(pose, -20);
                reasons.add("Reduced " + readable(pose) + " because discomfort was reported.");
            } else if (recent.size() >= 2 && recent.stream().filter(SessionMetric::skipped).count() >= 2) {
                adjustments.put(pose, -15);
                reasons.add("Shortened " + readable(pose) + " after it was skipped more than once.");
            } else if (recent.size() == 3 && recent.stream().allMatch(m -> m.comfortable() && !m.skipped() && m.averageConfidence() >= .8 && m.stability() >= .75)) {
                adjustments.put(pose, 10);
                reasons.add("Added 10 seconds to " + readable(pose) + " after three comfortable, steady sessions.");
            }
        });
        if (reasons.isEmpty()) reasons.add("Kept a gentle baseline while Saha learns from completed sessions.");
        return new Recommendation(adjustments, reasons);
    }
    private String readable(String id) { return id.replace('_', ' '); }
    public record Recommendation(Map<String, Integer> durationAdjustments, List<String> explanations) {
        public Recommendation { durationAdjustments = Map.copyOf(durationAdjustments); explanations = List.copyOf(explanations); }
    }
}

