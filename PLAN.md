# Saha delivery plan

Last updated: 2026-08-24

## This session (2026-08-24)

Everything that could move without a body in front of the camera, so that the
one session needing a body answers everything at once.

- **A diagnostics view for that session.** A "Diagnostics" button on the camera
  practice screen (absent in demo mode) shows the raw per-joint scores the
  model reports before smoothing - shoulders, hips, wrists, knees, ankles,
  left|right - plus the crop region, per-frame cost, and the landmark rate the
  pipeline sustains. In the camera-picture view the crop draws itself as a
  gold dashed box: hunting, clipping a limb, or collapsing are visible at a
  glance; no box means the model is seeing the whole frame. The readout is
  deliberately unsmoothed - judging the model through the smoother would
  measure the smoother. Vision plumbing is tested
  (`diagnosticsReportTheRegionTheModelWasActuallyShown`); the JavaFX layer
  compiles but no automation renders the coach screen, same as the rest of it.
- **`gradlew visionBench` answers the frame-rate question a machine can
  answer.** Median per-frame estimate cost, whole-frame path: Lightning ~8 ms
  at 640x480 and ~11 ms at 1280x960; the copy `CameraFrame` takes on
  construction is 0.1-0.5 ms. The capture-size increase costs ~3 ms against a
  33 ms budget at 30 fps: **capture size will not be the bottleneck on this
  machine.** What the bench cannot answer is tracking quality - that stays
  with the live session.
- **The MoveNet Thunder candidate is verified as far as a machine alone can.**
  Same converter and packaging as the Lightning export (Apache-2.0); SHA-256
  pinned in `scripts/fetch-model.ps1 -Thunder` and matching Hugging Face's own
  LFS manifest; loads in ONNX Runtime, declares 256x256, decodes through the
  production path in range (a test that skips when the candidate is absent);
  ~25/29 ms per frame - affordable at 30 fps, little headroom. **Not enabled
  and cannot be enabled by accident**: the locator only finds Lightning by
  name; Thunder takes the `saha.model` property, deliberately, after a
  real-body validation. `visionBench -PbenchModel=...` times any candidate.
- **The docs sweep found the pipeline claims were the smaller problem.**
  `docs/architecture.md` and `docs/final-report.md` still described the phase
  before live inference existed - "a future live landmark adapter", "no model
  bundled", "no TTS", 37 tests - and both claimed camera capture runs on a
  virtual thread when `OpenCvCameraCapture` deliberately uses a platform
  thread (blocking native calls pin a virtual carrier). The same false
  virtual-thread claim sat in `docs/contest-submission-draft.md` and is
  corrected there too; any copy of that draft published elsewhere would need
  the same correction. Both docs rewritten to current truth; the architecture
  diagram's gate said 0.70 against its own text's 0.35.
- **A threshold error introduced into the model card on 2026-08-23, fixed.**
  Yesterday's edit said the analyzer returns Unreliable below 0.30; the
  analyzer's gate is 0.35 (`PoseAnalyzer.RELIABILITY_THRESHOLD`), and 0.30 is
  the drawing/geometry threshold. The card now states both numbers and what
  each governs.
- **`lets-dance` updated rather than ported into.** It has no vision pipeline
  yet (scaffold only), so its PLAN.md now records that the crop landed in Saha
  first, what a port must keep (hip-centred sizing, the 1.9x torso floor, the
  whole-frame snap-back) and what it must revisit (`FOLLOW = .5` assumes a
  body holding still - a travelling step could outrun its own crop). Its open
  question about backport direction is resolved: the port flows from Saha.

## This session (2026-08-23)

Step 1 of the vision audit, the fix both open symptoms were traced to: the model
is no longer shown the whole letterboxed frame.

- **`PersonCrop` chooses a square patch around the person** from the previous
  frame's answer, which is what MoveNet's own reference pipeline does and this
  code never did. The region is centred on the hips - the middle of a standing
  body's vertical extent - and sized at 1.9x the torso or 1.2x the joints
  actually seen, whichever is larger. Losing the torso resets to the whole
  frame; the region eases rather than jumps, so the framing the model sees does
  not change under it every frame.
- **Centring it anywhere but the hips does not work**, which the tests caught
  rather than review. Centred on the middle of the torso - a third of the way up
  the body - a region that reaches the feet must reach as far above the head,
  and it swells back to the whole frame. That is where the reference's 1.9
  comes from, and it only holds measured from the hips.
- **The crop cannot starve a limb it has already lost.** A region sized only
  from confidently-seen joints would close around the torso of a cross-legged
  body, put the faint legs outside the patch, and measure the next region from a
  body with no legs at all. The torso factor exists to stop exactly that, and a
  test asserts legs scoring 0.12 stay inside the region.
