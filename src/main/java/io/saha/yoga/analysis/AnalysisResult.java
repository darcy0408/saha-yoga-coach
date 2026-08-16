package io.saha.yoga.analysis;

import java.util.List;

public sealed interface AnalysisResult permits AnalysisResult.Reliable, AnalysisResult.InstructionOnly, AnalysisResult.Unreliable {
    /**
     * An angle actually measured from the body this frame.
     *
     * Carried so the interface can show the number moving as you move, which
     * answers "is it seeing me?" far better than any status word. A measurement
     * that was not graded reports no verdict rather than a flattering one.
     */
    record Measurement(String label, double degrees, double minimum, double maximum, boolean graded) {
        public boolean inRange() { return graded && degrees >= minimum && degrees <= maximum; }
    }

    record Reliable(String status, List<String> suggestions, double confidence, List<Measurement> measurements) implements AnalysisResult {
        public Reliable {
            suggestions = List.copyOf(suggestions).stream().limit(2).toList();
            measurements = List.copyOf(measurements);
        }
    }
    record InstructionOnly(String guidance, double confidence) implements AnalysisResult {}
    record Unreliable(String guidance, double confidence) implements AnalysisResult {}
}
