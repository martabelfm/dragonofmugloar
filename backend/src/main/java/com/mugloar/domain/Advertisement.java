package com.mugloar.domain;

public record Advertisement(
        String adId,
        String message,
        int reward,
        int expiresIn,
        Object encrypted,
        String probability
) {
}
