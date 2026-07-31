# Pose accuracy implementation plan

## Outcome

Replace the current synthetic landmark figure as the pose users copy. Saha will use two visibly different systems:

1. **Teaching illustration** — a reference-validated, hand-authored vector pose showing the intended setup.
2. **Camera analysis overlay** — detected landmarks showing only what the camera can observe, with confidence-aware measurements.

The teaching illustration is instructional. The camera overlay is observational. Saha must never imply that an automatically generated skeleton is an approved yoga demonstration.

## Contest-ready scope

The first validated set will contain six beginner-friendly movements:

| Pose | Required view | Main visual checks |
|---|---|---|
| Mountain | front | neutral stance, feet and face consistent, relaxed arms |
| Chair | side | hips back, knees bent, chest lifted, weight grounded through feet |
| Warrior I | three-quarter/side | clear front lunge, long rear leg, stable rear foot, lifted torso |
| Warrior II | side/three-quarter | front knee above ankle, long rear leg, arms level, gaze over front hand |
| Cat–Cow | side | wrists under shoulders, knees under hips, distinct rounded and extended spine states |
| Tree | front | standing leg stable, lifted foot below or above—not on—the knee |

Other catalog poses remain available only as written instruction until their illustrations and transitions pass the same review gate.

## Architecture changes

### 1. Separate illustration data from detected landmarks

Add an `illustration` package with these immutable Java records:

- `PoseIllustration`: pose ID, view direction, canvas bounds, body shapes, floor line, provenance notes, review state.
- `IllustrationFrame`: a complete hand-authored visual state.
- `TransitionSequence`: source pose, destination pose, ordered stages, cue for each stage, and duration.
- `ReviewState`: `DRAFT`, `REFERENCE_CHECKED`, `HUMAN_REVIEWED`, or `ENABLED`.

`DemoLandmarkSource` will remain a test/camera-free input for analysis. It will no longer supply the large example figure.

### 2. Use art-directed vector illustrations

Create each teaching pose as a deliberate JavaFX vector composition rather than passing approximate joint coordinates through the current `constrain()` method. Each composition will include:

- head silhouette and gaze direction;
- neck, ribcage, pelvis, arms, hands, legs, and feet;
- clear overlap/depth treatment for side and three-quarter views;
- a floor line and, when useful, a block or chair;
- optional alignment guides that can be toggled independently.

The figure should read clearly at normal laptop size and in a recorded contest demo. Decorative facial features are secondary to unambiguous head and gaze direction.

### 3. Add manually staged transitions

Direct interpolation between arbitrary poses will be removed from the teaching illustration. Each supported transition will use reviewed stages, for example:

```text
Mountain → Chair
stand tall → soften knees → send hips back → raise or lower arms

Chair → Warrior I
rise to standing → hands to hips → step one foot back → bend front knee → lift arms
```

The UI will show the stage cue, animate only between adjacent approved stages, and pause hold timing and alignment feedback until the destination stage is complete. Unsupported pose pairs will use a safe reset through Mountain, Tabletop, or Final Rest instead of inventing a movement path.

### 4. Add an accuracy gate

The coaching screen may render an illustration only when its state is `ENABLED`. Otherwise it displays written instructions and the message:

> Illustration under review. Follow the written setup or skip this pose.

Enabling a pose requires:

- at least two reputable visual/instructional references recorded in review metadata;
- agreement between the illustration and catalog instructions;
- pose-specific geometry tests;
- transition review from every routine neighbor;
- a manual visual review at 100%, 125%, and 150% Windows scaling;
- supportive modification shown alongside the full pose.

## Implementation slices

### Slice A — safety boundary and UI separation

1. Rename the existing large view to “Camera landmark demonstration.”
2. Add a separate teaching-illustration panel.
3. Disable the current generated figure as instruction.
4. Add `ReviewState` and prevent unapproved illustrations from rendering.
5. Keep the practice-path strip and indicate which entries have validated visuals.

**Acceptance:** No unreviewed synthetic pose can be mistaken for the example a user should copy.

### Slice B — shared visual language

