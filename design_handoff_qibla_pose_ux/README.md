# Handoff: Qibla AR — Pose Guidance & Accuracy UX

## Overview

This bundle is a redesign of the **pose-guidance, accuracy-feedback, and pre-set-direction surfaces** of the QiblaAR Android app (`com.hashashino.qiblaar`). The goal is to give users clear, respectful, instruction-rich UI for:

- Holding the phone **flat** (compass screen) and **upright** (AR screen) with the right pitch.
- Understanding compass calibration state, magnetic-field interference, and GPS accuracy at a glance.
- Saving a known-good Qibla bearing while conditions are good, so they can pray accurately later in spots with no GPS or heavy magnetic interference (the "pre-set direction" / gyro-lock flow).

The design covers **13 screens across 5 sections**:
1. Compass · Pose & getting started (4 screens)
2. Compass · Interference & locked (3 screens)
3. AR view · Camera pose & sweep (5 screens)
4. Pre-set direction · 3-step flow (3 screens)
5. Calibration coachmark (1 screen)

---

## About the Design Files

The files in this bundle are **design references created as HTML + React JSX prototypes** — they exist purely to show the intended look, copy, animation, and behaviour. They are **not production code** and should not be shipped.

The implementation target is the **existing QiblaAR Android codebase** (Kotlin, View-based fragments — `CompassFragment`, `ArFragment`, `CalibrationFragment`, custom views like `CompassView`, `ArOverlayView`, `PhoneOrientationHintView`). The task is to **recreate these designs inside the existing Android fragments/views** — using Android resources (drawable XML, ConstraintLayout / Compose, Material Components, custom Views) — not to port the HTML/React.

Where the existing code already exposes the right data (`OrientationState.pitchDegrees`, `MagneticInterference.SEVERE`, `CompassAccuracy`, `isLocked`, `lockSource`), the UI just needs to be wired up to those flows.

## Fidelity

**High-fidelity.** Colors, typography, copy, spacing, and animation timing are intended to be implemented precisely. Treat exact hex values and pixel sizes as authoritative.

---

## Existing Code Context (read this first)

The Android app already has working sensor/state plumbing. The UI work is presentational — most of the inputs are already there.

### State flows the UI consumes

From `sensor/OrientationManager.kt`:

```kotlin
data class OrientationState(
    val azimuthDegrees: Float = 0f,            // 0..360, true-north
    val accuracy: CompassAccuracy = UNRELIABLE, // UNRELIABLE | LOW | HIGH
    val accuracyDegrees: Int = 45,              // typical accuracy band: 5, 10, 25, 45
    val magneticFieldMicroTesla: Float = 0f,    // raw µT
    val interference: MagneticInterference = NONE, // NONE | MILD | SEVERE
    val isLocked: Boolean = false,
    val lockSource: LockSource? = null,         // GPS | MANUAL
    val pitchDegrees: Float = 0f,               // physical phone tilt
    val isPoseValid: Boolean = true             // pose matches the screen's expected DevicePose
)

enum class DevicePose { FLAT, UPRIGHT }
```

### Pose thresholds (already enforced in OrientationManager)

| Screen   | Required pose | Threshold (existing code)              | UI behavior |
|----------|--------------|----------------------------------------|-------------|
| Compass  | `FLAT`       | `Math.abs(pitchDeg) < 70f`             | Block (full pose hint takeover) when invalid |
| AR       | `UPRIGHT`    | `pitchDeg < -20f` (tilted forward)     | Block (full pose hint takeover) when invalid |

The pose hint takeover should appear when `isPoseValid == false` and replace the main content.

### Magnetic interference thresholds (already classified in OrientationManager)

| µT magnitude | Class                          | UI behavior                                    |
|--------------|--------------------------------|------------------------------------------------|
| 25–65        | `NONE`                         | No banner                                      |
| 20–25 or 65–80 | `MILD`                       | Soft amber inline banner above the dial; keep showing the bearing |
| <20 or >80   | `SEVERE`                       | Hide bearing (blur), red modal card, force user to recalibrate or fall back to pre-set |

