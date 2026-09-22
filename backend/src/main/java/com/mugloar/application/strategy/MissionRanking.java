package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Probability;

import java.util.Comparator;
import java.util.List;

/** Mission ordering shared by every strategy: safest first, ties broken by reward then by soonest expiry. */
final class MissionRanking {
    /** Returns {@code null} if {@code ads} is empty. */
    static Advertisement safest(List<Advertisement> ads) {
        return ads.stream()
                .max(Comparator
                        .comparingInt((Advertisement ad) -> Probability.fromLabel(ad.probability()).safetyRank())
                        .thenComparingInt(Advertisement::reward)
                        // expiresIn is negated so "max" picks the mission expiring soonest among ties.
                        .thenComparingInt(ad -> -ad.expiresIn()))
                .orElse(null);
    }

    private MissionRanking() {}
}
