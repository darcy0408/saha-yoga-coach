# Saha delivery plan

Last updated: 2026-08-17

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

Reverted: drawing low-confidence joints at reduced opacity. Position is smoothed at a
fixed weight regardless of confidence, so faded joints still thrashed.

## Verification

`./gradlew.bat clean test` on Temurin 26.0.1, 2026-08-17: **95 tests, 0 failures**.
`git diff --check` clean. `gradlew cameraCheck`: camera 0 and 1 open on DirectShow at
640x480; Media Foundation hangs inside `open()` on this hardware and is not used.

Manual, on one person in one room: standing poses track and coach correctly. Seated
poses draw no legs, which is the open issue below.

## Next

1. Legs are not drawn in seated poses. First establish whether they are inside the
   camera frame at all - DirectShow may hand back a narrower field of view than the
   backend used previously. If they are in frame, weight position smoothing by
   confidence in `LandmarkSmoother` so an uncertain joint parks near its last good
   position instead of following noise; only then is drawing it faintly worth trying.
2. Validate each measured pose against people actually holding it, across varied
   bodies, rooms and lighting.
3. Record the demonstration video.
4. Floor poses earning measurement rather than being handed it.

## Blockers

- No pose model may be enabled until its provenance, license, checksum, input tensor,
  output ordering, and fixture behavior are verified.
- A reviewed, aesthetically consistent public-domain teaching set for all twenty poses
  has not been found; poses fall back to written guidance rather than shipping art
  that has not been looked at.
