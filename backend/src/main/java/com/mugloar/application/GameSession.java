package com.mugloar.application;

import com.mugloar.domain.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GameSession {
    PlayerState player;
    List<Advertisement> ads = List.of();
    List<ShopItem> shop = List.of();
    Reputation reputation;
    Decision recommendation;
    StrategyMode strategyMode = StrategyMode.SAFE_1000;
    int consecutiveBoardRefreshes;
    final List<TurnRecord> history = new ArrayList<>();
    final Map<String, Integer> purchasedItems = new HashMap<>();

    GameSession(PlayerState player) {
        this.player = player;
    }

    /** Rejects any further mutating action once the dragon has run out of lives. */
    void ensurePlayable() {
        if (player.isFinished()) throw new IllegalStateException("The game has ended.");
    }
}
