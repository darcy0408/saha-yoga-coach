package io.saha.yoga.analysis;

import java.util.List;

public sealed interface AnalysisResult permits AnalysisResult.Reliable, AnalysisResult.Unreliable {
    record Reliable(String status, List<String> suggestions, double confidence) implements AnalysisResult {
        public Reliable { suggestions = List.copyOf(suggestions).stream().limit(2).toList(); }
    }
    record Unreliable(String guidance, double confidence) implements AnalysisResult {}
}