- **Capture now negotiates the largest 4:3 mode the device offers**, verified
  against a real frame rather than trusted: `gradlew cameraCheck` reports camera
  0 sending **1280x960**, where it defaulted to 640x480 before. 4:3 is deliberate
  - the device also offers 1920x1080 and 1280x720, and widescreen would trade
  away the vertical view a standing body needs.
- **Frames are copied twice rather than four times.** `CameraFrame` cloned its
  payload on construction and again on every read, and the two live consumers
  only read. At 1280x960 the old path would have put roughly 590 MB/s through
  the collector, which in a coach that keeps time shows up as stutter.
- **Nothing is reported below the bottom of the picture any more.** A keypoint
  predicted into the letterbox padding used to map to y as far as 0.875 on a
  frame whose floor is 0.75.

## Completed

- Java 26 / Gradle 9.4 build verified on Temurin 26.0.1.
- Twenty-pose beginner routine, JavaFX workflow, derived-only persistence, and demo mode.
- Truthful `Reliable`, `InstructionOnly`, and `Unreliable` analysis outcomes.
- Java 26 preview `LazyConstant` for the pose catalog; FFM `MemorySegment` model load.
- Licensed teaching-asset catalog and review gate.
- Live MoveNet inference drives the analyzer; checksum-verified model fetch.
- Camera capture opens with DirectShow and never requests Media Foundation by name.
- Spoken guidance: pose announcement, two practical entry steps per pose, corrections,
  arrival confirmation, and an end-of-practice close.

## This session (2026-08-20)

An adversarial audit of the vision pipeline, prompted by a suspicion that the two
open symptoms had a common cause. They largely do, and it is upstream of both:

- The camera opens at 640×480 and `PoseEstimator` letterboxes the *whole* frame
  into MoveNet's 192×192 input, so a standing body spans about 144 pixels and a
  raised wrist two or three. MoveNet's reference pipeline crops to the person
  found in the previous frame; that step was never implemented here. "The model
  stops tracking wrists overhead" was therefore an untested hypothesis stated as
  fact, and the README now says so.
- `LandmarkSmoother` slows a joint's position update in proportion to its
  confidence, so a wrist whose score falls as it rises visibly parks mid-lift,
  then the drawing fade dissolves it. Correct for a held pose; it made the
  symptom look total when the underlying score drop was partial.
- Seated legs are the same starvation, deeper: a seated body is smaller in frame
  again, on top of the model's genuine weakness on non-upright bodies.

Removed a `failed` flag in `CameraLandmarkSource` whose only reader returned the
same value on both branches.

The sibling project `lets-dance` was scaffolded from this audit; its PLAN.md
carries the findings as design constraints, and the person-crop work is step one
in both plans. If it lands there first, backport rather than reimplement.

## This session (2026-08-16/17)

Pose geometry, corrected against a person on camera:

- Both warriors re-authored at `constrain()`'s bone lengths. The front knee measured
  152 degrees against a rule demanding 80-145, so the reference contradicted itself;
  it is now 102 degrees with a vertical shin and both feet flat on the floor.
- Cat-cow was a straight-legged stilt (178-degree knee) standing on hands and feet.
  It is now a true tabletop: 90-degree knees, with hands, knees and toes on the mat.
- Goddess sunk to thighs-parallel (92 degrees). Seated side reach bows outward.
- Feet lie flat rather than angling down from the ankle, which read as tiptoes.

Measurement and coaching:

- A leg hidden behind the other no longer overrules the leg in plain view. It cleared
  the confidence gate while largely inferred, and `MOST_BENT` took the smaller angle,
  so a correct lunge was told its knee had passed its ankle.
- Locust, Easy Seat and Seated Side Reach are measured, so they can chime. The seated
  rules read the head and shoulders, never the hips: sitting cross-legged puts the
  thighs across the hip joints and the model scores them near zero.
- Crossing into range rings the chime and says "Great! Now hold that pose."
- The target figure mirrors when the coach calls the change of sides.

Interface:

- The teaching card no longer draws outside its own background. Its snapshot now
  drives the real card through the application class at the real size.
- Final Rest shows its whole figure; that icon ships its own ground line, which the
  view was hanging the body from.
- The seated twist shows no figure: a twist is rotation about the spine, which a flat
  line drawing cannot depict, and the candidate icon was rejected on review.

Tooling:

- `gradlew cameraCheck` reports which devices and backends this machine can open,
  exercising the production capture class before loading anything.
- `gradlew chimeCheck` plays the chime and reports whether audio is available.
- `gradlew coverSnapshot` renders the project cover from the application itself.

A joint the camera half-sees is now drawn faintly rather than dropped, so a body
sitting cross-legged keeps its legs. This took two changes, and an earlier attempt at
only the second was reverted for looking worse than the missing limb:

