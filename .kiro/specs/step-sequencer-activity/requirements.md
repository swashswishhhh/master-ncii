# Requirements Document

## Introduction

StepSequencerActivity is a drag-to-reorder interactive mission for the Server Master NC II cyberpunk quiz game. The player is presented with six shuffled RDP configuration steps and must drag them into the correct sequence before tapping VERIFY. The activity integrates with the existing `simulatorLauncher` / `interactiveLauncher` result-contract pattern used by `MainActivity` and `QuizActivity`, and is scoped to level 2.10 — Remote Desktop / RDP Configuration.

## Glossary

- **StepSequencerActivity**: The host Android Activity that owns the drag-to-reorder mission lifecycle.
- **StepItem**: Immutable data model representing one draggable step card, carrying an `originalIndex` (0–5) and `stepText`.
- **StepAdapter**: `RecyclerView.Adapter` that renders `StepItem` objects as cyberpunk `MaterialCardView` rows and exposes a drag-start hook.
- **StepDragCallback**: `ItemTouchHelper.Callback` subclass that enables vertical drag-reorder and disables swipe-to-dismiss.
- **CorrectOrder**: The canonical sequence `[0, 1, 2, 3, 4, 5]` representing the six RDP configuration steps in the correct order.
- **VERIFY**: The `MaterialButton` the player taps to submit the current arrangement for validation.
- **SuccessOverlay**: The full-screen `FrameLayout` shown when the player submits the correct sequence.
- **simulatorLauncher**: The `ActivityResultLauncher` in `MainActivity` that handles result callbacks from simulator activities.
- **interactiveLauncher**: The `ActivityResultLauncher` in `QuizActivity` that handles result callbacks from interactive activities.
- **extra_task_completed**: Boolean Intent extra set to `true` in the result when the player completes the mission successfully.
- **DragHandle**: The `ImageView` on each step card that the player long-presses to initiate a drag.
- **ErrorHighlight**: The red stroke and red background tint applied to step cards that are in the wrong position after a failed VERIFY.

---

## Requirements

### Requirement 1: Activity Launch and Initial State

**User Story:** As a player, I want the step sequencer activity to launch with a shuffled set of RDP configuration steps, so that I have a meaningful challenge to solve.

#### Acceptance Criteria

1. WHEN `StepSequencerActivity` is launched via an `Intent`, THE `StepSequencerActivity` SHALL display a `RecyclerView` containing exactly 6 step cards.
2. WHEN `StepSequencerActivity` is launched, THE `StepSequencerActivity` SHALL present the 6 step cards in a shuffled order that differs from the correct sequence.
3. WHEN the shuffle produces the correct order by chance, THE `StepSequencerActivity` SHALL re-shuffle until the displayed order differs from `CorrectOrder`.
4. THE `StepSequencerActivity` SHALL display a VERIFY button that is always enabled regardless of the current card arrangement.
5. THE `StepSequencerActivity` SHALL display the title "ARRANGE THE RDP CONFIGURATION STEPS IN THE CORRECT ORDER" at the top of the screen.

---

### Requirement 2: Drag-to-Reorder Interaction

**User Story:** As a player, I want to drag step cards up and down to reorder them, so that I can arrange the steps into what I believe is the correct sequence.

#### Acceptance Criteria

1. WHEN a player long-presses the `DragHandle` on a step card, THE `StepSequencerActivity` SHALL initiate a drag operation for that card.
2. WHEN a drag operation is in progress, THE `StepAdapter` SHALL swap the dragged card with the card at the target position and update the `RecyclerView` display.
3. WHEN a card is dragged to a new position, THE `StepAdapter` SHALL clear all `ErrorHighlight` states from all cards.
4. THE `StepDragCallback` SHALL permit drag movement in the vertical direction (up and down) only.
5. THE `StepDragCallback` SHALL not permit swipe-to-dismiss on any card.
6. THE `StepDragCallback` SHALL not enable long-press drag on the card body; drag SHALL only be initiated via the `DragHandle`.

---

### Requirement 3: Sequence Validation

**User Story:** As a player, I want to tap VERIFY to check whether my arrangement is correct, so that I receive immediate feedback on my answer.

#### Acceptance Criteria

1. WHEN the player taps VERIFY and the current card arrangement matches `CorrectOrder`, THE `StepSequencerActivity` SHALL display the `SuccessOverlay`.
2. WHEN the player taps VERIFY and the current card arrangement matches `CorrectOrder`, THE `StepSequencerActivity` SHALL clear all `ErrorHighlight` states before showing the `SuccessOverlay`.
3. WHEN the player taps VERIFY and one or more cards are out of position, THE `StepAdapter` SHALL apply `ErrorHighlight` (red stroke and red background tint) to each card that is in the wrong position.
4. WHEN the player taps VERIFY and one or more cards are out of position, THE `StepSequencerActivity` SHALL display a Snackbar with the message "RECONFIGURE — X STEPS OUT OF SEQUENCE" where X is the count of misplaced cards.
5. WHEN the player taps VERIFY and one or more cards are out of position, THE `StepSequencerActivity` SHALL increment the attempt counter.
6. THE `StepSequencerActivity` SHALL allow the player to re-submit after a failed VERIFY with no limit on the number of attempts.

