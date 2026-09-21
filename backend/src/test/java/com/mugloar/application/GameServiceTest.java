package com.mugloar.application;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.Reputation;
import com.mugloar.domain.ShopItem;
import com.mugloar.domain.StrategyMode;
import com.mugloar.domain.TurnRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {
    @Test
    void automatedStepsReachTheRequiredScoreAndRecordEveryAction() {
        var service = service(new SuccessfulGamePort());
        var game = service.start();

        while (game.player().score() < GameService.TARGET_SCORE) {
            game = service.autoStep(game.player().gameId());
        }

        assertThat(game.targetReached()).isTrue();
        assertThat(game.player().score()).isEqualTo(GameService.TARGET_SCORE);
        assertThat(game.history()).hasSize(19).allMatch(TurnRecord::successful);
    }

    @Test
    void rejectsAnUnaffordablePurchaseBeforeCallingTheUpstreamApi() {
        var port = new SuccessfulGamePort();
        var service = service(port);
        var game = service.start();

        assertThatThrownBy(() -> service.purchase(game.player().gameId(), "hpot"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough gold");
        assertThat(port.purchaseCalls).isZero();
    }

    @Test
    void storesTheSelectedStrategyModeInTheGameSession() {
        var service = service(new SuccessfulGamePort());
        var game = service.start();

        var updated = service.updateStrategyMode(game.player().gameId(), StrategyMode.HIGH_SCORE);

        assertThat(updated.strategyMode()).isEqualTo(StrategyMode.HIGH_SCORE);
        assertThat(service.get(game.player().gameId()).strategyMode()).isEqualTo(StrategyMode.HIGH_SCORE);
    }

    @Test
    void countsSuccessfulPurchasesPerShopItem() {
        var port = new SuccessfulGamePort();
        port.gold = 300;
        var service = service(port);
        var game = service.start();

        service.purchase(game.player().gameId(), "wingpot");
        var updated = service.purchase(game.player().gameId(), "wingpot");

        assertThat(updated.purchasedItems()).containsEntry("wingpot", 2);
    }

    private static GameService service(GamePort port) {
        return new GameService(port, new GameSessionRepository(), new DecisionEngine());
    }

    private static final class SuccessfulGamePort implements GamePort {
        int score;
        int gold;
        int lives = 3;
        int level;
        int turn;
        int purchaseCalls;

        @Override public PlayerState startGame() {
            return new PlayerState("game-1", lives, gold, level, score, score, turn);
        }

        @Override public List<Advertisement> getAds(String gameId) {
            return List.of(new Advertisement("ad-a-" + turn, "Safe mission", 100, 7, null, "Piece of cake"));
        }

        @Override public List<ShopItem> getShop(String gameId) {
            return List.of(
                    new ShopItem("hpot", "Healing potion", 50),
                    new ShopItem("wingpot", "Potion of Stronger Wings", 100));
        }

        @Override public GamePort.SolveOutcome solve(String gameId, String adId) {
            turn++;
            score += 100;
            gold += 100;
            return new GamePort.SolveOutcome(true, lives, gold, score, score, turn, "Success");
        }

        @Override public GamePort.PurchaseOutcome purchase(String gameId, String itemId) {
            purchaseCalls++;
            if ("hpot".equals(itemId)) {
                gold -= 50;
                lives++;
            } else {
                gold -= 100;
                level++;
            }
            return new GamePort.PurchaseOutcome(true, gold, lives, level, ++turn);
        }

        @Override public Reputation investigate(String gameId) {
            return new Reputation(0, 0, 0);
        }
    }
}
