# Saha delivery plan

Last updated: 2026-08-04

## Completed

- Java 26 / Gradle 9.4 build verified on Temurin 26.0.1.
- Twelve-pose beginner routine, JavaFX workflow, derived-only persistence, and demo mode.
- Truthful `Reliable`, `InstructionOnly`, and `Unreliable` analysis outcomes.
- Reference-pose regression coverage and bilateral knee-rule evaluation.
- Java 26 preview `LazyConstant` used for the pose catalog.
- Licensed teaching-asset catalog and review gate; coaching use remains disabled.
- Feature branch published to GitHub.
- Opt-in OpenCV preview verified manually on Windows; frames remain local and transient.
- Live preview remains visible during practice and explicitly disables alignment claims until inference is connected.
- Five-level intensity changes hold duration only and never introduces advanced poses.
- Rejected diagnostic pose drafts are hidden from the normal user flow.
- Project-specific `open-session` and `close-session` skills added and structurally validated.

## In progress: verified landmark inference

Connect the working local preview to a verified pose model that:

1. Has an authoritative source, redistribution license, pinned checksum, and documented tensor contract.
2. Produces fixture-tested landmarks and confidence values through `LandmarkSource`.
3. Keeps capture and inference off the JavaFX thread and limits inference frequency.
4. Suppresses feedback for incomplete framing, occlusion, or confidence below 0.70.
5. Preserves demo mode and never records frames or coordinate histories.

Latest verification: `./gradlew.bat clean test` passed on Temurin 26.0.1 on 2026-08-04; 37 tests. `git diff --check` passed. Manual camera preview succeeded; live landmark inference is not implemented.

## Next

- Select and document an authoritative, redistributable ONNX pose model.
- Implement and fixture-test preprocessing, tensor decoding, and confidence mapping.
- Add pose-specific measurements beyond knee angle, beginning with Warrior II arm line and torso stacking.
- Add reviewed teaching visuals for the remaining poses.
- Refresh the contest demo and final report after live-inference verification.

## Blockers

- No pose model may be enabled until its provenance, license, checksum, input tensor, output ordering, and fixture behavior are verified.
- A consistent, aesthetically suitable, accurately reviewed 12-pose public-domain teaching set has not been found. Only Warrior II and Tree candidates are cataloged, and coaching use remains disabled.
