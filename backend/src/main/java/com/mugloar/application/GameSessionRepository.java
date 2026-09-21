package com.mugloar.application;

import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;

/** Sessions are never evicted: acceptable for this assignment's short-lived process, not for a long-running server. */
@Repository
public class GameSessionRepository {
    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    GameSession save(GameSession session) {
        sessions.put(session.player.gameId(), session);
        return session;
    }

    GameSession require(String gameId) {
        var session = sessions.get(gameId);
        if (session == null) throw new GameNotFoundException(gameId);
        return session;
    }
}
