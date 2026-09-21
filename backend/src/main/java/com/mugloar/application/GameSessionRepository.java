package com.mugloar.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/** Bounds session growth for a long-running process: idle games are evicted, not kept forever. */
@Repository
public class GameSessionRepository {
    private static final Duration IDLE_EXPIRY = Duration.ofHours(2);
    private static final int MAX_SESSIONS = 10_000;

    private final Cache<String, GameSession> sessions = Caffeine.newBuilder()
            .expireAfterAccess(IDLE_EXPIRY)
            .maximumSize(MAX_SESSIONS)
            .build();

    GameSession save(GameSession session) {
        sessions.put(session.player.gameId(), session);
        return session;
    }

    GameSession require(String gameId) {
        var session = sessions.getIfPresent(gameId);
        if (session == null) throw new GameNotFoundException(gameId);
        return session;
    }
}
