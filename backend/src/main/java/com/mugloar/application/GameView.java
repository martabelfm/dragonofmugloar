package com.mugloar.application;

import com.mugloar.domain.*;
import java.util.List;
import java.util.Map;

public record GameView(
        PlayerState player,
        List<Advertisement> ads,
        List<ShopItem> shop,
        Reputation reputation,
        StrategyMode strategyMode,
        Map<String, Integer> purchasedItems,
        Decision recommendation,
        List<TurnRecord> history,
        boolean finished,
        boolean targetReached
) {
}
