# Saha development plan

## Product boundary

Saha is educational fitness software, not medical care. It observes anonymous body landmarks locally, never identity or appearance, and offers cautious, confidence-aware yoga cues. The contest-ready path must work without a camera by replaying synthetic landmark fixtures.

## Environment inventory (updated 1 August 2026)

- Workspace: Windows repository at `C:\dev\yoga`, connected to GitHub.
- Installed JVM: Eclipse Temurin 26.0.1.
- Build tooling: Gradle 9.4.0 wrapper checked into the repository.
- Hardware probing through WMI was denied, so no CPU/GPU assumptions are made.
- Consequence: Java 26 builds and tests can be verified locally; camera and native inference still require explicit device/model validation.

## Architecture

| Layer | Responsibility | Boundary |
|---|---|---|
| JavaFX UI | onboarding, calibration, coaching, progress, stop control | Calls application services only |
| Routine | pose catalog, sequencing, timing | Pure Java, deterministic |
| Analysis | landmarks, joint angles, confidence, alignment rules | No images or persistence |
| Coaching | ranks at most two supportive corrections | Consumes analysis signals |
| Vision | camera frames and ONNX inference | Replaceable ports; demo fixture fallback |
| Personalization | transparent adjustments and explanations | Pure rules over session summaries |
| Storage | local JSON session metrics | Never stores frames or landmark histories |

The UI uses JavaFX's application thread for rendering. Camera and model work must run off that thread when implemented. Records model immutable domain events and sealed interfaces constrain analysis results. Pattern matching keeps result handling exhaustive and readable. Java 26 preview `LazyConstant` initializes the immutable pose catalog on first use. The Gradle Java 26 toolchain makes the requested platform explicit.

## Dependency decisions

- Gradle 9.4.0: first documented release with Java 26 runtime/toolchain support; wrapper is pinned.
- OpenJFX 26: desktop UI, platform-aligned with JDK 26. GPLv2 with Classpath Exception.
- ONNX Runtime Java 1.22.0 CPU: local inference abstraction; MIT license. The model adapter is disabled until a verified model file is installed.
- OpenPnP OpenCV 4.9.0-0: packaged Java/native camera binding; BSD-3-Clause upstream. Camera failure always falls back to demo mode.
- Jackson 2.19.2: local structured pose/session data; Apache-2.0.
- JUnit 5.13.4: tests; EPL-2.0.

All versions are pinned. Before submission, run a dependency audit and revalidate Java 26 compatibility; native libraries are the highest-risk area.

## Delivery phases

Pose illustration accuracy and transition work follows the dedicated [pose accuracy implementation plan](pose-accuracy-implementation-plan.md). Its review gate overrides pose-count goals: an unreviewed visual remains written-only and cannot appear as instructional artwork.

### Phase 1 — contest-ready minimum

1. Project/build and documentation scaffold.
2. Twelve-pose catalog and one approximately 20-minute beginner routine.
3. Landmark geometry, flexible alignment ranges, confidence gating, and synthetic demo stream.
4. JavaFX onboarding, calibration, session coaching, controls, and persistent stop action.
5. Local metrics store, progress summary, explainable personalization.
6. Automated unit/integration tests and failure-path checks.

### Phase 2 — expanded coaching

MoveNet ONNX preprocessing/postprocessing connected to the implemented local OpenCV preview, several routine modes, transition stability, local speech, accessibility review, and richer charts.

### Phase 3 — optional enhancements

Only after stability: second-camera fusion, voice control, wearable input, progress export, and landmark-only aerial visualization.

## Risks and mitigations

- **Java 26 preview API:** `LazyConstant` requires `--enable-preview`; Gradle applies it consistently to compilation, tests, and execution.
- **Pose model licensing/shape mismatch:** do not bundle an unverified model; checksum and document an approved MoveNet ONNX export before enabling inference.
- **Native camera variance:** isolate OpenCV, enumerate devices, catch load/open failures, preserve demo and instruction-only modes.
- **Unsafe or noisy coaching:** require aggregate landmark confidence of at least 0.70, use ranges, suppress feedback on occlusion, and show at most two cues.
- **Privacy leakage:** no frame recording, no coordinate history, redacted logs, derived metrics only, user-visible deletion.
- **Schedule:** prioritize a deterministic judge demo over optional integrations.

## Acceptance tests

- Clean `gradlew test` and `gradlew run` on JDK 26.
- Onboarding records consent and limitations without collecting identity.
- Calibration explains framing, light, space, and recovery from low confidence.
- Beginner plan includes warm-up, main work, transitions, cooldown, and totals 18–22 minutes.
- Catalog has at least 12 poses, each with instruction, modification, landmark requirements, and caution.
- Fixture landmarks produce expected angles; confidence below 0.70 yields guidance, never correction.
- Coach returns no more than two supportive suggestions.
- Pause/low confidence stops elapsed hold time; skip/easier/stop controls work.
- Only derived session summaries persist, can be viewed, and can be deleted.
- Three comfortable completions increase duration slightly; repeated skips/struggles reduce or replace it; discomfort blocks automatic recommendation.
- Demo completes onboarding-to-progress in under five minutes without camera.

## Feature self-review gate

Before calling a feature complete: run it, confirm evidence supports its wording, expose uncertainty, inspect stored/logged data, test it during a timed session, confirm Java owns the feature, and rehearse the judge path.