- `LandmarkSmoother` follows a position in proportion to confidence rather than at a
  fixed weight. A low score does not mean the coordinates are roughly right; it means
  they are largely invented and thrash frame to frame, so following them at full speed
  made the hidden leg crawl around the screen. A joint in plain view keeps up exactly
  as before; one the model has little opinion about parks near where it was last really
  seen.
- `SahaApp.visibility()` fades a joint continuously between the drawing threshold and a
  floor of 0.08, below which nothing is drawn. The reverted attempt used a hard step at
  the threshold, which made any joint hovering near the line flip between solid and
  faint several times a second.

The measurement gate is untouched: a faint leg still cannot earn a correction or a
chime, and the two places that use confidence for geometry rather than for drawing -
fitting the guide figure to the hips, and bowing the spine from the head - keep the
strict threshold.

## Verification

`./gradlew.bat clean test --no-build-cache --rerun-tasks` on Temurin 26.0.1,
2026-08-24: **123 tests, 0 failures, 0 skipped** (24 classes, with the Lightning
model and the Thunder candidate both present so every estimator fixture runs
rather than skips; a clone without the optional Thunder file runs 122, skips 1). The flags are not decoration: with the
build cache on, `clean test` returns `:test FROM-CACHE` in under a second without
executing anything, which is easily mistaken for a passing run.
`git diff --check` clean. `gradlew cameraCheck`: camera 0 opens on DirectShow and
delivers 1280x960 to the production capture class.

**What the 19 new tests do and do not cover.** The crop geometry is tested
without a model, and the image transform - what the model is actually shown, and
whether a point in it maps back to where it came from - is tested without one
too, so both run on a clone that has never fetched weights. What no test reaches
is the real model on a real cropped body: synthetic noise scores every joint
below the gate, so the model-backed fixtures only ever exercise the whole-frame
fallback, and the repo has no photograph of a person to feed it. **Whether the
crop actually recovers overhead wrists and seated legs is unverified and can
only be settled by a live camera.**

`./gradlew.bat clean test` on Temurin 26.0.1, 2026-08-20: **102 tests, 0 failures**
(counted from `build/test-results/test/TEST-*.xml`, 22 classes, with the model
present so the estimator fixtures run rather than skip).
`git diff --check` clean. `gradlew cameraCheck`: camera 0 and 1 open on DirectShow at
640x480; Media Foundation hangs inside `open()` on this hardware and is not used.

Manual, on one person in one room: standing poses track and coach correctly. Seated
poses drew no legs; the fade above is verified only in `gradlew figureSnapshot`, which
now renders Easy Seat, Seated Side Reach and Seated Fold at the confidence a real
camera reports. **Whether the faint legs sit still or crawl on a live camera has not
been checked by anyone.** A still image cannot show jitter, and jitter is exactly what
sank the previous attempt.

## Next

1. **Stand in front of the camera.** One session with a body in frame now settles
   four open questions at once, and nothing else here should move until it does.
   Press **Diagnostics: on** during it - the readout and the gold crop box were
   built for exactly this - and switch to "View: camera picture" to see the box:
   - Do wrists survive overhead, and do seated legs score above the gate? These
     are what the crop was built for, and neither has been observed.
   - Do the faint seated legs sit still or crawl? Outstanding from 2026-08-20 and
     still verified only in a snapshot, which cannot show jitter. If they crawl,
     the next lever is a variance gate - draw a low-confidence limb only once its
     position has settled - not another revert of the fade.
   - Does the crop hold on a real body, or does it hunt, clip a limb, or drop
     back to the whole frame repeatedly? Constant resets mean the torso gate is
     too strict; a clipped limb means the margins are too tight.
   - Does 1280x960 cost noticeable frame rate on this laptop? Half answered by
     `visionBench` on 2026-08-24: the pipeline's own cost is ~11 ms per frame,
     well inside a 30 fps budget, so any stutter seen live would come from
     somewhere else (camera delivery, preview drawing) - the diagnostics
     readout shows the sustained rate to check against.
2. Re-check the angle ranges afterwards. They were tuned against a starved model,
   so a better-fed one may read the same pose differently. Do not touch a range
   before step 1. MoveNet Thunder (256 px) is the follow-up if the crop was not
   enough.
3. Validate each measured pose against people actually holding it, across varied
   bodies, rooms and lighting.
4. Record the demonstration video.
5. Floor poses earning measurement rather than being handed it.

## Blockers

- No pose model may be enabled until its provenance, license, checksum, input tensor,
  output ordering, and fixture behavior are verified.
- A reviewed, aesthetically consistent public-domain teaching set for all twenty poses
  has not been found; poses fall back to written guidance rather than shipping art
  that has not been looked at.