### Compass accuracy (already classified)

| Class        | Band  | UI label  |
|--------------|-------|-----------|
| `HIGH`       | ±5°–±10° | green chip `±5°` |
| `LOW`        | ±25°  | amber chip `±18°` (or actual band) |
| `UNRELIABLE` | ±45°  | red chip `Inaccurate` — trigger calibration coachmark |

### Lock semantics (already implemented)

`OrientationManager.lock(source)` captures the current GPS-derived azimuth into a gyro-only anchor. Once locked:
- Bearing is driven by `TYPE_GAME_ROTATION_VECTOR` (gyro + accel, **no magnetometer**), so magnetic noise at the prayer spot doesn't affect it.
- `state.isLocked == true` and `state.lockSource == GPS` (or `MANUAL`).

This is what the design calls "**pre-set direction**". The user-facing language is "pre-set" / "save" / "anchor" — **not** "lock". The word "lock" is reserved for internals.

---

## Visual Foundations

### Color Tokens

The app's existing palette (`app/src/main/res/values/colors.xml`) was extended slightly for the design. Source of truth: `tokens.css`.

| Token             | Hex / Value                  | Existing colors.xml equivalent | Usage |
|-------------------|------------------------------|--------------------------------|-------|
| `--bg-app`        | `#14152A`                    | `background_dark` `#1A1A2E`    | Compass screen / app bg |
| `--bg-camera`     | `#0A0A14`                    | new                            | AR camera fallback |
| `--bg-surface`    | `#1B1D38`                    | `surface_dark` `#16213E`       | Cards, nav bar |
| `--bg-elevated`   | `#232548`                    | new                            | Hover / pressed surfaces |
| `--hairline`      | `rgba(255,255,255,0.08)`     | new                            | Card borders, dividers |
| `--text`          | `#FFFFFF`                    | `white`                        | Headings |
| `--text-2`        | `rgba(255,255,255,0.78)`     | new                            | Body copy |
| `--text-3`        | `rgba(255,255,255,0.56)`     | `gray` `#AAAAAA`               | Meta / labels |
| `--text-4`        | `rgba(255,255,255,0.40)`     | new                            | Disabled / tertiary |
| `--green`         | `#22c55e`                    | `accent_green` `#4CAF50`       | Primary action, ALIGNED state, pre-set badge |
| `--green-soft`    | `rgba(34,197,94,0.16)`       | new                            | Green chip / banner bg |
| `--green-line`    | `rgba(34,197,94,0.35)`       | new                            | Green chip / banner border |
| `--amber`         | `#F59E0B`                    | `accent_orange` `#FF9800`      | Mild interference, low GPS, pose-mismatch warnings |
| `--amber-soft`    | `rgba(245,158,11,0.14)`      | new                            | Amber banner bg |
| `--amber-line`    | `rgba(245,158,11,0.40)`      | new                            | Amber banner border |
| `--red`           | `#EF4444`                    | `accent_red` `#F44336`         | Severe interference, danger, N indicator |
| `--red-soft`      | `rgba(239,68,68,0.14)`       | new                            | Red banner bg |
| `--red-line`      | `rgba(239,68,68,0.40)`       | new                            | Red banner border |
| `--kaaba`         | `#d4a35e`                    | new                            | Reserved for warm Kaaba imagery (not used in current screens — Kaaba glyph uses green when correct) |

Implementation note: the chip system has three tones (green / amber / red) plus neutral. Pattern is `bg + 1px border + lighter text color (e.g. `#86EFAC` on `--green-soft`)`.

### Typography

**Single family: Plus Jakarta Sans** (Google Fonts, weights 400 / 500 / 600 / 700 / 800). Replace any existing Roboto/system-font usage with this.

Inter is the design-system fallback but is **not used** here — every text element is Plus Jakarta Sans.

