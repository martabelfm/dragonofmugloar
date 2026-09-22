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
        var purchases = Map.of(
                "cs", 2, "gas", 2, // starter-upgrade investment already complete
                "ch", 2, "rf", 1, "iron", 0, "mtrix", 3, "wingpotmax", 1);
        var decision = engine.decide(player(3, 350), List.of(ad("mission", 500, "Quite likely")),
                StrategyMode.HIGH_RISK, purchases, 0);

        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.PURCHASE, "iron");
    }

    @Test
    void highScoreModeInvestsInTwoStarterUpgradesBeforePremiumOnes() {
        var decision = engine.decide(player(3, 350), List.of(ad("mission", 500, "Quite likely")),
                StrategyMode.HIGH_RISK, Map.of(), 0);

        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.PURCHASE, "cs");
    }

    @Test
    void highScoreModeHealsAtTwoLivesInsteadOfBuyingACheapUpgrade() {
        var decision = engine.decide(player(2, 200), List.of(ad("mission", 500, "Quite likely")),
                StrategyMode.HIGH_RISK, Map.of(), 0);

        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.HEAL, "hpot");
    }

    @Test
    void highScoreModeSticksToGreenMissionsEarlyEvenWhenARiskierOneIsMoreRewarding() {
        var decision = engine.decide(player(3, 0), List.of(
                ad("risky-big", 900, "Risky"), ad("green-small", 30, "Walk in the park")),
                StrategyMode.HIGH_RISK, Map.of(), 0);

        assertThat(decision.targetId()).isEqualTo("green-small");
    }

    @Test
    void highScoreModeKeepsSolvingInsteadOfShoppingWhileEveryMissionIsASureThing() {
        var purchases = Map.of("cs", 2, "gas", 2); // starter investment already complete
        var decision = engine.decide(player(3, 400), List.of(
                ad("small", 50, "Sure thing"), ad("large", 900, "Sure thing")),
                StrategyMode.HIGH_RISK, purchases, 0);

        // 400 gold is enough for a premium upgrade, but the board is entirely "Sure thing" so it
        // keeps cashing in the best-paying one instead of pausing to shop.
        assertThat(decision).extracting(Decision::action, Decision::targetId)
                .containsExactly(Decision.Action.SOLVE, "large");
    }

    @Test
    void highScoreModePrefersTheSafestMissionWhenEveryRewardIsAlreadyDecent() {
        var lateGamePlayer = new PlayerState("game", 3, 0, 0, 0, 0, 20);
        var decision = engine.decide(lateGamePlayer, List.of(
                ad("safer", 220, "Quite likely"), ad("riskier", 280, "Gamble")),
                StrategyMode.HIGH_RISK, Map.of(), 0);

        // Without this rule, "riskier" would win on expected value despite paying only 60 gold more;
        // at this reward size the extra risk isn't worth it.
        assertThat(decision.targetId()).isEqualTo("safer");
    }

    @Test
    void highScoreModePrefersSafetyEvenWhenARiskierMissionPaysFarMore() {
        var lateGamePlayer = new PlayerState("game", 3, 0, 0, 0, 0, 20);
        var decision = engine.decide(lateGamePlayer, List.of(
                ad("safer", 220, "Quite likely"), ad("riskier", 900, "Gamble")),
                StrategyMode.HIGH_RISK, Map.of(), 0);

        // There is no upper bound on the "decent reward" rule: once every mission already pays at
        // least DECENT_REWARD_MIN, extra risk is not chased no matter how large the gap gets. Outsized
        // rewards are only worth the risk once the board is entirely "Sure thing".
        assertThat(decision.targetId()).isEqualTo("safer");
    }

    private static PlayerState player(int lives, int gold) {
        return new PlayerState("game", lives, gold, 0, 0, 0, 0);
    }

    private static Advertisement ad(String id, int reward, String probability) {
        return new Advertisement(id, id, reward, 5, null, probability);
    }
}
