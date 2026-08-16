# Contest demo video script (90–120 seconds)

The Modern Java in the Wild rules require a demo video of **90–120 seconds**.
This script times out at roughly 112 seconds at a calm speaking pace.

**Record with the camera ON.** The whole point of this entry is that it watches
you and talks to you, so the camera must be running and your tracked skeleton
must be visible on your body. Let the app's own voice be audible in the take —
it is a feature, not background noise, and hearing it makes the case better than
describing it would.

| Time | Screen | Narration and actions |
| --- | --- | --- |
| 0:00–0:12 | You, in frame, app open | "You can't read a screen from inside a forward fold. And the apps that *can* see you usually ship your living room to somebody's cloud." Stand back so your whole body is in frame. |
| 0:12–0:28 | Calibration, camera on | Enable the camera. "This is Java 26 running a pose model on my own machine. Those are my joints — no account, no API key, nothing uploaded." Move an arm; let the skeleton track it. |
| 0:28–0:50 | Practice begins | Start **Steady Start**. Let the voice announce the pose. "It speaks every pose and every correction, so I never have to look at the screen." Move into the pose it names. |
| 0:50–1:10 | A measured pose | Reach a pose with knee rules — chair or a warrior. "It measures the angles it can actually see, and offers at most two cues." Let a cue be spoken. |
| 1:10–1:28 | Step out of frame | Deliberately step half out of shot. "When it can't see what a pose needs, it stops the clock and tells me exactly what it lost — instead of guessing." Point at the paused timer and the named body part, then step back and let it resume. |
| 1:28–1:52 | Close — code or architecture slide | "Java 26 does all of it: the Foreign Memory API maps the model, virtual threads carry capture and speech, and the analysis result is a sealed type where the unreliable case *cannot hold a correction*. Praising a pose it never measured isn't a bug I have to test for. It's a compile error." |

## Recording checklist

- Total length must land between 90 and 120 seconds — rehearse once with a timer.
- **Run `.\scripts\fetch-model.ps1` first.** Without the model the app runs its
  synthetic demonstration, which is honest but is not the story worth telling.
- Frame yourself so your whole body fits, including your feet. Feet below the
  bottom edge is the most common reason a standing pose will not measure.
- The step-out-of-frame moment is the strongest fifteen seconds in the video.
  Rehearse it: step out, wait for the timer to say paused and the cue to name
  the missing part, then step back and let it recover on its own.
- Let the spoken guidance be heard. Check the recording captures system audio,
  not only the microphone.
- **Delete all local history** genuinely deletes `~/.saha/sessions.json` —
  capture the progress-page screenshot before clicking it.
- 1080p screen capture, no personal files visible.
- No third-party music (contest rules prohibit unlicensed copyrighted material).

## If recording without a camera

The judge path still works: skip the model, and the app replays synthetic
landmarks through the identical analysis pipeline while labelling itself as
demonstration. The deliberate low-confidence dip fires about 30 seconds after
**Start Steady Start** and lasts about a second; wait for it rather than cutting
around it, and do not skip poses beforehand, because a skip's five-second
transition can mask it.

## Extended live-demo walkthrough (3–5 minutes, not for the video)

For judges or viewers who want the longer tour:

1. **Onboarding:** Saha asks only what shapes a practice, states the safety boundary, and stores no identity.
2. **Calibration:** Camera height, full-body framing, light, floor space — and the live landmark overlay confirming what the coach can see.
3. **Practice:** Start Steady Start. Pose name and instruction are spoken. Read the status, cue, and confidence readout, which shows the measured number against the gate.
4. **Honesty:** Step out of frame for the pause and its named cause. Note that poses without validated rules say "instruction only" rather than implying they were checked.
5. **Control:** Easier option, Repeat cue, Spoken guidance, Camera colour, and the persistent Stop now.
6. **Progress and privacy:** Derived metrics only, the explanation for every routine adjustment, then Delete all local history.
7. **Close:** Java 26 runs the interface, capture, inference, analysis, speech, persistence, and personalization.
