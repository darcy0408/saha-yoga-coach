# Pose model setup

Weights are not committed to git — they are large, and they are not ours.
Saha runs in synthetic demo mode until the model is present, so the app and its
tests work immediately after a clone.

To enable real landmark estimation from your camera:

```powershell
.\scripts\fetch-model.ps1
```

That downloads MoveNet SinglePose Lightning (~9 MB, no account or API key) to
`models/movenet-singlepose-lightning.onnx` and **verifies its SHA-256 against
the artifact this project validated**. A mismatch stops the script: never
download a similarly named community conversion and assume its tensors or
license match.

Then run the app and enable the camera during setup:

```powershell
.\gradlew.bat run
```

Provenance, tensor layout, keypoint mapping, confidence handling, and known
limitations are recorded in [../docs/model-card.md](../docs/model-card.md).
The fixture tests in `PoseEstimatorTest` exercise the real model through the
production analysis boundary, and skip themselves when it is absent.
