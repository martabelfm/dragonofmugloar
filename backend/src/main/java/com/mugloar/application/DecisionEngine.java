package com.mugloar.application;

import com.mugloar.application.strategy.GameStrategy;
import com.mugloar.application.strategy.HighRiskStrategy;
import com.mugloar.application.strategy.ConservativeStrategy;
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

    private final GameStrategy conservativeStrategy;
    private final GameStrategy highRiskStrategy;

    public DecisionEngine() {
        this(new ConservativeStrategy(), new HighRiskStrategy());
    }

    DecisionEngine(GameStrategy conservativeStrategy, GameStrategy highRiskStrategy) {
        this.conservativeStrategy = conservativeStrategy;
        this.highRiskStrategy = highRiskStrategy;
    }

    public Decision decide(PlayerState player, List<Advertisement> ads) {
        return decide(player, ads, StrategyMode.CONSERVATIVE, Map.of(), 0);
    }

    /** Keeps manual guidance and automation on the same server-side policy. */
    public Decision decide(PlayerState player, List<Advertisement> ads, StrategyMode mode,
                           Map<String, Integer> purchasedItems, int consecutiveBoardRefreshes) {
        if (player.isFinished()) return Decision.stop("No lives remain.");
        if (mode == StrategyMode.OFF) return Decision.stop("Strategy is off.");
        var strategy = mode == StrategyMode.HIGH_RISK ? highRiskStrategy : conservativeStrategy;
        var decision = strategy.decide(player, ads, purchasedItems, consecutiveBoardRefreshes);
        log.debug("Turn {} ({}): {} {} (utility {}) - {}", player.turn(), mode, decision.action(),
                decision.targetId(), decision.utility(), decision.reason());
        return decision;
    }
}
