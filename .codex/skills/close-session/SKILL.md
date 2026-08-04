---
name: close-session
description: Close and hand off a development session in the Saha Java 26 yoga-coach repository. Use when the user says close session, end session, wrap up, stop work, or requests a Saha handoff. Review changes, run Java 26 verification, audit privacy and coaching claims, update persistent project status, and report Git state without publishing unless explicitly authorized.
---

# Close Session

Leave a verified, truthful handoff that another session can resume immediately.

## Workflow

1. Inspect `git status --short --branch`, `git diff --stat`, relevant diffs, and recent commits. Preserve unrelated and user-owned changes.
2. Audit the session:
   - No credentials, private media, raw frames, landmark histories, personal free text, or local absolute paths are committed.
   - Logs contain no images, coordinates, detailed measurements, or identifying data.
   - Preview-only camera behavior is not described as inference.
   - Unmeasured poses use instruction-only language, low confidence suppresses corrections, and no more than two cues appear.
   - Teaching visuals are not enabled without provenance, license, and review status.
3. Verify on Java 26. On Windows, set session-local `JAVA_HOME` to the installed Temurin 26 directory when required, then run `./gradlew.bat clean test`. Report the count from `build/test-results/test/TEST-*.xml`; never reuse an old count. Run `git diff --check` and focused snapshot or launch tasks when relevant.
4. Separate automated and manual evidence. Camera access, pose anatomy, contrast, clipping, keyboard navigation, and real-session usability need explicit manual results or screenshots.
5. Update `PLAN.md` with the date, completed slice, exact successful and failed commands, blockers, and next task. Update `README.md`, `docs/final-report.md`, the model card, privacy docs, or asset credits only when facts changed.
6. If authorized to commit, review the staged diff and commit only intended files. Do not push, merge, publish, open a PR, or delete branches unless separately authorized.
7. Re-run `git status --short --branch` and report whether the branch is clean, dirty, ahead, or behind.

## Handoff format

Provide:

- Completed work and user-visible behavior.
- Verification commands and actual results.
- Manual checks completed or required.
- Git branch, commit, and synchronization state.
- Known limitations or blockers.
- One recommended next vertical slice with its acceptance test.

Never claim work is on GitHub unless the push succeeded, or that live coaching works until verified ONNX landmarks drive the analyzer.