| Role                        | Weight | Size  | Notes |
|-----------------------------|--------|-------|-------|
| Hero / step title           | 800    | 24–28 | letter-spacing -0.01em |
| Section title (in-screen)   | 800    | 20–22 | |
| Bearing readout (e.g. `295°`) | 800  | 38    | tabular-nums, letter-spacing -0.02em |
| Body / paragraph            | 500/600| 13–14 | line-height 1.5 |
| Header microcopy            | 700    | 11    | uppercase, letter-spacing 0.5–0.8 |
| Chip / pill label           | 700    | 10.5–12| letter-spacing 0.2–0.4 |
| Button label                | 700    | 14    | |
| Numeric tabular             | use `font-variant-numeric: tabular-nums` everywhere a bearing/distance changes live |

### Spacing & Layout

- Phone canvas: 412 × 880 (matches modern Android viewport).
- Side padding inside screens: 16–20px.
- Card padding: 14–20px.
- Vertical rhythm: 8 / 12 / 16 / 22 / 24 / 32 dp between groups.

### Radii

| Element | Radius |
|---------|--------|
| Chips / pills | 999 (full) |
| Buttons | 12 |
| Small cards / banners | 12–14 |
| Hero cards / frosted modals | 18–22 |
| Avatar / icon circles | 999 |

### Shadows

Surfaces use 1px borders (`--hairline`) over flat dark backgrounds — no drop shadows on UI cards. The phone bezel in the mock uses `0 50px 100px -30px rgba(0,0,0,0.6)` for presentation only; ignore in implementation.

### Iconography

