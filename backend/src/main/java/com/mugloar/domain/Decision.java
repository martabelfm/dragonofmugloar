package com.mugloar.domain;

public record Decision(
        Action action,
        String targetId,
        String title,
        String reason,
        int utility
) {
    public enum Action { SOLVE, HEAL, PURCHASE, INVESTIGATE, STOP }

    public static Decision stop(String reason) {
        return new Decision(Action.STOP, null, "Stop", reason, 0);
    }
}
