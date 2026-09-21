package com.mugloar.domain;

import java.time.Instant;

public record TurnRecord(
        int turn,
        Decision.Action action,
        String targetId,
        String description,
        boolean successful,
        int score,
        int gold,
        int lives,
        Instant occurredAt
) {
}
