---
name: open-session
description: Open or resume a development session in the Saha Java 26 yoga-coach repository. Use when the user says open session, start session, resume Saha, continue the yoga project, or asks what to work on next. Reconstruct repository state, protect existing work, select the next contest-relevant vertical slice, and begin implementation unless the user requests status only.
---

# Open Session

Resume work from repository evidence rather than conversation memory.

## Workflow

1. Confirm the workspace is Saha. Expect `build.gradle.kts`, `PLAN.md`, `docs/`, `models/`, and `src/`.
2. Run `git status --short --branch`, `git log -5 --oneline`, and `git remote -v`. Treat dirty and untracked files as user-owned until their origin is clear. Never discard them.
3. Read `PLAN.md`, `README.md`, `docs/development-plan.md`, `docs/architecture.md`, and files relevant to the active slice. Prefer current code and Git history when documentation is stale; repair stale facts during the slice.
4. Run `java -version` and `javac -version`. Require Java 26 for authoritative verification. On Darcy's Windows host, use session-local `JAVA_HOME` at `C:\Program Files\Eclipse Adoptium\jdk-26.0.1.8-hotspot` only when necessary. Camera behavior needs a manual device check.
5. Reconstruct the milestone, last verified command, branch divergence, blockers, and smallest useful vertical slice. Update `PLAN.md` if stale.
6. Announce the immediate slice and verification command, then begin unless the user requested status only.

## Priority order

1. Truthful camera and landmark behavior; never describe preview-only video as analyzed.
2. Verified, licensed ONNX model provenance and deterministic tensor/landmark tests.
3. Confidence gating and bilateral, pose-specific measurements without false reassurance.
4. Accurate, licensed, human-reviewed teaching visuals; rejected drafts stay developer-only.
5. Accessible JavaFX UX, explainable personalization, contest docs, and demo reliability.

## Guardrails

- Keep frames local and transient. Never add private media, raw frames, personal landmark histories, identity analysis, or sensitive logs.
- Never call an unmeasured pose aligned, safe, or correct. Use instruction-only language when evidence is absent.
- Never enable teaching art without documented provenance, license, and visual review.
- Preserve demo mode as the camera-free fallback.
- Keep Java 26 preview flags consistent while `LazyConstant` remains preview.
- Do not commit, push, merge, publish, or open a PR unless explicitly authorized.
- Use `apply_patch` for manual source edits and run focused tests before the full suite.

## Opening report

State branch and dirty/ahead/behind status, milestone, blockers, immediate target, and intended verification. Do not repeat the full history.