---

### Requirement 4: Success Overlay and Mission Completion

**User Story:** As a player, I want to see a success screen after submitting the correct sequence, so that I know I have completed the mission and can return to the map.

#### Acceptance Criteria

1. WHEN the `SuccessOverlay` is shown, THE `StepSequencerActivity` SHALL animate the overlay container from alpha 0 to alpha 1 over 400 ms.
2. WHEN the `SuccessOverlay` is shown, THE `StepSequencerActivity` SHALL animate the "SEQUENCE CONFIRMED" text with a scale-up overshoot effect over 600 ms.
3. WHEN the `SuccessOverlay` is shown, THE `StepSequencerActivity` SHALL reveal the CONTINUE button after an 800 ms delay.
4. WHEN the player taps CONTINUE on the `SuccessOverlay`, THE `StepSequencerActivity` SHALL call `setResult(RESULT_OK)` with `extra_task_completed` set to `true` and then finish.
5. WHEN `StepSequencerActivity` finishes with `RESULT_OK` and `extra_task_completed = true`, THE `simulatorLauncher` in `MainActivity` SHALL record a score of 1 out of 1 for level 2.10 and unlock the next level.

---

### Requirement 5: Error Feedback and Recovery

**User Story:** As a player, I want clear visual feedback when my arrangement is wrong and the ability to correct it, so that I can keep trying until I get the right sequence.

#### Acceptance Criteria

1. WHEN `ErrorHighlight` is applied to a card, THE `StepAdapter` SHALL render that card with a red stroke color (`#F44336`) and a red background tint.
2. WHEN the player performs any drag move after a failed VERIFY, THE `StepAdapter` SHALL clear all `ErrorHighlight` states from all cards.
3. IF the player presses the system back button without completing the mission, THEN THE `StepSequencerActivity` SHALL return `RESULT_CANCELED` with no `extra_task_completed` extra.
4. WHEN `StepSequencerActivity` returns `RESULT_CANCELED`, THE `simulatorLauncher` in `MainActivity` SHALL not record any score or unlock for level 2.10.

---

### Requirement 6: Visual Design and Theme

**User Story:** As a player, I want the step sequencer to match the cyberpunk aesthetic of the rest of the game, so that the experience feels consistent and immersive.

#### Acceptance Criteria

1. THE `StepSequencerActivity` SHALL use a background color of `#0A0F0A` for the main screen.
2. THE `StepAdapter` SHALL render each step card using `MaterialCardView` with background color `#111620`, neon green stroke (`#39FF7F`), and 8 dp corner radius.
3. THE `StepSequencerActivity` SHALL display all text using a monospace font.
4. THE `StepAdapter` SHALL display a step number badge (1–6) on the left side of each card and a `DragHandle` icon on the right side, both tinted neon green.
5. THE VERIFY button SHALL use a neon green background with dark text and a monospace font.

---

### Requirement 7: StepItem Data Model

**User Story:** As a developer, I want a well-defined data model for each step card, so that the adapter and activity can reliably exchange step data.

#### Acceptance Criteria

1. THE `StepItem` SHALL store an `originalIndex` in the range [0, 5] representing the step's position in `CorrectOrder`.
2. THE `StepItem` SHALL store a non-null, non-empty `stepText` string containing the instruction shown on the card.
3. THE `StepItem` SHALL expose an `errorHighlighted` boolean field that defaults to `false` on construction.
4. THE `StepItem` SHALL provide getter and setter methods for all fields.

---

### Requirement 8: Integration with MainActivity (Level 2.10)

**User Story:** As a developer, I want `StepSequencerActivity` wired into the existing level routing system, so that level 2.10 launches the correct interactive mission.

#### Acceptance Criteria

1. WHEN a player taps level 2.10 in `MainActivity` and the level is unlocked, THE `MainActivity` SHALL launch `StepSequencerActivity` via `simulatorLauncher`.
2. THE `AndroidManifest.xml` SHALL declare `StepSequencerActivity` with `screenOrientation="portrait"` and `exported="false"`.
3. WHEN `QuizActivity` calls `getSimulatorForLevel("2.10")`, THE `QuizActivity` SHALL return `StepSequencerActivity.class`.

---

### Requirement 9: Correct RDP Configuration Sequence

**User Story:** As a learner, I want the correct sequence to reflect the actual RDP configuration procedure, so that completing the mission reinforces accurate technical knowledge.

#### Acceptance Criteria

1. THE `StepSequencerActivity` SHALL define the correct sequence as exactly these six steps in this order:
   1. Open Server Manager
   2. Add Remote Desktop Services Role
   3. Configure Network Level Authentication (NLA)
   4. Open Windows Firewall and allow port 3389
   5. Add users to Remote Desktop Users group
   6. Test connection using mstsc.exe
2. WHEN validating the player's arrangement, THE `StepSequencerActivity` SHALL compare each card's `originalIndex` against `CorrectOrder` `[0, 1, 2, 3, 4, 5]` positionally.
