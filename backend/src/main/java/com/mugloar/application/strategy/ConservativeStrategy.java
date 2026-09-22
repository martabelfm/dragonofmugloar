package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.Probability;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Reaches the 1,000-point target with a low-risk, easy-to-explain policy.
 *
 * <p>Rules are checked in this exact order; the first one that applies wins:
 * <ol>
 *   <li>Heal if lives are low and a potion is affordable.</li>
 *   <li>Otherwise, buy the least-purchased starter upgrade if one is affordable.</li>
 *   <li>Otherwise, solve the safest mission rated {@link Probability#QUITE_LIKELY} or better.</li>
 *   <li>Otherwise, refresh the board (up to {@link #REFRESH_LIMIT} times) hoping for a safer mission.</li>
 *   <li>Once the refresh limit is reached, take the least-dangerous mission left, if any.</li>
 * </ol>
 *
 * <p>To tune this policy, adjust the constants below; the order of the rules lives in {@link #decide}.
 */
public class ConservativeStrategy implements GameStrategy {

    /** Heal as soon as lives drop below this many. */
    private static final int LIFE_BUFFER = 3;

    /** Refresh the board this many times before settling for whatever mission is safest. */
    private static final int REFRESH_LIMIT = 3;

    /** Cheap upgrades, bought round-robin so no single skill falls behind. */
    private static final List<String> STARTER_UPGRADES = List.of("cs", "gas", "wax", "tricks", "wingpot");

    /** Missions below this safety rank are never accepted while a safer option might appear. */
    private static final int MINIMUM_SAFE_RANK = Probability.QUITE_LIKELY.safetyRank();

    private static final int HEAL_UTILITY = 10_000;
    private static final int UPGRADE_UTILITY = 9_000;
    private static final int INVESTIGATE_UTILITY = 1_000;

    @Override
    public Decision decide(PlayerState player, List<Advertisement> ads, Map<String, Integer> purchasedItems,
                            int consecutiveBoardRefreshes) {
        if (needsHealing(player)) {
            return new Decision(Decision.Action.HEAL, "hpot", "Healing potion",
                    "Restore the three-life safety buffer before taking another risk.", HEAL_UTILITY);
        }
        if (canAffordAStarterUpgrade(player)) {
            var upgradeId = leastPurchased(STARTER_UPGRADES, purchasedItems);
            return new Decision(Decision.Action.PURCHASE, upgradeId, "Balanced equipment upgrade",
                    "Improve the least-trained dragon skill; healing is handled first when wounded.",
                    UPGRADE_UTILITY);
        }

        var safeMission = MissionRanking.safest(safeMissions(ads));
        if (safeMission != null) {
            return solveDecision(safeMission, describeSafeMission(safeMission));
        }
        if (consecutiveBoardRefreshes < REFRESH_LIMIT) {
            return new Decision(Decision.Action.INVESTIGATE, null, "Refresh the message board",
                    "No mission meets the safe risk policy; refresh without risking a life.", INVESTIGATE_UTILITY);
        }

        var fallback = MissionRanking.safest(ads);
        return fallback == null
                ? Decision.stop("No advertisement is available.")
                : solveDecision(fallback, "The refresh limit was reached; take the least-dangerous mission.");
    }

    private boolean needsHealing(PlayerState player) {
        return player.lives() < LIFE_BUFFER && player.gold() >= ShopPrices.HEALING_POTION_COST;
    }

    private boolean canAffordAStarterUpgrade(PlayerState player) {
        return player.gold() >= ShopPrices.LEVEL_UPGRADE_COST;
    }

    /** Missions rated {@link #MINIMUM_SAFE_RANK} or safer, in board order. */
    private List<Advertisement> safeMissions(List<Advertisement> ads) {
        return ads.stream()
                .filter(ad -> Probability.fromLabel(ad.probability()).safetyRank() >= MINIMUM_SAFE_RANK)
                .toList();
    }

    private String describeSafeMission(Advertisement mission) {
        return "%s is the safest available tier; reward %d gold, expires in %d turn%s."
                .formatted(mission.probability(), mission.reward(), mission.expiresIn(),
                        mission.expiresIn() == 1 ? "" : "s");
    }

    private Decision solveDecision(Advertisement mission, String reason) {
        return new Decision(Decision.Action.SOLVE, mission.adId(), mission.message(), reason, utility(mission));
    }

    /** Picks whichever of {@code itemIds} has been bought the fewest times so far. */
    private String leastPurchased(List<String> itemIds, Map<String, Integer> purchasedItems) {
        return itemIds.stream()
                .min(Comparator.comparingInt(id -> purchasedItems.getOrDefault(id, 0)))
                .orElseThrow();
    }

    /** Display-only ranking: safety dominates, reward breaks ties, an imminent expiry breaks those too. */
    private int utility(Advertisement ad) {
        var probability = Probability.fromLabel(ad.probability());
        return probability.safetyRank() * 1_000 + ad.reward() * 10 - Math.max(0, 3 - ad.expiresIn());
    }
}