Lucide-style 2px stroke icons (matches the design system's aspirational direction). Exact set used in screens:
- `lock` / `unlock` (open + closed padlock)
- `compass`, `map-pin` / `gps`, `camera`, `magnet`
- `warn` (triangle), `info` (circle-i), `check`, `x`, `chevron-right`, `arrow-up`, `rotate-ccw`, `sparkles`
- Custom **Kaaba glyph**: a 24×24 rectangle with a horizontal line near the top and two vertical interior lines (mihrab cube stylization). See `phone-frame.jsx` `Icon.Kaaba` and `ar-screens.jsx` `KaabaMarker` for the exact path.

The existing `res/drawable/ic_compass.xml`, `ic_camera.xml`, `ic_calibrate.xml`, `ic_figure8.xml` should be updated or replaced with the Lucide equivalents for consistency.

### Motion

| Animation | Duration | Easing | Notes |
|-----------|----------|--------|-------|
| Pose hint loop (flat & upright) | 3.6s | `cubic-bezier(.55,.05,.25,1)` | Phone rotates from "wrong" to "correct" pose, lingers at correct, snaps back. See `pose-illustrations.jsx` keyframes. |
| Figure-8 calibration | matches sensor `accuracy` events | smooth | Progress bar from 0–100%. |
| Sweep arrow (AR searching) | none — purely position | — | Arrow position derived from `qibla - azimuth` delta. |
| Card hover / fade ins | 250ms ease-out | — | |

---

## Screen-by-Screen Spec

Each screen is a `<DCArtboard>` in `Qibla AR Pose UX.html`. Open that file in a browser to interact.

### Section 1 — Compass · Pose & Getting Started

All compass screens share `CompassShell` (`compass-screens.jsx`):
- Header row (top, 20px padding): left = location label ("QIBLA COMPASS" eyebrow + city), right = accuracy chip (green/amber/red).
- Body (centered column).
- Bottom nav: 3 tabs — Compass · AR View · Calibrate — active tab in `--green`, others in `--text-4`.

#### 1.1 Finding location (`CompassFindingLocation`)
- **When:** App opened, `Location` not yet acquired or `accuracy > 50m`.
- **Behaviour:** Compass disk drawn dimmed (opacity 0.5, `accuracy="unreliable"`). Center label is a 44×44 amber-tinted GPS icon + "SEARCHING" microcopy. Below: 18px bold "Finding your location" + 13px muted "Step outside or near a window…" copy.
- **Action:** Link button at the bottom: "Use pre-set direction instead" (opens the pre-set flow). Only shown if user has a saved direction.

#### 1.2 Phone tilted — blocking pose hint (`CompassPoseHint`)
- **When:** `pose != FLAT` (i.e. `|pitch| >= 15°`) — note the design tightens the threshold from the existing 70° to 15° for nudging; the existing 70° threshold can remain as the hard block but UI should nudge starting around 15°.
- **Behaviour:** Compass replaced entirely by the **`PhoneFlatIllustration`** animation (`pose-illustrations.jsx`). 280×200px SVG with: a table parallelogram at the bottom labelled "TABLE · LEVEL", phone starts standing upright on its left edge, rotates clockwise down to lie flat; bubble level pip centers; mini-compass disk fades in above; soft green wash.
- **Copy:** "Lay phone flat" (22px / 800), then "Place your phone face-up on a flat surface — like a table or floor — so the compass can read true heading."
- **Live tilt indicator:** small amber-bordered card: "Tilted **32°** — needs to be under 15°" (use real `pitchDegrees`).
- **Action:** Link "Why does this matter?" → opens a sheet explaining sensor fusion.

#### 1.3 Compass uncalibrated (`CompassCalibrating`)
- **When:** `accuracy == UNRELIABLE` AND user hasn't deferred. Auto-route from a banner on the main compass screen.
- **Behaviour:** Figure-8 SVG illustration with phone token. Progress bar 0–100% (driven by `onAccuracyChanged` events). Subtext "About 5 seconds." ETA text.
- **Action:** "Skip for now" link.
- See also `CalibrationScreen` (Section 5) — that's the standalone "Calibrate" tab; this is the inline mid-flow version.

#### 1.4 Ready — accurate (`CompassReady`)
- **When:** `accuracy == HIGH`, `pose == FLAT`, `interference == NONE`, GPS good.
- **Behaviour:** Full compass disk (`CompassDisk` from `compass-disk.jsx`) at 282px. Bearing readout below shows `295° true north` + Kaaba glyph + `6,420 km away`. Signal trio row shows three stat cards: GPS `±3m`, Compass `±5°`, Magnetic `48µT` — all green.
- **Actions:** Primary outline button "Pre-set direction" + square icon button for "Recalibrate" (rotate-ccw).

### Section 2 — Compass · Interference & Locked

#### 2.1 Mild interference (nudge) (`CompassInterferenceMild`)
- **When:** `interference == MILD`.
- **Behaviour:** Amber inline banner at top: "**Nearby metal detected.** Bearing may be off by ±18°. Step away from devices, table legs, or rebar." Compass disk still shown, `accuracy="low"` desaturates the Qibla pointer to gray. Bearing readout still visible. Below the disk, a magnetic-field stat row: `Magnetic field · 78µT (normal 25–65)`.

#### 2.2 Severe interference (block) (`CompassInterferenceSevere`)
- **When:** `interference == SEVERE`.
- **Behaviour:** Compass disk blurred (`filter: blur(4px) saturate(0.4)`) and dimmed. Overlaid centered: 84×84 red-tinted X icon. Below: red banner card titled "**Reading not trustworthy**" with body "Magnetic field is **142µT** — way above normal. You're likely near a speaker, laptop, MRI, or steel beam. Move 2 meters away and recalibrate."
- **Actions:** Primary green "Recalibrate now" (sparkles icon) + ghost "Use pre-set direction instead".

#### 2.3 Locked / using pre-set (`CompassLocked`)
- **When:** `isLocked == true`.
- **Behaviour:** Header location label is replaced with "Pre-set · Saved at home" (or saved label). Top banner (green): "Using your **saved direction**. GPS-free, immune to local interference." + "2h ago" timestamp. Compass disk shown with `locked=true` — that adds a small "PRE-SET DIRECTION" pill below center. Bearing readout uses the saved value.
- **Actions:** "Use live compass" (unlock icon, quiet button) + "Re-anchor" (ghost — recapture with current sensors).

### Section 3 — AR view · Camera pose & sweep

All AR screens render over `CameraFeed` (`ar-screens.jsx`) — in production this is the live camera preview; the mock draws a stylized interior. `ArHeader` shows mode chip (left: `AR · live` / `AR · pre-set`) and status chip (right: tone-coded label like `ALIGNED · ±5°`, `47° OFF`, `WRONG POSE`).

#### 3.1 Phone too flat — blocking pose hint (`ArPoseHint`)
- **When:** `pose != UPRIGHT` (existing threshold: `pitchDeg >= -20f` — phone not tilted forward enough).
- **Behaviour:** Frosted dark card covers the top 60% of screen with the **`PhoneUprightIllustration`** animation. Phone starts horizontal in hand (screen up — the classic "compass pose"), rotates counter-clockwise up to vertical facing wall on the right. Green dashed camera ray casts to Kaaba marker on the wall once upright. Hand grip stays static at the bottom.
- **Copy:** "Hold phone upright" (20px / 800), then "Point the camera at the wall in front of you, like you're taking a portrait photo. The Qibla marker appears where the Kaaba lies through the wall."
- **Live tilt pill:** amber chip "Currently tilted **-68°** — needs near vertical" (use real `pitchDegrees`).
- **Action:** Quiet button at the bottom: "Use compass mode instead".

#### 3.2 Searching — sweep right (`ArSearching`)
- **When:** `pose == UPRIGHT`, but `|qibla - azimuth| > 10°`.
- **Behaviour:** No Kaaba marker yet. Side hint pinned to the offscreen direction (right or left) using `SweepArrow`: green gradient arrow + chip "Turn right → 47°" (degrees = signed delta). Thin glowing edge bar on the same side as a peripheral cue. Bottom heading card: "FACING 248°" → chevron → "KAABA 295°".
- **Copy:** Footer microcopy: "Slowly sweep the phone right →".

#### 3.3 Aligned (`ArAligned`)
- **When:** `|qibla - azimuth| <= 5°` (within accuracy band).
- **Behaviour:** `KaabaMarker` shown centered at 50%/38% with `aligned=true` — green crosshair brackets + green Kaaba glyph + green-tinted "ﺍﻟْﻜَﻌْﺒَﺔُ · 6,420 km" pill below. Green toast at the bottom: check icon + "**Pointing at the Kaaba**" + "Lay your prayer mat parallel to this line".
- **Action:** Primary green button "Save this direction" (lock icon) — captures gyro lock at current bearing.

#### 3.4 Magnetic interference in AR (`ArInterference`)
- **When:** `pose == UPRIGHT` AND `interference == SEVERE`.
- **Behaviour:** Kaaba marker rendered blurred + 60% opacity (visibly drifting). Top of screen: status chip red `COMPASS DISTURBED`. Mid-card: red-bordered frosted card with magnet icon, title "**The AR marker is drifting**", body "We detected **142 µT** nearby — likely speakers, laptop, or a metal door frame. Don't trust this bearing." Inline tip with lock icon: "Switch to your pre-set direction. Gyro-only tracking ignores magnetic noise."
- **Action:** Primary "Switch to pre-set" (lock icon).

#### 3.5 Pre-set in AR (`ArLocked`)
- **When:** `pose == UPRIGHT` AND `isLocked == true`.
- **Behaviour:** Camera live, but tracking is gyro-only. Mode chip in header reads `AR · pre-set` (instead of `live`). Kaaba marker shown aligned. Bottom card (frosted, green-bordered): lock icon + "**Pre-set direction · gyro lock**" + "Saved at home · won't drift from metal here" + inline "Unlock" text button.

### Section 4 — Pre-set direction · 3-step flow

Standalone flow reached from any "Pre-set direction" button. Files: `flow-screens.jsx`.

#### 4.1 Why pre-set (`LockStep1`)
- Hero with 64×64 lock-icon tile, then 28/800 title "Save your Qibla direction for later" + body explaining the value. Three `Benefit` rows (GPS-free / immune to metal / quick to recall). Primary "Continue" + link "Maybe later".

#### 4.2 Choose method (`LockStep2`)
- Step indicator "STEP 2 OF 3".
- **Option A (recommended):** green-bordered card "Capture current direction". Live readiness preview: three tags reflecting current state — `GPS ±3m`, `Compass ±5°`, `Magnetic OK`. Primary action "Lay phone flat & tap to capture".
- **Option B:** neutral card "Set manually" — for use when user can see a known reference (mihrab, prayer rug). Tap → enters a manual dial UI (not detailed in this design; recommend a 360° rotating dial with snap-to-degree).
- Bottom info card explaining gyro anchoring.

#### 4.3 Saved confirmation (`LockStep3`)
- Big 120×120 green check badge with radial glow. Title "Direction saved". Subtitle about gyro tracking.
- Saved-bearing card: lock-tile + "SAVED BEARING `295° NW`" on left, "FROM `Tampines, SG`" on right. Below: "Distance to Kaaba `6,420 km`" / "Captured `Just now · ±5°`".
- Actions: Primary "Open in AR view" + ghost "Back to compass".

### Section 5 — Calibration coachmark (`CalibrationScreen`)

- Eyebrow: "STEP 1 OF 2 · COMPASS" + amber chip "Needs calibration".
- Title "Trace a figure-8" + body explanation.
- Big animated figure-8 SVG with dashed guide + green/amber progress gradient + glowing moving dot following the parametric path (Lissajous-style `x = A sin(t), y = B sin(2t)`).
- Progress bar (driven by `onAccuracyChanged`) + label "Strength medium · ~3 seconds left".
- Tips card with 3 lucide-icon rows: avoid metal, twist not slide, repeat after jumps.

---

## State Machine Summary

Single top-level state derived from `OrientationState`:

```
priority(highest → lowest):
  1. !isPoseValid                 → POSE_HINT (CompassPoseHint or ArPoseHint)
  2. interference == SEVERE       → INTERFERENCE_SEVERE
  3. accuracy == UNRELIABLE       → CALIBRATION
  4. GPS not acquired (Compass)   → FINDING_LOCATION
  5. isLocked                     → LOCKED (CompassLocked / ArLocked)
  6. AR mode + |azimuth-qibla|>5° → SEARCHING
  7. interference == MILD         → INTERFERENCE_MILD overlay over READY
  8. else                         → READY (CompassReady / ArAligned)
```

In Kotlin Flow this is a single `combine(...)` collapsing `OrientationState` + GPS state → a sealed `CompassUiState` / `ArUiState`. The fragment renders one of the states above.

---

## Interactions

| Trigger | Action |
|---------|--------|
| Tap "Pre-set direction" / "Save this direction" | Push `LockFlow` activity/dialog → step 1 |
| In Step 2, tap "Capture" | `orientationManager.lock(LockSource.GPS)` after verifying `isPoseValid && interference == NONE && accuracy == HIGH`. On failure, show inline error. |
| In Step 2, tap "Set manually" | Navigate to manual-dial sub-screen (not in this design) |
| In a LOCKED state, tap "Use live compass" / "Unlock" | `orientationManager.unlock()` |
| In LOCKED, tap "Re-anchor" | `orientationManager.unlock()` then re-route to capture (LockStep2 option A) |
| Accuracy chip in any header | Tap → navigate to Calibrate tab |
| Severe interference modal, tap "Recalibrate now" | Navigate to Calibrate tab |
| Severe interference modal, tap "Use pre-set instead" | If saved exists → switch to locked; else → LockStep1 |
| AR aligned, tap "Save this direction" | `orientationManager.lock(LockSource.GPS)` directly (no flow — single tap), show 1s success toast then dismiss |

---

## Copy Reference (final, use verbatim)

- Pose hint headings: "Lay phone flat" / "Hold phone upright"
- Pose hint bodies:
  - "Place your phone face-up on a flat surface — like a table or floor — so the compass can read true heading."
  - "Point the camera at the wall in front of you, like you're taking a portrait photo. The Qibla marker appears where the Kaaba lies through the wall."
- Severe interference: "Reading not trustworthy" / "Magnetic field is X µT — way above normal. You're likely near a speaker, laptop, MRI, or steel beam. Move 2 meters away and recalibrate."
- Mild interference: "Nearby metal detected. Bearing may be off by ±N°. Step away from devices, table legs, or rebar."
- Aligned toast: "Pointing at the Kaaba" / "Lay your prayer mat parallel to this line"
- Pre-set step 1 hero: "Save your Qibla direction for later"
- Pre-set step 1 sub: "Pre-set the bearing while you have good GPS — then pray anywhere, even with no signal or magnetic interference."
- Pre-set step 3 hero: "Direction saved"
- Sacred string: ﻿ﺍﻟْﻜَﻌْﺒَﺔُ · {distance}  ← use Naskh-compatible font fallback for the Arabic; on Android `serif` or the bundled Noto Sans Arabic will render correctly.

Tone is **instructional and respectful** — explain the *why*, never gamify. Singaporean English. No emoji.

---

## Files in This Bundle

| File | Role |
|------|------|
| `Qibla AR Pose UX.html` | Entry — open in a browser. Renders all 13 screens in a pannable design canvas. |
| `tokens.css` | All color / type / radius / spacing tokens as CSS custom properties — source of truth. |
| `design-canvas.jsx` | Pan/zoom canvas component (presentation only). |
| `phone-frame.jsx` | Dark Android phone shell + shared atoms: `Chip`, `Dot`, `BottomNav`, `Icon` (Lucide set). |
| `compass-disk.jsx` | The compass dial SVG (`CompassDisk`) + bearing readout (`BearingReadout`). Reproduce in `CompassView.kt`. |
| `compass-screens.jsx` | All 7 compass states. Maps 1:1 to `CompassUiState`. |
| `ar-screens.jsx` | All 5 AR states. Includes `CameraFeed` (mock), `ArHeader`, `KaabaMarker`, `SweepArrow`, `ArBottomBar`. |
| `flow-screens.jsx` | Pre-set 3-step flow + standalone calibration screen. |
| `pose-illustrations.jsx` | The two animated pose illustrations — these are the trickiest pieces; the SVG keyframes show exact timing and pivot points. |

---

## Implementation Notes

- **Compass disk**: existing `CompassView.kt` is close but needs: green Qibla pointer with Kaaba glyph at the tip (currently a small green dot), white North needle (currently red), thinner degree ticks, radial gradient fill on the disk. See `compass-disk.jsx` for exact tick / gradient values.
- **Pose hint view**: existing `PhoneOrientationHintView.kt` should be replaced. The new design is significantly more communicative — the table/wall references, hand grip, mini compass payoff, and camera ray are all new and important. Reimplement as a Kotlin `View` with `Canvas` drawing + `ValueAnimator`, or migrate the fragment to Jetpack Compose and use `Canvas { }` with `animateFloatAsState`. Keyframe values to mirror:
  - Cycle: 3600ms, `FastOutSlowInInterpolator` equivalent.
  - Flat: rotation `-90° → 0°` of phone group, pivoted at the bottom-LEFT corner of the flat-position phone (94, 152 in 280×200 viewport).
  - Upright: rotation `90° → 0°` pivoted at hand position (120, 165).
  - Linger 14% → 82% on the correct pose. Quick reset on the back-half.
- **Magnetic field display**: thresholds match existing code. Format µT with no decimals.
- **AR camera overlay**: the existing `ArOverlayView.kt` likely just draws the arrow. Extend it to also draw the corner brackets (KaabaMarker), the green sweep arrow when off-axis, and the distance pill.
- **Status bar / nav**: respect the user's system theme — these designs are dark-only because the app already uses `background_dark`. Don't add a light variant.

---

## Out of Scope (Ask if needed)

- Onboarding (first-launch permission grants for camera + location)
- Manual dial UI (the "Set manually" branch of LockStep2 — needs its own design)
- Settings / preferences
- Saved-direction management (multiple saved locations, edit, delete)
- Prayer-time integration
- Tablet / large-screen layout
