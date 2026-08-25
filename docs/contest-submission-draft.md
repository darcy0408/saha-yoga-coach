# Hackster submission — Modern Java in the Wild

Copy-paste source for the Hackster project post. Each section below maps to a
field in Hackster's project editor. Deadline: **August 16, 2026, 11:59 PM PT**.
Category: **Best Home Solution** (also eligible for Best in Show).

Positioning note, from a review of the published field: no other entry does
computer vision, and none runs a neural network on the entrant's own machine.
Several depend on cloud APIs, keys, or purchased hardware. Saha's advantage is
that it *sees* and *speaks*, needs nothing but a computer and its webcam, and is
built so it cannot claim more than it measured. Lead with those three.

---

## Project name

Saha — the yoga coach that watches you, on your machine only

## Short description (elevator pitch)

A Java 26 yoga coach that watches you through your own webcam, speaks its cues
aloud, and is built so it cannot claim more than it actually measured. No cloud,
no account, no API key, no stored video.

## Cover image

TODO (user): the practice screen mid-pose — your body under the tracked skeleton
on the left, the pose figure and cue panel on the right. That single frame tells
the whole story: it is watching, and it is explaining.

---

## Story

### You cannot read a screen from inside a forward fold

Home yoga has a blind spot that no amount of video production fixes: the
instructor cannot see you. She does not know your knee collapsed inward, that
you have quietly avoided the same pose for three sessions, or that you left the
frame entirely. And the products that *can* see you usually do it by sending
video of your living room to somebody else's servers.

There is a second, quieter problem. Even an app that watches you is useless if
its guidance lives on a screen you cannot look at. Try reading a cue while your
head is below your hips.

Saha (from the Sanskrit *saha*, "together with") answers both. It is a Java 26
desktop coach that estimates your body's landmarks **on this machine**, draws
them over your own video, speaks each pose and correction **out loud**, and is
engineered so that an unmeasured pose can never be praised as a good one.

### What it does

Saha guides a 20-pose beginner practice, sequenced the way a class actually
runs: centering, a floor warm-up, standing work while the body is warm, one
balance while attention is sharpest, floor strength and a gentle backbend, then
seated folds and twists that unwind into final rest. Around twenty minutes, end
to end.

While you practise, it:

- **watches you.** MoveNet SinglePose Lightning runs locally on every frame.
  Your joints and bones are drawn over a mirrored preview, so you can see
  exactly what the coach sees.
- **speaks.** Every pose is announced with its instruction, and corrections are
  spoken through the operating system's own voice — rate-limited so it guides
  rather than chatters, and toggleable.
- **measures, then cues.** Joint angles are checked against pose-specific ranges
  on whichever side the camera can see, and it offers at most **two** supportive
  cues at a time.
- **pauses instead of guessing.** When the camera cannot see what a pose needs,
  the timer stops and it names the body part it has lost — "your feet and knees
  are out of view" — rather than repeating a generic line about lighting.
- **shows the shape.** Each pose carries a minimal line figure, stroked live from
  MIT-licensed vector geometry in the interface's own ink.
- **explains itself.** Only derived metrics — completion, stability, duration —
  are stored, in a local JSON file, and every routine adjustment it makes comes
  with the reason it made it. One button deletes the lot.

A 1–5 intensity control scales hold times without introducing advanced poses,
and deliberately leaves the settling and the final rest alone, because
shortening those works against what they are for.

### Honesty as an architectural property

This is the part I would most like judges to look at.

The analysis pipeline returns one of three sealed result types:

- `Reliable` — every landmark the pose actually needs cleared the confidence
  gate, so measured feedback is allowed;
- `InstructionOnly` — the pose has no implemented geometric rules, so the
  interface says "instruction only" rather than implying alignment was checked;
- `Unreliable` — confidence is too low. This type *cannot carry corrective
  suggestions at all*: the field does not exist on it.

Because `AnalysisResult` is sealed and every consumer pattern-matches
exhaustively, "unmeasured pose accidentally praised as aligned" is not a bug
that testing might catch. It is a compile error.

The same discipline runs through everything else. The model is used only when
its SHA-256 matches the validated artifact — the fetch script refuses anything
else — and without it the app falls back to synthetic landmarks and *says so on
screen* instead of pretending to see you. A landmark below the drawing threshold
is never painted onto the video, so a guess cannot masquerade as an observation.
Two poses were deleted from the practice outright rather than ship them with a
figure that did not match. Poses whose geometry has not been validated on camera
carry no alignment rules at all.

