# Decision policy notes

This file documents the current policy boundary, not a promise of a particular live score. The remote game has hidden and changing mechanics, so score experiments should not be treated as deterministic acceptance tests.

## Single source of truth

`backend/src/main/java/com/mugloar/application/DecisionEngine.java` is the only place that chooses a recommended action. The Angular client renders its recommendation and `POST /api/games/{gameId}/auto/step` executes that same action. This prevents manual guidance and automation from drifting apart.

## Conservative mode

The baseline policy is intentionally explainable. It evaluates actions in this exact priority order:

1. If the dragon has fewer than three lives and at least 50 gold, buy a healing potion (`hpot`).
2. If at least 100 gold remains, buy the least-purchased starter upgrade from `cs`, `gas`, `wax`, `tricks`, and `wingpot`. This distributes upgrades evenly.
3. Otherwise, consider missions rated `Quite likely` or safer. Choose by safety first, then reward, then the sooner expiry.
4. If no accepted mission exists, investigate (refresh) the board up to three consecutive times without risking a life.
5. Once that refresh limit is reached, take the safest mission left on the board. Stop only when no advertisement exists.

The utility values establish the same priority for display and diagnostics: healing is 10,000, an upgrade is 9,000, and investigation is 1,000. A mission's utility is its safety rank multiplied by 1,000, plus reward multiplied by 10, with a small penalty when it is close to expiring.

The deterministic `GameServiceTest` validates that this automation reaches the assignment's 1,000-point target against a successful game port.

**Live verification:** running `CONSERVATIVE` automation end to end against the real `https://dragonsofmugloar.com` API reached a score of 3,305 (over 3x the 1,000-point target) across 97 turns before the run ended on lost lives. Live scores vary between runs since the remote game's mechanics are not deterministic; this run is evidence the policy clears the target in practice, not a guaranteed result.

## High-risk mode

High-risk mode is a deliberately isolated experimental policy. It evaluates actions in this exact priority order:

1. With two lives or fewer and at least 50 gold, buy a healing potion. This always comes first, even
   mid-streak — staying alive is non-negotiable. Earlier revisions only healed at one life and instead
   bought a cheap upgrade at exactly two lives — that starved gold that should have gone to healing and
   left the dragon stuck at two lives, never affording a premium upgrade again.
2. Otherwise, while every mission on the board is rated `Sure thing`, keep solving the best-paying one
   instead of shopping. A late-game `Sure thing` can be worth far more than any upgrade costs, so
   pausing to buy one is a wasted turn.
3. Otherwise (the board is no longer all `Sure thing`), invest in up to two starter upgrades total (from
   `cs`, `gas`, `wax`, `tricks`, `wingpot`, least-purchased first) whenever one is affordable. Once two
   have been bought, starter upgrades are left alone and gold is saved for premium ones instead.
4. Otherwise, with at least 350 gold, buy the least-purchased premium upgrade from `ch`, `rf`, `iron`,
   `mtrix`, and `wingpotmax`, retaining a 50-gold healing reserve. Combined with rule 2, this means: once
   a mission harder than `Sure thing` appears, gold is spent on premium upgrades turn after turn until
   the board is entirely `Sure thing` again, at which point rule 2 takes back over.
5. Otherwise, solve a mission. Before turn 15, only "green" missions rated `Walk in the park` or safer
   are considered, falling back to the usual pick below if none is on the board. From turn 15 onward,
   `Suicide mission`, `Impossible`, and unknown risks are excluded while any other mission exists; with
   one life, the safest non-terminal mission is chosen; if every remaining mission's reward is already
   at least 200 gold (no upper bound), the safest one is taken outright since the extra reward elsewhere isn't
   worth chasing; otherwise the mission with the highest calculated utility is chosen.
6. If only terminal-risk missions remain, choose the safest of them as a last resort. If the board is empty, investigate.

Mission utility changes with the run. Before turn 15, safety and normalized reward each receive equal weight. From turn 15 onward, safety receives more weight and reward less; missions below 300 reward also use the lower difficulty weight. Equal utility is resolved in favor of the mission expiring sooner.

Any future tuning should add or amend focused tests in `DecisionEngineTest` first, then validate against the live game separately.

## Cooperation with the UI

The UI does not duplicate either strategy. Every backend `GameView` contains the selected `strategyMode` and one current `recommendation` produced by `DecisionEngine` from player state, advertisements, purchase counts, and consecutive board refreshes.

The API values are `OFF`, `CONSERVATIVE`, and `HIGH_RISK`. The backend session is the source of truth, so the selected strategy is restored with the rest of the game when `/games/{gameId}` is reloaded. New games start with `OFF`.

- Guidance is **Off** by default for every new game. Off removes visual guidance and stops a running automation loop. It does not change the backend's saved strategy mode; the player can still solve missions, buy items, or investigate directly.
- **Conservative** and **High risk** enable guidance and send the selected mode to `PUT /api/games/{gameId}/strategy-mode`. The backend immediately recalculates the recommendation.
- The UI highlights the recommended mission or shop item with `NEXT`. An investigation recommendation highlights the Investigate button. The UI uses the backend target identifier; it does not recalculate which action is best.
- **Run strategy** repeatedly calls `POST /api/games/{gameId}/auto/step`. For each call, `GameService` executes exactly the action in the current backend recommendation (`HEAL`, `PURCHASE`, `INVESTIGATE`, or `SOLVE`) and returns a fresh `GameView`. The Angular store waits for that response before requesting the next step.
- **Stop automation** sets a client-side stop flag. The loop stops between requests, so an already-sent turn is allowed to finish safely.
- Automation also stops on a failed request, a finished game, or a `STOP` recommendation. Each completed action is preserved in the chronicle, and mission results produce the short success/failure notification in the UI.

This arrangement makes guidance and automation two presentations of the same server-side decision. Selecting a strategy changes policy; selecting Off changes only how the player interacts with it.

## Safe experimentation rules

- Never automatically retry a turn-changing upstream request: the remote API has no idempotency key.
- Preserve the action history so a run can be inspected after it ends.
- Keep experiments out of controller and UI code. A policy change belongs in `DecisionEngine` plus tests.
