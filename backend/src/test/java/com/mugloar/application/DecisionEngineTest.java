package com.mugloar.application;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.StrategyMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTest {
    private final DecisionEngine engine = new DecisionEngine();

    @Test
    void safeModeRestoresTheLifeBufferBeforeTakingAnotherRisk() {
        var decision = engine.decide(player(2, 50), List.of(ad("safe", 100, "Piece of cake")));

        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.HEAL, "hpot");
    }

    @Test
    void safeModePrefersTheSafestMissionThenTheBetterReward() {
        var decision = engine.decide(player(3, 0), List.of(
                ad("danger", 999, "Gamble"),
                ad("small", 10, "Sure thing"),
                ad("large", 80, "Sure thing")));

        assertThat(decision.targetId()).isEqualTo("large");
    }

    @Test
    void safeModeRefreshesBeforeTakingAnUnsafeMission() {
        var decision = engine.decide(player(3, 0), List.of(
                ad("unknown", 10, "New label"), ad("danger", 100, "Suicide mission")));

        assertThat(decision.action()).isEqualTo(Decision.Action.INVESTIGATE);
    }

    @Test
    void safeModeFallsBackToTheLeastDangerousMissionAfterThreeRefreshes() {
        var decision = engine.decide(player(3, 0), List.of(
                ad("impossible", 500, "Impossible"), ad("risky", 10, "Risky")),
                StrategyMode.CONSERVATIVE, Map.of(), 3);

        assertThat(decision.targetId()).isEqualTo("risky");
    }

    @Test
    void safeModeUpgradesTheLeastPurchasedStarterSkill() {
        var purchases = Map.of("cs", 3, "gas", 2, "wax", 0, "tricks", 1, "wingpot", 4);
        var decision = engine.decide(player(3, 100), List.of(ad("safe", 100, "Sure thing")),
                StrategyMode.CONSERVATIVE, purchases, 0);

        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.PURCHASE, "wax");
    }

    @Test
    void highScoreModeExcludesTerminalRisksWhenAnAlternativeExists() {
        var decision = engine.decide(player(3, 0), List.of(
                ad("impossible", 10_000, "Impossible"),
                ad("suicide", 9_000, "Suicide mission"), ad("gamble", 20, "Gamble")),
                StrategyMode.HIGH_RISK, Map.of(), 0);

        assertThat(decision.targetId()).isEqualTo("gamble");
    }

    @Test
    void highScoreModeUsesTheSafestMissionAtOneLifeWhenHealingIsUnaffordable() {
        var decision = engine.decide(player(1, 40), List.of(
                ad("valuable", 2_000, "Risky"), ad("safe-small", 20, "Sure thing"),
                ad("safe-large", 80, "Sure thing")), StrategyMode.HIGH_RISK, Map.of(), 0);

        assertThat(decision.targetId()).isEqualTo("safe-large");
    }

    @Test
    void highScoreModeBalancesPremiumUpgrades() {
        var purchases = Map.of("ch", 2, "rf", 1, "iron", 0, "mtrix", 3, "wingpotmax", 1);
        var decision = engine.decide(player(3, 350), List.of(ad("sure", 500, "Sure thing")),
                StrategyMode.HIGH_RISK, purchases, 0);

        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.PURCHASE, "iron");
    }

    private static PlayerState player(int lives, int gold) {
        return new PlayerState("game", lives, gold, 0, 0, 0, 0);
    }

    private static Advertisement ad(String id, int reward, String probability) {
        return new Advertisement(id, id, reward, 5, null, probability);
    }
}
