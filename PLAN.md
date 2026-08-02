# Saha delivery plan

Last updated: 2026-08-01

## Completed

- Java 26 / Gradle 9.4 build verified on Temurin 26.0.1.
- Twelve-pose beginner routine, JavaFX workflow, derived-only persistence, and demo mode.
- Truthful `Reliable`, `InstructionOnly`, and `Unreliable` analysis outcomes.
- Reference-pose regression coverage and bilateral knee-rule evaluation.
- Java 26 preview `LazyConstant` used for the pose catalog.
- Licensed teaching-asset catalog and review gate; coaching use remains disabled.
- Feature branch published to GitHub.

## In progress: live camera foundation

Build an opt-in `--camera` path that:

1. Loads OpenCV and opens a selected local camera without recording frames.
2. Reports native-library, permission, and device failures in plain language.
3. Keeps the JavaFX thread responsive and closes the camera deterministically.
4. Falls back to demo mode without a blank screen or crash.
5. Does not call camera preview "pose coaching" until a verified ONNX model produces tested landmarks.

Verification: `./gradlew.bat clean test` on Temurin 26.0.1, plus a manual camera smoke test on the Windows host.

## Next

- Select and document an authoritative, redistributable ONNX pose model.
- Implement and fixture-test preprocessing, tensor decoding, and confidence mapping.
- Add pose-specific measurements beyond knee angle, beginning with Warrior II arm line and torso stacking.
- Add reviewed teaching visuals for the remaining poses.
- Refresh the contest demo and final report after live-camera verification.

## Blockers

- No pose model may be enabled until its provenance, license, checksum, input tensor, output ordering, and fixture behavior are verified.
- Camera hardware behavior requires a manual smoke test; automated tests use fakes and must not require a device.
