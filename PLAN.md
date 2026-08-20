# Saha delivery plan

Last updated: 2026-08-20

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

1. Feed the model a crop centred on the person instead of the whole letterboxed
   frame, and capture above 640×480. The 2026-08-20 audit traced both open
   symptoms — wrists lost overhead, legs unseen while seated — largely to the
   body spanning only ~144 of the input's 192 pixels. Crop around the previous
   frame's keypoints with a generous margin, fall back to the full frame when
   tracking is lost, and re-test both symptoms before touching any angle range.
   MoveNet Thunder (256 px) is the follow-up if the crop alone is not enough.
2. Watch the faint seated legs on a live camera. If they sit still, this is done and
   the crop above will firm them up further; if they crawl, the damping is too weak,
   and the next lever is to draw a low-confidence limb only once its position has
   actually settled - a variance gate - rather than reverting the fade again.
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
