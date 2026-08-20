# Saha — a private, supportive yoga coach

Saha is a Java 26 desktop application that guides an approximately 20-minute yoga practice and turns local body landmarks into cautious, understandable alignment cues. It is educational fitness software, not medical care.

## Competition relevance

Built for Hackster.io's **Modern Java in the Wild**, Saha targets Best Home Solution and Best in Show. Java owns the UI, routine engine, landmark geometry, coaching policy, personalization, and local persistence. The memorable judge path works without special hardware through deterministic demonstration landmarks.

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
- Opt-in, local OpenCV camera capture during calibration and practice; frames are analysed on this device, then discarded, never written to disk or uploaded.
- Synthetic landmark demonstration through the same analysis boundary when no model is installed.
- Joint-angle calculations and a 0.35 minimum confidence threshold, the value validated against a real body.
- Bilateral rule strategies support either lead side instead of assuming the left leg always leads.
- Poses without implemented measurements explicitly say "instruction only" rather than implying alignment was checked.
- At most two supportive, observable cues at once; timer pauses when confidence is low.
- Pause, repeat, skip, easier-option, and always-visible immediate-stop controls.
- Derived-only JSON session history, explainable rule-based personalization, and full deletion.
- Automated geometry, confidence, routine, personalization, and persistence tests.

With the verified model in place, the camera drives coaching: landmarks are estimated on this device, drawn over the mirrored video, and passed through the same confidence gate and alignment rules. Without it the app falls back to synthetic demo landmarks and says so on screen. It also falls back rather than crashing or going blank when camera access fails.

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

Run `.\scripts\fetch-model.ps1` once. It downloads MoveNet SinglePose Lightning (~9 MB, no account) and refuses any file whose SHA-256 does not match the validated artifact. Weights are not committed to git. See [models/README.md](models/README.md) and the [model card](docs/model-card.md).

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

- Camera coaching has been walked through pose by pose against one person, on one Windows machine, in one room, on the last day of the entry period. That pass found and fixed real faults — a lunge shallower than its own rule, a leg hidden behind another leg overruling the leg in plain view, arms measured at a joint that leaves the frame. It is not a validation study: one body, one camera height, one set of proportions and one light source.
- Standing poses are the best supported. Poses done on the floor are the least: the pose model is trained on upright people, so a body lying or sitting on the mat scores low enough that coaching often declines to speak rather than guess. Bridge, Cat–Cow and Head to Knee are the ones to treat as unproven.
- Wrists raised above the head score too low to draw or measure, so overhead reach is measured at the elbow. An audit traced much of this to the pipeline rather than the model: the whole camera frame is letterboxed into the model's 192-pixel input with no crop to the person, so a standing body spans about 144 pixels and a raised wrist only a few. Cropping to the person, capturing above 640×480, and the higher-resolution Thunder variant are the untried fixes, in that order; until one is tried this is a limitation of this pipeline, not an established fact about the model.
- Without the model present the app falls back to synthetic demo landmarks, which prove workflow and analysis behaviour, not model accuracy.
- Spoken guidance uses the Windows speech engine; other platforms fall back to silence rather than a second-rate voice.
- Alignment rules cover a reliable subset; some poses provide instruction and confidence checks without geometric correction.
- Single-view 2D angles cannot resolve depth or guarantee alignment or safety.
- Spoken guidance uses whichever voice Windows has installed; the stock desktop voices sound dated, and adding a natural voice in Settings improves it considerably.
- Multiple routine modes and a full accessibility audit remain future work.
- What this most needs next is other people: a range of bodies, rooms and cameras, and someone who teaches yoga checking the ranges against what they would actually say in a class.

## Three-minute demonstration

Run the app, explain local-only processing on onboarding, show calibration/demo fallback, start the routine, wait for a confidence pause, request an easier option, skip through several poses, stop, and show/delete derived history. A narrated script is in [docs/demo-script.md](docs/demo-script.md).

## Future improvements

Validate and ship a licensed MoveNet ONNX artifact first. Then add live OpenCV capture, adaptive inference frequency, local speech, keyboard/screen-reader testing, richer stability trends, and additional routine modes. Multi-camera, wearable, and voice features remain optional until the core coach is validated with diverse users.

## License

Saha source is MIT licensed. Third-party dependencies retain their own licenses; see [architecture](docs/architecture.md).