1. Build reusable vector components for torso, pelvis, limbs, hands, feet, head, props, and floor.
2. Establish a single proportion guide, stroke system, depth treatment, and accessible palette.
3. Add a developer-only pose gallery showing every illustration at once.
4. Add snapshot/layout checks for clipping, missing parts, and inconsistent orientation.

**Acceptance:** The six figures have consistent anatomy while allowing pose-specific perspective and foreshortening.

### Slice C — validate Chair and Warrior poses first

1. Implement Chair from a side view with a visible hip hinge and grounded feet.
2. Implement Warrior I with a clear front lunge and distinct rear-foot placement.
3. Implement Warrior II with knee-over-ankle, straight rear leg, level arms, and gaze over the front hand.
4. Add optional guide overlays for the front shin, arm line, and floor contact.

**Acceptance:** A reviewer can identify each pose without reading its name, and geometry assertions match the documented visual checks.

### Slice D — Mountain, Cat–Cow, and Tree

1. Implement Mountain and Tree as stable front-view illustrations.
2. Split Cat–Cow into two distinct frames rather than using one oscillating landmark skeleton.
3. Animate Cat to Cow only through reviewed spine/head stages.

**Acceptance:** Face, feet, balance point, and support limbs are immediately understandable.

### Slice E — transition library

1. Define transitions used by the six-pose routine.
2. Add reset transitions for standing-to-floor and floor-to-standing changes.
3. Replace generic whole-body morphing with stage-by-stage cues.
4. Add Pause, Replay transition, Skip, Easier option, and Stop behavior tests.

**Acceptance:** No limb changes sides, crosses through the torso, changes length, or moves through a visibly impossible path.

### Slice F — camera analysis integration

1. Render detected camera landmarks with a different color and label from the teaching illustration.
2. Compare observable joint ranges with flexible tolerances, not pixel similarity to the illustration.
3. Suppress corrections when required landmarks or view orientation are unreliable.
4. State which camera view is required before each supported pose.

**Acceptance:** Users can always distinguish “example pose” from “camera sees,” and low confidence produces framing guidance rather than alignment correction.

## Test strategy

### Automated tests

- Illustration registry refuses `DRAFT` and `REFERENCE_CHECKED` assets in production coaching mode.
- Every enabled pose has a floor line, head, torso, both hands, and both feet unless its documented view legitimately occludes a part.
- Pose-specific relationships hold within ranges, such as Warrior II front knee above the front ankle.
- Transition sequences start and end on the exact registered pose frames.
- Adjacent transition frames preserve named body-part identity and remain within configured movement limits.
- Routine generation uses only poses whose instructional mode is enabled or explicitly written-only.
- Existing confidence, timing, persistence, and personalization tests continue to pass.

### Manual review sheet

For every pose and transition answer:

- Can the pose be recognized without its label?
- Are the front and rear limbs unmistakable?
- Do hands and feet show useful placement and direction?
- Does the head face where the instructions say to look?
- Is the support base physically plausible?
- Does the modification look easier rather than merely different?
- Would showing this to a beginner be responsible?

Any “no” returns the asset to `DRAFT`.

## Delivery order and estimates

| Milestone | Deliverable | Estimated focused work |
|---|---|---:|
| 1 | Safety boundary, data model, review gate | 1 day |
| 2 | Reusable vector body and pose gallery | 1–2 days |
| 3 | Chair, Warrior I, Warrior II | 2–3 days |
| 4 | Mountain, Cat–Cow, Tree | 2 days |
| 5 | Reviewed transitions and controls | 2–3 days |
| 6 | Camera overlay separation and full verification | 2 days |

This is approximately two focused development weeks, with visual review time included. Additional poses should be added one at a time after the six-pose path is reliable.

## Definition of done

- Six enabled, reference-reviewed teaching illustrations.
- A complete routine that uses only those six poses plus explicit transition stages.
- No generic landmark morph is presented as yoga instruction.
- Written and visual modifications for each supported pose.
- Clear separation between teaching art and observed camera landmarks.
- Java 26 build and all automated tests pass.
- A three-to-five-minute judge demo works without a camera.
- Documentation identifies references, completed content, review status, and remaining limitations without overstating safety or accuracy.