Plenty of fitness software scores you from evidence that cannot support the
score. Saha is built so it structurally cannot.

### Modern Java 26, doing the actual work

Java is not a wrapper around a Python model here. It is the entire product —
capture, inference, geometry, coaching policy, speech, persistence, and
interface.

- **Preview `LazyConstant`** — the immutable 20-pose catalog is built exactly
  once, on first use (`PoseCatalog`). `--enable-preview` is applied consistently
  across compile, test, and run.
- **The Foreign Function & Memory API** — the 9 MB model is read through a
  memory-mapped `MemorySegment` in a confined `Arena` (`PoseEstimator`), so the
  OS pages it in lazily and releases it deterministically instead of leaving a
  nine-megabyte heap copy for the collector.
- **Sealed interfaces + exhaustive pattern matching** — the three-state analysis
  result above, consumed with an exhaustive `switch`.
- **Records** — landmarks, frames, poses, alignment rules, session metrics and
  icons are immutable records across 15 files.
- **Virtual threads, where they belong** — the speech helper's reader and
  writer run on virtual threads (`SystemVoice`). Camera capture and inference
  deliberately do not: that loop is blocking native calls, which would pin a
  virtual thread's carrier for the whole practice, so it gets a platform thread
  (`OpenCvCameraCapture`). The JavaFX side coalesces frames so capture can
  never flood the event queue.
- **Sequenced collections** — `getFirst()` / `getLast()` throughout the routine
  and cue handling, where "the first instruction" and "the last phase" are the
  actual domain concepts.
- **Text blocks** — the speech helper script is a readable block rather than an
  escaped string, and is passed as an encoded command so the shell cannot mangle
  its quoting.
- **JavaFX 26** — onboarding, calibration, the practice screen, the landmark
  overlay, and progress.

Supporting cast: ONNX Runtime (inference), OpenPnP OpenCV (capture), Jackson
(derived-metric JSON), JUnit 5 — **123 tests**, covering geometry, confidence
gating, cue limits, instruction-only truthfulness, bilateral rules, routine
ordering and duration, personalization, persistence, asset licence checksums,
pose-icon coverage, spoken-cue pacing, and the real model driven through the
production analysis boundary.

### Privacy is the default, not a setting

Normal operation saves no video, no images, no face data, no landmark
coordinates, no identity, emotion, age, body shape, or appearance score. Frames
are analysed and discarded. Nothing is uploaded, there is no account, and there
is no API key to obtain — the model runs on your CPU and the voice is
synthesised by your own operating system. Derived pose metrics live in
`~/.saha/sessions.json`, and the progress page deletes them.

Saha is educational fitness software, not medical care. It avoids guarantees and
body scoring, and tells you to stop for pain, dizziness, numbness, weakness, or
unusual discomfort.

### BYOD, in the strictest sense

Any 64-bit computer you already own, and the webcam already built into it. No
Raspberry Pi, no sensor, no breadboard, no cloud subscription, no API key, no
paid service. Total additional cost: nothing. And the judge path works **without
a camera at all** — with no model installed, the app runs its synthetic
demonstration through the identical analysis pipeline and labels itself honestly
while doing it.

### Try it in three commands

```powershell
git clone https://github.com/darcy0408/saha-yoga-coach
.\scripts\fetch-model.ps1     # ~9 MB, no account, checksum-verified
.\gradlew.bat run
```

Consent, continue to camera setup, enable the camera, and start Steady Start.

### What's next

The last day was spent in front of the camera, walking the practice pose by
pose, and it earned its keep: a Warrior lunge shallower than the rule it was
teaching, a leg hidden behind another leg overruling the leg in plain view, a
reach measured at a joint that leaves the frame the moment the arms go up. Each
one is fixed and covered by a test. That is one person, one room, one camera
height and one set of proportions — enough to find real faults, not enough to
call anything validated, and the honest place to start is saying so.

So: validate each measured pose on camera against people actually holding it,
across varied bodies, rooms, and lighting, and widen the rules beyond knee
angles once that evidence exists. Then a full accessibility audit, and floor poses — the
weakest case for a single-camera model — *earning* measurement rather than being
handed it. The pattern is deliberate: this coach adds a claim only after it can
back it.

---

## Brief written summary (required by rules)

