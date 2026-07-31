# Pose model setup

No model is bundled in this contest-safe baseline. Demo mode works immediately.

The planned adapter expects a verified MoveNet SinglePose Lightning ONNX model at:

```text
models/movenet-singlepose-lightning.onnx
```

Before enabling it:

1. Record the authoritative download URL and redistribution license.
2. Pin and verify SHA-256.
3. Confirm input layout, size, RGB normalization, and output landmark ordering against the exporting project's documentation.
4. Run synthetic and prerecorded fixture tests on JDK 26.
5. Validate suppression at confidence below 0.70 and measure CPU latency.
6. Add the hash and evaluation results to `docs/model-card.md`.

Never download a similarly named community conversion and assume its tensors or license match.

