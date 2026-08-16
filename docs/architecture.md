# Architecture

## Components

Saha uses ports between capture, landmark inference, analysis, coaching, routines, and storage. `LandmarkSource` is the only input the analysis pipeline needs. `DemoLandmarkSource` currently supplies deterministic normalized coordinates. `OpenCvCameraCapture` provides an opt-in, non-recording calibration preview on a background virtual thread; the JavaFX side coalesces frames so capture cannot flood its event queue. A future live landmark adapter will add resize/input normalization, ONNX Runtime inference, and MoveNet output decoding without leaking frames beyond that boundary.

`PoseAnalyzer` first finds the minimum confidence of every required landmark. Below 0.35 it returns the sealed `Unreliable` result, which contains framing guidance but cannot contain corrective suggestions. Reliable frames evaluate 2D joint angles against pose-specific ranges. Rules are prioritized and capped at two. Timing consumes only reliable frames.

`RoutineGenerator` creates a 1,150-second beginner sequence. `PersonalizationEngine` consumes derived `SessionMetric` records and produces duration deltas plus user-facing reasons. `JsonSessionStore` is the only persistent component.

## Data flow

```mermaid
flowchart LR
  C[Camera or fixture] --> L[Anonymous landmarks]
  L --> G{Confidence ≥ .70?}
  G -- no --> F[Framing guidance + pause]
  G -- yes --> A[Angles and flexible ranges]
  A --> K[At most two cues]
  K --> M[Derived session metrics]
  M --> P[Local JSON + personalization]
```

## Java platform choices

Gradle targets Java 26. Records express immutable domain values; sealed results and exhaustive switch patterns make reliability branching explicit; collection factories minimize mutable shared state. JavaFX provides an accessible native desktop surface and scheduled UI timing. Future blocking capture/inference work belongs on virtual threads, with results marshalled back to the JavaFX thread.

## Dependencies and licenses

| Component | Pinned version | License | Purpose/status |
|---|---:|---|---|
| Gradle | 9.4.0 | Apache-2.0 | Java 26-capable wrapper |
| OpenJFX | 26 | GPLv2 + Classpath Exception | UI |
| ONNX Runtime | 1.22.0 | MIT | Phase 2 local inference runtime |
| OpenPnP OpenCV | 4.9.0-0 | BSD-3-Clause | Local calibration preview; inference not connected |
| Jackson | 2.19.2 | Apache-2.0 | Derived-metric JSON |
| JUnit | 5.13.4 | EPL-2.0 | Tests |

## Compatibility decision

The application builds and tests with Temurin 26.0.1 through the pinned Gradle 9.4.0 wrapper. Native OpenCV camera behavior and an exact model artifact have not yet passed their separate acceptance gates, so live pose inference remains isolated and disabled rather than represented as working. Demo mode keeps all non-capture behavior functional.

## Logging

Application logs must contain lifecycle/error identifiers and aggregate timing only. Never log frames, images, coordinates, profile free text, or detailed body measurements.
