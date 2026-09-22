package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.Probability;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Experimental higher-risk policy: balances upgrades against mission expected value. */
public class HighRiskStrategy implements GameStrategy {
    private static final List<String> STARTER_UPGRADES = List.of("cs", "gas", "wax", "tricks", "wingpot");
    private static final List<String> PREMIUM_UPGRADES = List.of("ch", "rf", "iron", "mtrix", "wingpotmax");

    private static final int INVESTIGATE_UTILITY = 1_000;
    private static final int HEAL_UTILITY = 100_000;
    /** Upgrade cost plus a healing-potion reserve, so a purchase never leaves the dragon unable to heal. */
    private static final int DEFENSIVE_UPGRADE_GOLD_THRESHOLD =
            ShopPrices.LEVEL_UPGRADE_COST + ShopPrices.HEALING_POTION_COST;
    private static final int DEFENSIVE_UPGRADE_UTILITY = 95_000;
    private static final int PREMIUM_UPGRADE_GOLD_THRESHOLD =
            ShopPrices.PREMIUM_UPGRADE_COST + ShopPrices.HEALING_POTION_COST;
    private static final int PREMIUM_UPGRADE_UTILITY = 90_000;

    /** Weights for {@link #highScoreUtility}: below this turn, favor safety and reward equally. */
    private static final int EARLY_GAME_TURN_LIMIT = 15;
    private static final int LOW_REWARD_THRESHOLD = 300;
    private static final double HIGH_DIFFICULTY_WEIGHT = 75.0;
    private static final double LOW_DIFFICULTY_WEIGHT = 50.0;
    private static final double EARLY_GAME_REWARD_WEIGHT = 50.0;
    private static final double LATE_GAME_REWARD_WEIGHT = 25.0;
    private static final double MAX_DIFFICULTY_ORDER = 11.0;
    private static final double DIFFICULTY_ORDER_RANGE = 10.0;

    @Override
    public Decision decide(PlayerState player, List<Advertisement> ads, Map<String, Integer> purchasedItems,
                            int consecutiveBoardRefreshes) {
        if (player.lives() == 1 && player.gold() >= ShopPrices.HEALING_POTION_COST) {
            return new Decision(Decision.Action.HEAL, "hpot", "Healing potion",
                    "Restore the last life before taking another mission.", HEAL_UTILITY);
        }
        if (player.lives() == 2 && player.gold() >= DEFENSIVE_UPGRADE_GOLD_THRESHOLD) {
            return purchaseDecision(leastPurchased(STARTER_UPGRADES, purchasedItems), "Defensive level upgrade",
                    "At two lives, strengthen the dragon while retaining 50 gold for emergency healing.",
                    DEFENSIVE_UPGRADE_UTILITY);
        }
        if (player.gold() >= PREMIUM_UPGRADE_GOLD_THRESHOLD) {
            return purchaseDecision(leastPurchased(PREMIUM_UPGRADES, purchasedItems), "Premium level upgrade",
                    "Buy the least-purchased premium upgrade before risking a life; retain 50 gold for healing.",
                    PREMIUM_UPGRADE_UTILITY);
        }

        var regularMissions = nonTerminalMissions(ads);
        var candidates = regularMissions.isEmpty() ? ads : regularMissions;
        var mission = regularMissions.isEmpty()
                ? MissionRanking.safest(ads)
                : player.lives() == 1
                ? MissionRanking.safest(regularMissions)
                : bestExpectedValueMission(player.turn(), regularMissions);

        if (mission != null) {
            var reason = regularMissions.isEmpty()
                    ? "No non-terminal mission is available; take the least-bad last resort."
                    : "Best expected-value mission among non-terminal risks.";
            return new Decision(Decision.Action.SOLVE, mission.adId(), mission.message(), reason,
                    (int) Math.round(highScoreUtility(player.turn(), mission, candidates) * 1_000));
        }
        return new Decision(Decision.Action.INVESTIGATE, null, "Refresh the message board",
                "No mission is currently available.", INVESTIGATE_UTILITY);
    }

    private List<Advertisement> nonTerminalMissions(List<Advertisement> ads) {
        return ads.stream().filter(ad -> {
            var probability = Probability.fromLabel(ad.probability());
            return probability != Probability.SUICIDE_MISSION
                    && probability != Probability.IMPOSSIBLE
                    && probability != Probability.UNKNOWN;
        }).toList();
    }

    private Decision purchaseDecision(String itemId, String title, String reason, int utility) {
        return new Decision(Decision.Action.PURCHASE, itemId, title, reason, utility);
    }

    private Advertisement bestExpectedValueMission(int turn, List<Advertisement> ads) {
        return ads.stream().max(Comparator
                .comparingDouble((Advertisement ad) -> highScoreUtility(turn, ad, ads))
                .thenComparingInt(ad -> -ad.expiresIn())).orElse(null);
    }

    private String leastPurchased(List<String> itemIds, Map<String, Integer> purchasedItems) {
        return itemIds.stream().min(Comparator.comparingInt(id -> purchasedItems.getOrDefault(id, 0))).orElseThrow();
    }

    private double highScoreUtility(int turn, Advertisement ad, List<Advertisement> ads) {
        var minReward = ads.stream().mapToInt(Advertisement::reward).min().orElse(0);
        var maxReward = ads.stream().mapToInt(Advertisement::reward).max().orElse(minReward + 1);
        var rewardRange = Math.max(1, maxReward - minReward);
        var earlyGame = turn < EARLY_GAME_TURN_LIMIT;
        var difficultyWeight = earlyGame || ad.reward() < LOW_REWARD_THRESHOLD
                ? LOW_DIFFICULTY_WEIGHT : HIGH_DIFFICULTY_WEIGHT;
        var rewardWeight = earlyGame ? EARLY_GAME_REWARD_WEIGHT : LATE_GAME_REWARD_WEIGHT;
        return ((MAX_DIFFICULTY_ORDER - difficultyOrder(ad.probability())) / DIFFICULTY_ORDER_RANGE) * difficultyWeight
                + ((double) (ad.reward() - minReward) / rewardRange) * rewardWeight;
    }

    private int difficultyOrder(String label) {
        return switch (Probability.fromLabel(label)) {
            case SURE_THING -> 1;
            case PIECE_OF_CAKE -> 2;
            case WALK_IN_THE_PARK -> 3;
            case QUITE_LIKELY -> 4;
            case HMMM -> 5;
            case GAMBLE -> 6;
            case RISKY -> 7;
            case PLAYING_WITH_FIRE -> 8;
            case RATHER_DETRIMENTAL -> 9;
            case SUICIDE_MISSION -> 10;
            case IMPOSSIBLE, UNKNOWN -> 11;
        };
    }
}