Saha is a privacy-first yoga coach written entirely in Java 26. It estimates
your body's landmarks from your own webcam using MoveNet running locally through
ONNX Runtime, draws them over a mirrored preview, and speaks each pose and
correction aloud through the operating system's voice — because nobody can read
a screen from inside a forward fold. It guides a 20-pose, twenty-minute practice
sequenced like a real class, offers at most two cues at a time, and pauses while
naming the body part it cannot see rather than guessing. Sealed result types make
overclaiming a compile error: the unreliable case structurally cannot carry a
correction. Java 26 preview LazyConstant builds the pose catalog, the Foreign
Function & Memory API memory-maps the model, virtual threads carry capture and
speech, and records and exhaustive pattern matching run the domain. No cloud, no
account, no API key, no stored video.

---

## Bill of materials

| Item | Notes |
| --- | --- |
| 64-bit Windows / macOS / Linux PC | Any machine that runs JDK 26 (BYOD) |
| Built-in or USB webcam | Optional — the judge path runs without one |
| Temurin JDK 26.0.1 (free) | Or any 64-bit JDK 26 |
| Saha source code (free, MIT) | github.com/darcy0408/saha-yoga-coach |
| MoveNet SinglePose Lightning (free) | ~9 MB, fetched by script, checksum-pinned |

No purchases, accounts, API keys, or cloud services are required.

## Full instructions

1. Install a 64-bit JDK 26 (verified with Temurin 26.0.1) and set `JAVA_HOME`.
2. Clone `https://github.com/darcy0408/saha-yoga-coach`.
3. Run the tests — no camera or person needed:
   `gradlew.bat clean test` (Windows) or `./gradlew clean test` (macOS/Linux).
4. Fetch the pose model: `.\scripts\fetch-model.ps1`. It downloads ~9 MB and
   **refuses any file whose SHA-256 does not match** the validated artifact.
   Skip this step and the app runs its labelled synthetic demonstration instead.
5. Launch: `gradlew.bat run`. Dependencies come from Maven Central on first build.
6. Accept the consent checkbox, review calibration, and enable the local camera
   preview — your tracked joints appear over your own video.
7. Start **Steady Start**. Listen: each pose is announced aloud. Step out of
   frame and watch the timer pause and the app name what it lost.
8. Try **Easier option**, **Repeat cue**, **Spoken guidance**, and **Camera
   colour**. **Stop now** is always on screen.
9. Open the progress page for derived metrics and the explanation of every
   routine adjustment, then **Delete all local history**.

## Java 26 verification (required by rules)

- `build.gradle.kts` pins the Gradle Java toolchain to **Java 26**
  (`JavaLanguageVersion.of(26)`) and applies `--enable-preview` to compile,
  test, and run tasks.
- Verified on **Temurin 26.0.1** with **Gradle 9.4**; `gradlew.bat clean test --no-build-cache --rerun-tasks`
  passes **123 tests**.
- Java 26 preview `LazyConstant` is exercised directly in
  `src/test/java/io/saha/yoga/routine/Java26LazyConstantTest.java`.

## Code and resource links

- Repository (MIT): `https://github.com/darcy0408/saha-yoga-coach`
- Architecture, model card, privacy notes, and asset ledger: `docs/` in the repo
- Model provenance, tensor layout, and known limitations: `docs/model-card.md`

## Demo video

TODO (user): record per `docs/demo-script.md` — timed to the required 90–120
seconds. Show the tracked skeleton on your body, let the voice be heard, and
step out of frame so the pause and its named cause land on camera.

## Images

TODO (user):
1. practice screen with your tracked skeleton and the cue panel (cover),
2. the moment the timer pauses and names what it cannot see,
3. calibration with the landmark overlay,
4. `build/review/routine.png` — the whole 20-pose practice at a glance,
5. progress page with the personalization explanation.

## Attribution and licensing

- Saha source: MIT. Dependencies: OpenJFX (GPLv2+CE), ONNX Runtime (MIT),
  OpenPnP OpenCV (BSD-3-Clause), Jackson (Apache-2.0), JUnit (EPL-2.0).
- Pose figures: Atlas Icons yoga pack by Ramy Wafaa / Vectopus, **MIT**; the
  licence text ships beside the geometry in
  `src/main/resources/io/saha/yoga/illustrations/atlas/`.
- Warrior II illustration: **CC0 1.0** by Gerald_G via OpenClipart, checksum
  pinned and credited on screen although CC0 does not require it.
- Pose model: MoveNet SinglePose Lightning, fetched from its published source
  and SHA-256 verified; not redistributed in the repository.
- All other code, interface, text, and documentation are original work by the
  entrant. Full reasoning for every asset decision is in
  `docs/third-party-assets.md`.
