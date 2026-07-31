# Model card: Saha landmark pipeline

## Current status

The repository ships a deterministic synthetic landmark source, not a learned model. It exists to test the complete coaching workflow without a camera and must not be presented as evidence of real-world pose-estimation accuracy.

## Planned model

The intended Phase 2 model family is MoveNet SinglePose Lightning exported to ONNX, executed locally using ONNX Runtime Java CPU. No model file is bundled until its origin, redistribution license, SHA-256 checksum, expected input/output tensors, and fixture accuracy are verified.

## Intended use

Estimate coarse 2D body landmarks for low-risk educational yoga cues. Not intended for diagnosis, clinical assessment, identity, emotion, age, body composition, high-risk poses, or claims of safety.

## Limitations and evaluation

Single-camera 2D estimation is sensitive to occlusion, loose clothing, light, camera angle, skin/background contrast, mobility aids, and bodies underrepresented in training data. Evaluation must include varied bodies, clothing, assistive devices, camera positions, and lighting. Report per-landmark confidence, suppression rate, inference latency, and cue false-positive rate; do not report a single “correct pose” score.

