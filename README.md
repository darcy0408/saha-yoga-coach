# Saha — a private, supportive yoga coach

Saha is a Java 26 desktop application that guides an approximately 20-minute yoga practice and turns local body landmarks into cautious, understandable alignment cues. It is educational fitness software, not medical care.

## Competition relevance

Built for Hackster.io's **Modern Java in the Wild**, Saha targets Best Health Solution and Best in Show. Java owns the UI, routine engine, landmark geometry, coaching policy, personalization, and local persistence. The memorable judge path works without special hardware through deterministic demonstration landmarks.

## The problem

Home yoga videos cannot notice when someone leaves the frame, needs a gentler option, or has repeatedly struggled with a pose. Cloud vision products also create privacy concerns. Saha responds only when landmark evidence is sufficiently reliable, stores derived metrics rather than images, and explains every routine adjustment.

## Implemented features

- Polished JavaFX onboarding, calibration, coaching, and progress screens.
- A 19:10 beginner routine with warm-up, main sequence, cooldown, and 12 poses.
- Structured instructions, modifications, landmark requirements, angle ranges, and general cautions.
- Near-real-time synthetic landmark demonstration through the production analysis boundary.
- Joint-angle calculations and a 0.70 minimum confidence threshold.
- At most two supportive, observable cues at once; timer pauses when confidence is low.
- Pause, repeat, skip, easier-option, and always-visible immediate-stop controls.
- Derived-only JSON session history, explainable rule-based personalization, and full deletion.
- Automated geometry, confidence, routine, personalization, and persistence tests.

Live OpenCV capture and ONNX inference are intentionally disabled until the documented MoveNet model artifact is verified. The UI never crashes or goes blank because demo mode remains available.

## Java 26

The build is pinned to a Java 26 Gradle toolchain. The code uses records for immutable measurements and events, sealed analysis outcomes with exhaustive pattern matching, modern collection factories, and virtual-thread-ready service boundaries. These features keep safety-relevant state explicit and make unreliable results impossible to treat accidentally as corrections. Gradle 9.4+ is required because it added Java 26 support.

## Architecture

```text
JavaFX → routine/session controller → coaching + personalization
                              ↓
camera/demo → landmark source → confidence gate → geometry/rules
                              ↓
                   derived metrics → local JSON
```

See [architecture](docs/architecture.md) and the [development plan](docs/development-plan.md).

## Requirements and installation

1. Install a 64-bit JDK 26 and set `JAVA_HOME`.
2. On Windows, run `gradlew.bat test`; on macOS/Linux, run `./gradlew test`.
3. No camera, account, credential, or model is required for demo mode.

The initial development machine had JDK 21 only, so Java 26 compilation must be repeated on the submission machine or CI. Dependencies are downloaded from Maven Central on first build.

For a provisional check on that host only, use `gradlew.bat test -PjavaVersion=21`. This does not constitute the required Java 26 acceptance test; Java 26 remains the default.

## Model setup

Read [models/README.md](models/README.md). The current contest-safe build does not claim live inference: it replays synthetic landmark fixtures. Once a specific MoveNet ONNX artifact has passed checksum, license, tensor-shape, and pose-fixture validation, place it at `models/movenet-singlepose-lightning.onnx` and enable the adapter in a Phase 2 build.

## Run

```powershell
.\gradlew.bat run
```

Select the consent checkbox, continue through calibration, and start **Steady Start**. Use **Skip** to move rapidly through poses during a demonstration. The timer pauses for deliberately injected low-confidence frames.

## Test

```powershell
.\gradlew.bat clean test
```

Tests do not need a camera or person. Fixtures contain synthetic normalized landmarks only.

## Privacy and safety

Normal operation saves no video, images, face data, landmark coordinates, identity, emotion, age, body shape, or appearance scores. Derived pose metrics are written to `~/.saha/sessions.json`; the progress page can delete them. Saha does not upload data. Stop for pain, dizziness, numbness, weakness, or unusual discomfort. See [privacy and safety](docs/privacy-and-safety.md).

## Known limitations

- Live camera capture and ONNX inference are behind a disabled integration boundary pending model validation.
- Demo landmarks are synthetic and prove workflow/analysis behavior, not model accuracy.
- Alignment rules cover a reliable subset; some poses provide instruction and confidence checks without geometric correction.
- Single-view 2D angles cannot resolve depth or guarantee alignment or safety.
- Text-to-speech, multiple routine modes, and accessibility audit remain Phase 2.
- Local JDK 26/build verification was unavailable at repository creation.

## Three-minute demonstration

Run the app, explain local-only processing on onboarding, show calibration/demo fallback, start the routine, wait for a confidence pause, request an easier option, skip through several poses, stop, and show/delete derived history. A narrated script is in [docs/demo-script.md](docs/demo-script.md).

## Future improvements

Validate and ship a licensed MoveNet ONNX artifact first. Then add live OpenCV capture, adaptive inference frequency, local speech, keyboard/screen-reader testing, richer stability trends, and additional routine modes. Multi-camera, wearable, and voice features remain optional until the core coach is validated with diverse users.

## License

Saha source is MIT licensed. Third-party dependencies retain their own licenses; see [architecture](docs/architecture.md).
