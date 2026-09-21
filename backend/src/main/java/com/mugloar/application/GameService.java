package com.mugloar.application;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.ShopItem;
import com.mugloar.domain.StrategyMode;
import com.mugloar.domain.TurnRecord;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class GameService {
    public static final int TARGET_SCORE = 1_000;
    private static final String HEALING_POTION_ID = "hpot";
    private static final int HISTORY_LIMIT = 1_000;
    private final GamePort gamePort;
    private final GameSessionRepository repository;
    private final DecisionEngine decisionEngine;

    public GameService(GamePort gamePort, GameSessionRepository repository, DecisionEngine decisionEngine) {
        this.gamePort = gamePort;
        this.repository = repository;
        this.decisionEngine = decisionEngine;
    }

    public GameView start() {
        var session = repository.save(new GameSession(gamePort.startGame()));
        synchronized (session) {
            refreshResources(session);
            return view(session);
        }
    }

    public GameView get(String gameId) {
        var session = repository.require(gameId);
        synchronized (session) {
            return view(session);
        }
    }

    public GameView refresh(String gameId) {
        var session = repository.require(gameId);
        synchronized (session) {
            refreshResources(session);
            return view(session);
        }
    }

    public GameView solve(String gameId, String adId) {
        var session = repository.require(gameId);
        synchronized (session) {
            ensurePlayable(session);
            var ad = session.ads.stream().filter(value -> value.adId().equals(adId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Advertisement is not available."));
            var outcome = gamePort.solve(gameId, adId);
            session.player = new PlayerState(gameId, outcome.lives(), outcome.gold(), session.player.level(),
                    outcome.score(), outcome.highScore(), outcome.turn());
            record(session, Decision.Action.SOLVE, adId, outcome.message(), outcome.success());
            session.consecutiveBoardRefreshes = 0;
            refreshResources(session);
            return view(session);
        }
    }

    public GameView purchase(String gameId, String itemId) {
        var session = repository.require(gameId);
        synchronized (session) {
            ensurePlayable(session);
            var item = session.shop.stream().filter(value -> value.id().equals(itemId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Shop item is not available."));
            if (session.player.gold() < item.cost()) {
                throw new IllegalArgumentException("Not enough gold for %s.".formatted(item.name()));
            }
            var outcome = gamePort.purchase(gameId, itemId);
            session.player = new PlayerState(gameId, outcome.lives(), outcome.gold(), outcome.level(),
                    session.player.score(), session.player.highScore(), outcome.turn());
            if (outcome.success()) session.purchasedItems.merge(itemId, 1, Integer::sum);
            record(session, HEALING_POTION_ID.equals(itemId) ? Decision.Action.HEAL : Decision.Action.PURCHASE, itemId,
                    outcome.success() ? "Purchased %s.".formatted(item.name()) : "Purchase failed.", outcome.success());
            session.consecutiveBoardRefreshes = 0;
            refreshResources(session);
            return view(session);
        }
    }

    public GameView investigate(String gameId) {
        var session = repository.require(gameId);
        synchronized (session) {
            ensurePlayable(session);
            session.reputation = gamePort.investigate(gameId);
            session.player = new PlayerState(gameId, session.player.lives(), session.player.gold(),
                    session.player.level(), session.player.score(), session.player.highScore(), session.player.turn() + 1);
            record(session, Decision.Action.INVESTIGATE, null, "Investigated reputation.", true);
            session.consecutiveBoardRefreshes++;
            refreshResources(session);
            return view(session);
        }
    }

    public GameView autoStep(String gameId) {
        var session = repository.require(gameId);
        synchronized (session) {
            ensurePlayable(session);
            return switch (session.recommendation.action()) {
                case HEAL -> purchase(gameId, session.recommendation.targetId());
                case PURCHASE -> purchase(gameId, session.recommendation.targetId());
                case INVESTIGATE -> investigate(gameId);
                case SOLVE -> solve(gameId, session.recommendation.targetId());
                case STOP -> view(session);
            };
        }
    }

    public GameView updateStrategyMode(String gameId, StrategyMode strategyMode) {
        var session = repository.require(gameId);
        synchronized (session) {
            session.strategyMode = strategyMode;
            session.consecutiveBoardRefreshes = 0;
            session.recommendation = decide(session);
            return view(session);
        }
    }

    private void refreshResources(GameSession session) {
        if (session.player.isFinished()) {
            session.ads = List.of();
            session.recommendation = Decision.stop("The game is over.");
            return;
        }
        session.ads = List.copyOf(gamePort.getAds(session.player.gameId()));
        ensureShopLoaded(session);
        session.recommendation = decide(session);
    }

    private void record(GameSession session, Decision.Action action, String targetId, String description, boolean success) {
        if (session.history.size() >= HISTORY_LIMIT) session.history.removeFirst();
        session.history.add(new TurnRecord(session.player.turn(), action, targetId, description, success,
                session.player.score(), session.player.gold(), session.player.lives(), Instant.now()));
    }

    private void ensureShopLoaded(GameSession session) {
        if (session.shop.isEmpty()) {
            session.shop = List.copyOf(gamePort.getShop(session.player.gameId()));
        }
    }

    private void ensurePlayable(GameSession session) {
        if (session.player.isFinished()) throw new IllegalStateException("The game has ended.");
    }

    private GameView view(GameSession session) {
        return new GameView(session.player, List.copyOf(session.ads), List.copyOf(session.shop), session.reputation,
                session.strategyMode, java.util.Map.copyOf(session.purchasedItems),
                session.recommendation, List.copyOf(session.history), session.player.isFinished(),
                session.player.score() >= TARGET_SCORE);
    }

    private Decision decide(GameSession session) {
        return decisionEngine.decide(session.player, session.ads, session.strategyMode,
                session.purchasedItems, session.consecutiveBoardRefreshes);
    }
}
