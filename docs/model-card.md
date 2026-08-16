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
- **Input**: `[1, 192, 192, 3]`, RGB, letterboxed with neutral grey padding so a
  4:3 frame is not stretched. The declared tensor dtype is read from the model
  and the input is built to match (uint8, int32 or float).
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
and silently corrupt every joint angle the coach measures.

## Confidence and suppression

Per-landmark confidence flows from the model into the existing confidence gate
unchanged. Below it the analyzer returns `Unreliable`, which by construction
cannot carry corrective suggestions, and the pose timer pauses. Landmarks below
0.30 are not drawn at all, so a guess never appears on screen as an
observation. Before the first inference completes the source reports an empty
frame, which the analyzer treats as unreliable rather than as a good pose.

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

## Still to validate

On-camera validation of each measured pose — chair, both warriors, triangle,
tree and low lunge — with the angle ranges checked against a person actually
holding the shape. Until a pose has been through that, its cues should be read
as plausible rather than proven.
