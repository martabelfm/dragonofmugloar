# Game mechanics notes

The upstream game has hidden and partly random rules. This document separates repeated observations from working hypotheses used by the strategies.

## Observed behavior

### Turns and levels

- Mission boards tend to become harder when turns advance without leveling.
- Leveling can make the available missions easier again.
- Starter equipment costs 100 gold and appears to give 1 level.
- Premium equipment costs 300 gold and appears to give 2 levels.
- Premium equipment is therefore more level-efficient per turn, although less efficient per gold.
- A shop purchase uses a turn. Healing after a failed mission effectively costs another turn plus 50 gold.

This suggests that mission difficulty depends on turn progression relative to dragon level, although the exact formula is unknown.

### Reputation

Reputation has three values: People, State, and Underworld. It accumulates continuously from
the start of the game session; `investigate/reputation` only reveals the current values, it does
not reset or scope them to "since the last check." (An early data-collection run showed a jump
of People +6.1 on the very first check after 45 turns of `HIGH_RISK` automation — that jump was
the accumulated effect of every prior turn being revealed for the first time, not caused by one
mission.)

Reputation effects are a **fixed amount per mission archetype**, identified by the message
wording rather than the probability label, and are only applied when the mission **succeeds**.
Confirmed against 376 solves: every archetype below
was also seen to fail at least twice, and not one failure changed reputation by any amount.

| Message pattern | People | State | Underworld | Successes (n) |
|---|---|---|---|---|
| "Create an advertisement campaign for *X* to promote their *Y* based business" | +1 | 0 | 0 | 46 |
| "Help defending *place* from the intruders" | 0 | 0 | 0 | 44 |
| "Help *X* to [clean/fix/repair] their *Y*" (plain chores) | +0.1 | 0 | 0 | 35 |
| "Escort *X* to *Y* where they can meet with their long lost *Z*" | +0.1 | 0 | 0 | 29 |
| "Help *X* to sell an unordinary *Y* on the local market" | +0.1 | 0 | 0 | 25 |
| "...biographical novel about their difficulties with a deranged *X*" | +1 | 0 | 0 | 23 |
| "Steal *X* delivery to *Y* and share some of the profits with the people." | +1 | **−2** | 0 | 23 |
| "Help *X* to transport a magic *Y* to *place*" | +0.3 | 0 | 0 | 20 |
| "Help *X* to reach an agreement with *Y* on the matters of disputed *Z*" | +0.2 | 0 | 0 | 18 |
| "Investigate *X* and find out their relation to the magic *Y*" (often encrypted) | **−0.1** | **+1** | 0 | 5 |
| "Rescue *X* from *Y* where they are attacked by angry *Z*" | +0.1 | 0 | 0 | 5 |
| "Infiltrate *The [Gang Name]* and recover their secrets." (always encrypted) | 0 | **+2** | **−1** | 3 |
| "Kill *X* with *weapon* and make *Y* ... take the blame" (ROT13-encoded) | unconfirmed — every attempt so far has failed | | | 0 |

**Underworld is now confirmed.** `Infiltrate ... secrets` is the only archetype seen to move it —
always −1, paired with State +2 and no People change, the only archetype trading State against
Underworld rather than moving them together. `Kill X ... take the blame` remains the one
unconfirmed archetype: every sampled attempt (across both this dataset and earlier manual runs)
has failed before revealing an effect.

Separately and previously documented:

- Around **-10 State reputation**, trap failures have started appearing in observed runs. State
  never dropped below −4 in the 376-row dataset above, so it added no new trap evidence either way.
- The trap response is different from an ordinary probability failure: `You fell into a trap set up by people who did not appreciate your dealings.`
- When State reputation has remained good, this trap response has not been observed.

This strongly suggests that the trap is reputation-related, but `-10` is not yet proven to be an exact threshold. Mission probability may still contribute to the result.

A previous community experiment also identified `steal` and `advertisement` as strongly reputation-affecting words, with `magic` and `deranged` having a smaller effect. It did not determine the complete Underworld behavior. See the [public experiment](https://github.com/CardoEggert/DragonsOfMugloarPlayer). The table above independently confirms and quantifies `steal`, `advertisement`, `magic`, and `deranged`, and adds `escort`, plain chores, and "defending" as newly measured archetypes.

The High Risk strategy now avoids `steal` missions while any non-steal mission rated `Gamble` or safer is available. Steal becomes eligible only when every alternative is red (`Risky` or worse).

### Missions

- Probability labels are useful but do not describe every risk; mission wording and reputation also appear important.
- Mission expiry continues during shopping, healing, and other turn-consuming actions.
- Encrypted advertisements must be decoded before evaluation. Observed `encrypted` values include
  `1` and `2` (both truthy); both base64 and ROT13 encodings have been observed for `adId`,
  `message`, and `probability` together on the same ad.
- A "Kill *X* with *weapon* and make *Y* from *place* in *place* to take the blame" archetype
  (ROT13-encoded) exists; see [Reputation](#reputation) — its effect on success is still unknown.
- Results remain nondeterministic, so a single run is not enough to establish a rule.

### Investigation

Investigation reveals People, State, and Underworld reputation and advances the upstream game by one
turn. The collector investigates after every mission to calculate an exact reputation delta, so its
experimental games advance two turns per recorded mission.

## Strategy implications

- Maintain level growth instead of saving gold indefinitely.
- Prefer premium upgrades when level gained per turn matters more than gold efficiency.
- Avoid repeated failure-and-heal cycles because they consume lives, gold, and turns.
- Evaluate mission wording as well as probability and reward.
- Avoid damaging State reputation through `steal` missions unless the alternatives are already dangerous.
