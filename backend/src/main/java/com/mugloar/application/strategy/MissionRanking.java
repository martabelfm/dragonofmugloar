package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Probability;

import java.util.Comparator;
import java.util.List;

/** Mission ordering shared by every strategy: safest first, then better reward, then more time left. */
final class MissionRanking {
    static Advertisement safest(List<Advertisement> ads) {
        return ads.stream().max(Comparator
                .comparingInt((Advertisement ad) -> Probability.fromLabel(ad.probability()).safetyRank())
                .thenComparingInt(Advertisement::reward)
                .thenComparingInt(ad -> -ad.expiresIn())).orElse(null);
    }

    private MissionRanking() {}
}
