package com.mugloar.application;

import com.mugloar.application.strategy.GameStrategy;
import com.mugloar.application.strategy.HighScoreStrategy;
import com.mugloar.application.strategy.SafeStrategy;
import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.StrategyMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Converts the current game board into one explainable next action. */
@Component
public class DecisionEngine {
    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    private final GameStrategy safeStrategy;
    private final GameStrategy highScoreStrategy;

    public DecisionEngine() {
        this(new SafeStrategy(), new HighScoreStrategy());
    }

    DecisionEngine(GameStrategy safeStrategy, GameStrategy highScoreStrategy) {
        this.safeStrategy = safeStrategy;
        this.highScoreStrategy = highScoreStrategy;
    }

    public Decision decide(PlayerState player, List<Advertisement> ads) {
        return decide(player, ads, StrategyMode.SAFE_1000, Map.of(), 0);
    }

    /** Keeps manual guidance and automation on the same server-side policy. */
    public Decision decide(PlayerState player, List<Advertisement> ads, StrategyMode mode,
                           Map<String, Integer> purchasedItems, int consecutiveBoardRefreshes) {
        if (player.isFinished()) return Decision.stop("No lives remain.");
        var strategy = mode == StrategyMode.HIGH_SCORE ? highScoreStrategy : safeStrategy;
        var decision = strategy.decide(player, ads, purchasedItems, consecutiveBoardRefreshes);
        log.debug("Turn {} ({}): {} {} (utility {}) - {}", player.turn(), mode, decision.action(),
                decision.targetId(), decision.utility(), decision.reason());
        return decision;
    }
}
