# Hackster submission — Modern Java in the Wild

Copy-paste source for the Hackster project post. Each section below maps to a
field in Hackster's project editor. Deadline: **August 16, 2026, 11:59 PM PT**.
Category targets: **Best Health Solution** and **Best in Show**.

---

## Project name

Saha — a private, supportive yoga coach in Java 26

## Short description (elevator pitch)

A privacy-first desktop yoga coach: local body landmarks, cautious cues, and
explainable personalization. No cloud, no account, no stored video.

## Cover image

TODO (user): screenshot of the coaching screen mid-pose showing the cue panel
and confidence value, at the app's default window size. Alternative: the
calibration screen with the live preview and demo-mode badge.

---

## Story

### The problem: home practice is convenient but blind

Home yoga videos cannot notice when someone leaves the frame, needs a gentler
option, or has struggled with the same pose three sessions in a row. The
products that *can* see you usually do it by streaming video to a cloud
service — a hard trade for something as personal as exercising in your living
room. And many fitness apps overclaim: they score, praise, and "correct" from
evidence that cannot support those claims.

Saha (from the Sanskrit *saha*, "together with") takes the opposite bet: a
desktop coach that processes everything locally, speaks cautiously, and is
engineered so it *cannot* claim more than it measured.

### What Saha does

Saha guides an approximately 20-minute beginner routine of 12 poses with
warm-up, main sequence, and cooldown. During practice it:

- shows structured instructions, modifications, and general cautions per pose;
- converts anonymous body landmarks into 2D joint angles and checks them
  against pose-specific flexible ranges, on either lead side;
- offers at most **two** supportive, observable cues at a time;
- pauses the pose timer whenever landmark confidence drops below 0.70 instead
  of guessing;
- provides pause, repeat, skip, an easier option, and an always-visible
  immediate-stop control;
- records only derived metrics (completion, stability, duration) to a local
  JSON file, explains every routine adjustment it makes from them, and deletes
  everything on request.

A 1–5 intensity control transparently scales hold times without introducing
advanced poses. An opt-in OpenCV camera preview helps with framing during
calibration and practice; frames are transient, never analyzed, stored, or
uploaded in this build.

### Honesty as an architectural property

The analysis pipeline returns one of three sealed result types:

- `Reliable` — every required landmark cleared the 0.70 confidence gate, so
  measured feedback is allowed;
- `InstructionOnly` — the pose has no implemented geometric rules, so the UI
  says "instruction only" rather than implying alignment was checked;
- `Unreliable` — confidence is too low; the result *cannot carry corrective
  suggestions by construction*, only framing guidance, and the timer pauses.

Because the hierarchy is sealed and every consumer pattern-matches
exhaustively, "unmeasured pose accidentally praised as aligned" is a compile
error, not a bug class. The same honesty extends to the demo: this build's
coaching runs on deterministic synthetic landmarks through the production
analysis boundary, and the UI labels it as such. Live MoveNet inference stays
disabled until a model artifact passes provenance, license, checksum, tensor,
and fixture validation — that gate is documented in the repo rather than
quietly skipped.

### Modern Java 26, doing all the work

Java is not a wrapper here; it is the entire product:

- **Java 26 toolchain** — `build.gradle.kts` pins a Java 26 Gradle toolchain;
  the build is verified on Temurin 26.0.1 with Gradle 9.4.
- **Preview `LazyConstant`** (Java 26 preview API) — the immutable 12-pose
  catalog is constructed exactly once, on first use; `--enable-preview` is
  applied consistently across compile, test, and run.
- **Records** — landmarks, measurements, session metrics, and events are
  immutable record types.
- **Sealed interfaces + exhaustive pattern matching** — the three-state
  analysis result described above.
- **Virtual threads** — opt-in camera capture runs on a background virtual
  thread; the JavaFX side coalesces frames so capture can never flood the UI
  event queue.
- **JavaFX 26** — onboarding, calibration, coaching, and progress screens.

Supporting cast: OpenPnP OpenCV (local preview), ONNX Runtime (staged for
Phase 2 inference), Jackson (derived-metric JSON), JUnit 5 (37 tests covering
geometry, confidence gating, cue limits, instruction-only truthfulness,
bilateral rules, routine generation, personalization, and persistence).

### Privacy and safety by design

