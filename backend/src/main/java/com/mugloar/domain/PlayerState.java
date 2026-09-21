package com.mugloar.domain;

public record PlayerState(
        String gameId,
        int lives,
        int gold,
        int level,
        int score,
        int highScore,
        int turn
) {
    public boolean isFinished() {
        return lives <= 0;
    }
}
