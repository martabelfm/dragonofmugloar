package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.Probability;

import java.util.List;
import java.util.Map;

/** Reaches the 1,000-point target with an explainable, low-risk policy. */
public class SafeStrategy implements GameStrategy {
    private static final int LIFE_BUFFER = 3;
    private static final int REFRESH_LIMIT = 3;
    private static final String UPGRADE_ID = "wingpot";
    private static final int INVESTIGATE_UTILITY = 1_000;

    @Override
    public Decision decide(PlayerState player, List<Advertisement> ads, Map<String, Integer> purchasedItems,
                            int consecutiveBoardRefreshes) {
        if (player.lives() < LIFE_BUFFER && player.gold() >= ShopPrices.HEALING_POTION_COST) {
            return new Decision(Decision.Action.HEAL, "hpot", "Healing potion",
                    "Restore the three-life safety buffer before taking another risk.", 10_000);
        }
        if (player.gold() >= ShopPrices.LEVEL_UPGRADE_COST) {
            return new Decision(Decision.Action.PURCHASE, UPGRADE_ID, "Equipment upgrade",
                    "Spend available gold on a level upgrade; healing is handled first when wounded.", 9_000);
        }

        var safeMission = MissionRanking.safest(safeMissions(ads));
        if (safeMission != null) return solveDecision(safeMission,
                "%s is the safest available tier; reward %d gold, expires in %d turn%s."
                        .formatted(safeMission.probability(), safeMission.reward(), safeMission.expiresIn(),
                                safeMission.expiresIn() == 1 ? "" : "s"));
        if (consecutiveBoardRefreshes < REFRESH_LIMIT) {
            return new Decision(Decision.Action.INVESTIGATE, null, "Refresh the message board",
                    "No mission meets the safe risk policy; refresh without risking a life.", INVESTIGATE_UTILITY);
        }
        var fallback = MissionRanking.safest(ads);
        return fallback == null
                ? Decision.stop("No advertisement is available.")
                : solveDecision(fallback, "The refresh limit was reached; take the least-dangerous mission.");
    }

    private List<Advertisement> safeMissions(List<Advertisement> ads) {
        return ads.stream()
                .filter(ad -> Probability.fromLabel(ad.probability()).safetyRank()
                        >= Probability.QUITE_LIKELY.safetyRank())
                .toList();
    }

    private Decision solveDecision(Advertisement mission, String reason) {
        return new Decision(Decision.Action.SOLVE, mission.adId(), mission.message(), reason, utility(mission));
    }

    private int utility(Advertisement ad) {
        var probability = Probability.fromLabel(ad.probability());
        return probability.safetyRank() * 1_000 + ad.reward() * 10 - Math.max(0, 3 - ad.expiresIn());
    }
}
