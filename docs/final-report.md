# Implementation report

```text
Build status: PASS on Temurin JDK 26.0.1; Gradle configuration cache is disabled because the OpenJFX run task is incompatible with it.
Test status: PASS — 10 tests covering geometry, reliability, cue limit, routine length/adjustments, personalization, and persistence.
Implemented features: JavaFX onboarding, calibration, synthetic-landmark coaching demo, 19:10 beginner routine, 12 poses/modifications, confidence-aware angle feedback, pause/repeat/skip/easier/stop, derived local history, explainable personalization, progress, deletion, documentation.
Java 26 features used: Java 26 Gradle toolchain target; records; sealed results and exhaustive pattern matching; modern collection APIs. JavaFX 26 is the default UI runtime.
Pose model: No model bundled. MoveNet SinglePose Lightning ONNX is planned but disabled pending provenance, license, checksum, tensor, and accuracy verification.
Supported poses: Mountain, Chair, Warrior I, Warrior II, Triangle, Tree, Cat–Cow, Bird Dog, Low Lunge, Bridge, Seated Forward Fold, Final Rest.
Privacy behavior: Local processing; no frame/image/coordinate persistence; derived metrics only at ~/.saha/sessions.json; no upload; in-app deletion.
Known limitations: No verified live camera/ONNX adapter; synthetic demo is not accuracy evidence; some poses are instruction-only; no TTS; Java 26 execution remains unverified locally.
Demo instructions: Run gradlew.bat run on JDK 26, accept onboarding, review calibration, start Steady Start, observe a low-confidence pause, request an easier option, skip poses, stop, inspect/delete progress.
Recommended next improvement: Validate and integrate a licensed MoveNet ONNX artifact with OpenCV capture, then test cue suppression and latency across diverse users and hardware.
```
