# Decision policy notes

This file documents the current policy boundary, not a promise of a particular live score. The remote game has hidden and changing mechanics, so score experiments should not be treated as deterministic acceptance tests.

## Single source of truth

`backend/src/main/java/com/mugloar/application/DecisionEngine.java` is the only place that chooses a recommended action. The Angular client renders its recommendation and `POST /api/games/{gameId}/auto/step` executes that same action. This prevents manual guidance and automation from drifting apart.

## Safe 1000 mode

The baseline policy is intentionally explainable:

1. Restore the three-life buffer when an affordable healing potion is available.
2. Spend available gold on the repeatable level upgrade.
3. Prefer `Quite likely` or safer advertisements, ordered by safety, reward, and expiry.
4. Refresh the board up to three times when no accepted ad exists.
5. Use the least-dangerous remaining ad only after that bounded refresh limit.

The deterministic `GameServiceTest` validates that this automation reaches the assignment's 1,000-point target against a successful game port.

## High-score mode

High-score mode is a deliberately isolated experimental policy. It avoids `Suicide mission` and `Impossible` while alternatives exist, protects the final life, and balances purchases among upgrade types. Any future tuning should add or amend focused tests in `DecisionEngineTest` first, then validate against the live game separately.

## Safe experimentation rules

- Never automatically retry a turn-changing upstream request: the remote API has no idempotency key.
- Preserve the action history so a run can be inspected after it ends.
- Keep experiments out of controller and UI code. A policy change belongs in `DecisionEngine` plus tests.
