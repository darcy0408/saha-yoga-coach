# Saha — a private, supportive yoga coach

Saha is a Java 26 desktop application that guides an approximately 20-minute yoga practice and turns local body landmarks into cautious, understandable alignment cues. It is educational fitness software, not medical care.

## Competition relevance

Built for Hackster.io's **Modern Java in the Wild**, Saha targets Best Health Solution and Best in Show. Java owns the UI, routine engine, landmark geometry, coaching policy, personalization, and local persistence. The memorable judge path works without special hardware through deterministic demonstration landmarks.

## The problem

Home yoga videos cannot notice when someone leaves the frame, needs a gentler option, or has repeatedly struggled with a pose. Cloud vision products also create privacy concerns. Saha responds only when landmark evidence is sufficiently reliable, stores derived metrics rather than images, and explains every routine adjustment.

## Implemented features

- Polished JavaFX onboarding, calibration, coaching, and progress screens.
- A beginner practice of 20 poses in seven phases — centering, warm-up, standing, balance, floor work, cooldown, rest — about 20 minutes end to end.
- A minimal line figure for every pose, stroked live from MIT-licensed vector geometry in the interface's own ink.
- Spoken guidance through the operating system's own voice, so poses and cues can be followed without reading the screen. Local, toggleable, and rate-limited so it guides rather than chatters.
- Live landmark estimation from your camera with MoveNet, drawn over the video, feeding the same confidence gate and alignment rules.
- A 1–5 intensity control that transparently adjusts active hold times without introducing advanced poses.
- Structured instructions, modifications, landmark requirements, angle ranges, and general cautions.
- Opt-in, local OpenCV camera preview during calibration and practice; frames are transient and never analyzed or stored in this build.
- Near-real-time synthetic landmark demonstration through the production analysis boundary.
- Joint-angle calculations and a 0.70 minimum confidence threshold.
- Bilateral rule strategies support either lead side instead of assuming the left leg always leads.
- Poses without implemented measurements explicitly say "instruction only" rather than implying alignment was checked.
- At most two supportive, observable cues at once; timer pauses when confidence is low.
- Pause, repeat, skip, easier-option, and always-visible immediate-stop controls.
- Derived-only JSON session history, explainable rule-based personalization, and full deletion.
- Automated geometry, confidence, routine, personalization, and persistence tests.

Live OpenCV preview is available from camera setup. ONNX inference is intentionally disabled until the documented MoveNet model artifact is verified, so the application clearly labels coaching as synthetic demo analysis. The UI falls back to demo mode instead of crashing or going blank when camera access fails.

## Java 26

The build is pinned to a Java 26 Gradle toolchain and verified on Temurin 26.0.1. The immutable pose catalog uses Java 26's preview `LazyConstant` API so it is constructed once, on first use. The code also uses records for immutable measurements and events, plus sealed analysis outcomes with exhaustive pattern matching. The sealed result hierarchy distinguishes measured feedback, instruction-only guidance, and unreliable camera input so an unmeasured pose cannot accidentally receive a positive alignment claim. Gradle 9.4+ is required because it added Java 26 support; Gradle supplies `--enable-preview` for compile, test, and run tasks.

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

The project has been verified with Temurin JDK 26.0.1. Dependencies are downloaded from Maven Central on first build. Gradle's build cache is enabled; its configuration cache is disabled because the OpenJFX `run` task is not compatible with it.

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

- Camera coaching has not yet been validated against a person holding each pose, so its cues should be read as plausible rather than proven.
- Without the model present the app falls back to synthetic demo landmarks, which prove workflow and analysis behaviour, not model accuracy.
- Spoken guidance uses the Windows speech engine; other platforms fall back to silence rather than a second-rate voice.
- Alignment rules cover a reliable subset; some poses provide instruction and confidence checks without geometric correction.
- Single-view 2D angles cannot resolve depth or guarantee alignment or safety.
- Text-to-speech, multiple routine modes, and accessibility audit remain Phase 2.
- Live camera behavior still needs a device-specific Windows smoke test even though the Java 26 build is verified.

## Three-minute demonstration

Run the app, explain local-only processing on onboarding, show calibration/demo fallback, start the routine, wait for a confidence pause, request an easier option, skip through several poses, stop, and show/delete derived history. A narrated script is in [docs/demo-script.md](docs/demo-script.md).

## Future improvements

Validate and ship a licensed MoveNet ONNX artifact first. Then add live OpenCV capture, adaptive inference frequency, local speech, keyboard/screen-reader testing, richer stability trends, and additional routine modes. Multi-camera, wearable, and voice features remain optional until the core coach is validated with diverse users.

## License

Saha source is MIT licensed. Third-party dependencies retain their own licenses; see [architecture](docs/architecture.md).
