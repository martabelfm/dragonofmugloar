package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;

import java.util.List;
import java.util.Map;

/** One decision policy: turns the current board into a single recommended action. */
public interface GameStrategy {
    Decision decide(PlayerState player, List<Advertisement> ads, Map<String, Integer> purchasedItems,
                     int consecutiveBoardRefreshes);
}
