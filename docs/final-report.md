# Implementation report

```text
Build status: PASS on Temurin JDK 26.0.1; Gradle configuration cache is disabled because the OpenJFX run task is incompatible with it.
Test status: PASS — 33 tests covering geometry, confidence thresholds, cue limits, instruction-only truthfulness, all authored reference poses, deliberate deviations, routine generation/navigation, personalization, persistence, illustration approval, asset checksums, and Java 26 lazy initialization.
Implemented features: JavaFX onboarding, calibration, synthetic-landmark coaching demo, 19:10 beginner routine, 12 poses/modifications, confidence-aware angle feedback for six poses, explicit instruction-only guidance for six poses, pause/repeat/skip/easier/stop, derived local history, explainable personalization, progress, deletion, licensed-asset review, and documentation.
Java 26 features used: Java 26 Gradle toolchain and JavaFX 26 verified on Temurin 26.0.1; Java 26 preview Lazy Constants for initialize-once pose data; records; and a sealed three-state analysis result with exhaustive pattern matching.
Pose model: No model bundled. MoveNet SinglePose Lightning ONNX is planned but disabled pending provenance, license, checksum, tensor, and accuracy verification.
Supported poses: Mountain, Chair, Warrior I, Warrior II, Triangle, Tree, Cat–Cow, Bird Dog, Low Lunge, Bridge, Seated Forward Fold, Final Rest.
Privacy behavior: Local processing; no frame/image/coordinate persistence; derived metrics only at ~/.saha/sessions.json; no upload; in-app deletion.
Known limitations: No verified live camera/ONNX adapter; synthetic demo is not accuracy evidence; six poses are explicitly instruction-only; the measured poses currently use a limited knee-angle rule set; no TTS.
Demo instructions: Run gradlew.bat run on JDK 26, accept onboarding, review calibration, start Steady Start, observe a low-confidence pause, request an easier option, skip poses, stop, inspect/delete progress.
Recommended next improvement: Validate and integrate a licensed MoveNet ONNX artifact with OpenCV capture, then test cue suppression and latency across diverse users and hardware.
```
