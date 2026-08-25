# Model card: Saha landmark pipeline

## Current status

Saha estimates real body landmarks on this device when a verified model is
present, and falls back to a deterministic synthetic source when it is not.

- **Model**: MoveNet SinglePose Lightning, ONNX export
- **Source**: https://huggingface.co/Xenova/movenet-singlepose-lightning
  (`onnx/model.onnx`), fetched by `scripts/fetch-model.ps1`
- **SHA-256**: `1AD4F8D6C2F776A9967DB3993C9CA740BC350104F9D37C151DC183FC29A464AD`
  — pinned in the fetch script, which refuses a file that does not match
- **Size**: 9,413,268 bytes (~9 MB); not committed to git
- **Input**: `[1, 192, 192, 3]`, RGB. The model is shown a square region cropped
  around the person, chosen from the previous frame's keypoints, and scaled
  uniformly into the input with neutral grey padding for any part of the region
  lying outside the frame — never stretched, because a stretched body has wrong
  joint angles. Until a body has been found, and whenever the torso is lost, the
  region is the whole frame, which is the letterbox this did before the crop
  existed. The declared tensor dtype is read from the model and the input is
  built to match (uint8, int32 or float).
- **Output**: `[1, 1, 17, 3]` ordered **y, x, score** — not x, y. Reversing
  these rotates the whole skeleton ninety degrees.
- **Execution**: ONNX Runtime Java, CPU, one intra-op thread, on the capture
  thread. Nothing is uploaded and no frame is written to disk.

The same artifact was previously validated in a separate project on a real
body, where a repetition counter driven by its keypoints matched observed
repetitions exactly across several sessions.

## Mapping into the coaching pipeline

MoveNet emits 17 COCO keypoints. Saha uses 13 of them directly (nose,
shoulders, elbows, wrists, hips, knees, ankles) and ignores the eye and ear
keypoints. Saha's `LEFT_HAND`, `RIGHT_HAND`, `LEFT_TOE` and `RIGHT_TOE` have no
MoveNet equivalent and are placed a short fixed distance beyond the wrist and
ankle along the limb's own direction, for drawing only.

Both axes are normalized by the frame **width**, so the body keeps its true
proportions. Normalizing y by the frame height instead would stretch the figure
and silently corrupt every joint angle the coach measures. On a 4:3 frame that
puts the bottom of the picture at 0.75, and nothing is reported below it: a
keypoint the model places in the padding is outside what the camera actually
photographed.

## Confidence and suppression

Per-landmark confidence flows from the model into the coaching pipeline
unchanged, where two thresholds use it for different jobs, deliberately:

- **Analysis, 0.35.** When the least confident landmark a pose requires falls
  below `PoseAnalyzer.RELIABILITY_THRESHOLD`, the analyzer returns
  `Unreliable`, which by construction cannot carry corrective suggestions, and
  the pose timer pauses. A guess can therefore never earn a correction or a
  chime.
- **Drawing, 0.30 fading to a floor of 0.08.** A landmark at or above 0.30 is
  drawn solid. Below that it fades continuously, and under 0.08 nothing is
  drawn — a body the camera half-sees keeps the limb, at an opacity that shows
  how little the model is committing to it, rather than losing it entirely.
  The two places that use a landmark's position for drawing-support geometry
  rather than measurement — fitting the guide figure to the hips and bowing
  the spine from the head — hold the strict 0.30 rather than fading.

Before the first inference completes the source reports an empty frame, which
the analyzer treats as unreliable rather than as a good pose.

## Intended use

Estimate coarse 2D body landmarks for low-risk educational yoga cues. Not
intended for diagnosis, clinical assessment, identity, emotion, age, body
composition, high-risk poses, or claims of safety.

## Limitations and evaluation

Single-camera 2D estimation is sensitive to occlusion, loose clothing, light,
camera angle, skin/background contrast, mobility aids, and bodies
underrepresented in training data. Floor poses are the weakest case: a body on
hands and knees is far from the standing figures such models see most, so
Saha's floor poses carry no alignment rules and remain instruction-only.

Evaluation must include varied bodies, clothing, assistive devices, camera
positions, and lighting. Report per-landmark confidence, suppression rate,
inference latency, and cue false-positive rate; do not report a single
"correct pose" score.

## The Thunder candidate

MoveNet SinglePose Thunder is the named fallback if the person crop alone does
not recover overhead wrists and seated legs: the same architecture at a
256-pixel input, so the body lands on nearly twice the pixels per axis. A
candidate has been verified as far as a machine alone can (2026-08-24):

- **Source**: https://huggingface.co/Xenova/movenet-singlepose-thunder
  (`onnx/model.onnx`) — the same converter and packaging as the Lightning
  export above; Apache-2.0.
- **SHA-256**: `3DCA9F6E5F8A64DC9935A5BE06FD8BF81BF01E696C9C05C6F2A650E0A401B763`,
  matching Hugging Face's own LFS manifest for the file — pinned in
  `scripts/fetch-model.ps1 -Thunder`, which refuses a file that does not match.
- **Size**: 25,067,197 bytes (~24 MB); not committed to git.
- **Tensor behavior**: loads in ONNX Runtime, declares a 256×256 input, and
  its output decodes through the production path with every landmark in range
  (`PoseEstimatorTest.theThunderCandidateWhenPresentLoadsAndAnswersInRange`,
  which skips when the candidate is absent).
- **Cost on the development machine** (`gradlew visionBench
  -PbenchModel=models/movenet-singlepose-thunder.onnx`): ~25 ms per frame at
  640×480 capture and ~29 ms at 1280×960, against Lightning's ~8 and ~11.
  Inside a 30 fps budget, but with little headroom at one inference thread.

**The candidate is not enabled and cannot be enabled by accident.** The
application only ever looks for the Lightning file by name; running Thunder
requires deliberately setting the `saha.model` system property. What remains
before it may be enabled is the part no machine can do: accuracy on a real
body, on camera.

## Still to validate

On-camera validation of each measured pose — chair, both warriors, triangle,
tree and low lunge — with the angle ranges checked against a person actually
holding the shape. Until a pose has been through that, its cues should be read
as plausible rather than proven.

Whether the person crop and 1280×960 capture recover overhead wrists and
seated legs on a live camera — and if they do not, whether the Thunder
candidate above does.
