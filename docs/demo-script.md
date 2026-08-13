# Contest demo video script (90–120 seconds)

The Modern Java in the Wild rules require a demo video of **90–120 seconds**.
This script times out at roughly 115 seconds at a calm speaking pace. Record
the screen at the app's default window size; no camera or person is required
because the judge path runs on deterministic demo landmarks.

| Time | Screen | Narration and actions |
| --- | --- | --- |
| 0:00–0:15 | Onboarding | "Home yoga videos can't see you, and camera apps that can usually ship your video to the cloud. Saha is a Java 26 desktop coach that keeps every frame on your machine." Select the consent checkbox and continue. |
| 0:15–0:30 | Calibration | "Calibration explains camera height, framing, and light. The preview is local and transient — and this badge is honest: coaching in this build runs on deterministic demo landmarks, not a hidden model." |
| 0:30–1:00 | Coaching | Start **Steady Start**. "Each pose shows structured instructions, at most two supportive cues, and a live confidence value. When confidence drops below 0.70 —" (wait for the injected low-confidence frame) "— corrections stop and the timer pauses instead of guessing." |
| 1:00–1:15 | Controls | Click **Easier option**, then **Skip** through a pose or two. "Easier options, pause, repeat, skip — and a stop control that never leaves the screen." |
| 1:15–1:35 | Progress and privacy | Stop the session. Show progress, the routine-change reason, then click **Delete all local history**. "Saha stores only derived metrics in local JSON, explains every personalization decision, and deletes everything on request." |
| 1:35–1:55 | Close (code or architecture slide optional) | "Java 26 runs all of it: JavaFX for the UI, records for immutable landmarks, sealed analysis results so an unmeasured pose can never be called aligned, preview Lazy Constants for the pose catalog, and virtual threads for capture. Live MoveNet inference is a documented next step — not a fabricated claim." |

## Recording checklist

- Total length must land between 90 and 120 seconds — rehearse once with a timer.
- 1080p screen capture, app window centered, no personal files visible.
- The injected low-confidence pause occurs during normal demo playback; wait for it rather than cutting around it.
- No third-party music (contest rules prohibit unlicensed copyrighted material).

## Extended live-demo walkthrough (3–5 minutes, not for the video)

For judges or viewers who want the longer tour, the original walkthrough
remains useful as an in-person script:

1. **Onboarding:** Saha asks only what helps shape a practice, states the safety boundary, and stores no identity. Select consent.
2. **Calibration:** Camera height, full-body framing, light, floor space, and the honest demo-mode badge.
3. **Coaching:** Start Steady Start. Read the pose/status/suggestion/confidence structure. Wait for the injected low-confidence frame to show correction and timing pause. Click **Easier option**, then **Pause/Resume**.
4. **Control:** Skip through poses and show the persistent **Stop now** control.
5. **Progress and privacy:** Stop. Show completed pose count and stability as derived metrics. Explain the routine-change reason and click **Delete all local history**.
6. **Close:** Java 26 runs the UI, analysis, coaching, persistence, and transparent personalization. Live MoveNet remains a documented next step, not a fabricated claim.