Normal operation saves no video, images, face data, landmark coordinates,
identity, emotion, age, body shape, or appearance scores. Derived pose metrics
live in `~/.saha/sessions.json`; the progress page deletes them. Nothing is
uploaded, and no account exists. Saha is educational fitness software, not
medical care: it avoids guarantees and body scoring, and tells users to stop
for pain, dizziness, numbness, weakness, or unusual discomfort.

### BYOD

Saha needs nothing beyond hardware people already own: any 64-bit PC and,
optionally, its built-in or USB webcam for the framing preview. The judge path
requires no camera at all.

### What's next

Validate and ship a licensed MoveNet ONNX artifact, connect the existing
preview to live landmarks across diverse users and environments, then add
local speech and a full accessibility audit. The gate is deliberate: the coach
earns live inference only when the model's provenance and behavior are proven.

---

## Brief written summary (required by rules)

Saha is a privacy-first yoga coach written entirely in Java 26. A JavaFX
desktop app guides a 20-minute, 12-pose beginner routine, converts local
anonymous body landmarks into at most two cautious alignment cues, pauses
whenever landmark confidence is low, and personalizes future sessions from
derived metrics it stores only in local JSON. Sealed result types make
overclaiming a compile error: unmeasured poses are labeled instruction-only,
and low-confidence frames cannot carry corrections. Java 26 preview
LazyConstant, records, exhaustive pattern matching, and virtual threads run
the catalog, domain model, analysis, and camera capture. No cloud, no
account, no stored video.

---

## Bill of materials

| Item | Notes |
| --- | --- |
| 64-bit Windows / macOS / Linux PC | Any machine that runs JDK 26 (BYOD) |
| Webcam (optional) | Built-in or USB, for the local framing preview only |
| Temurin JDK 26.0.1 (free) | Or any 64-bit JDK 26 |
| Saha source code (free, MIT) | github.com/darcy0408/saha-yoga-coach — Gradle wrapper included |

No purchases, accounts, API keys, or cloud services are required.

## Full instructions

1. Install a 64-bit JDK 26 (verified with Temurin 26.0.1) and set `JAVA_HOME`.
2. Clone `https://github.com/darcy0408/saha-yoga-coach`.
3. Run the tests — no camera or person is needed:
   `gradlew.bat clean test` (Windows) or `./gradlew test` (macOS/Linux).
4. Launch: `gradlew.bat run`. Dependencies download from Maven Central on the
   first build.
5. Select the consent checkbox, review calibration (optionally enabling the
   local camera preview), and start **Steady Start**.
6. Watch for the deliberate low-confidence moment: corrections stop and the
   timer pauses. Try **Easier option**, **Skip**, and **Stop now**.
7. Open the progress page to see derived metrics and the explanation for any
   routine adjustment, then **Delete all local history** if you wish.

## Java 26 verification (required by rules)

- `build.gradle.kts` pins the Gradle Java toolchain to **Java 26**
  (`JavaLanguageVersion.of(26)` by default) and applies `--enable-preview` to
  compile, test, and run tasks.
- Verified on **Temurin 26.0.1** with **Gradle 9.4**; `gradlew.bat clean test`
  passes 37 tests.
- Java 26 preview `LazyConstant` is exercised directly in
  `src/test/java/io/saha/yoga/routine/Java26LazyConstantTest.java`.

## Code and resource links

- Repository (MIT): `https://github.com/darcy0408/saha-yoga-coach`
- Architecture, model card, privacy notes, and asset ledger: `docs/` in the repo

## Demo video

TODO (user): record per `docs/demo-script.md` — the script is timed to the
required 90–120 second window and needs no camera or person on screen.

## Images

TODO (user): capture during the demo run —
1. onboarding/consent screen, 2. calibration with preview and demo badge,
3. coaching screen with cues and confidence, 4. low-confidence pause,
5. progress page with the personalization reason.

## Attribution and licensing

- Saha source: MIT. Dependencies: OpenJFX (GPLv2+CE), ONNX Runtime (MIT),
  OpenPnP OpenCV (BSD-3-Clause), Jackson (Apache-2.0), JUnit (EPL-2.0).
- Two review-gallery illustrations are CC0 1.0 from OpenClipart
  (Gerald_G, files 8248/8249); attribution is not required by CC0 but is
  retained in `docs/third-party-assets.md` with checksums for transparency.
  They appear only in a developer review gallery, not in coaching.
- All other code, UI, text, and documentation are original work by the
  entrant.
