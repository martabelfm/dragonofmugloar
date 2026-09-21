package com.mugloar.application;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.Probability;
import com.mugloar.domain.StrategyMode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Converts the current game board into one explainable next action. */
@Component
public class DecisionEngine {
    static final int HEALING_POTION_COST = 50;
    static final int LEVEL_UPGRADE_COST = 100;

    private static final int SAFE_LIFE_BUFFER = 3;
    private static final int SAFE_REFRESH_LIMIT = 3;
    private static final String SAFE_UPGRADE_ID = "wingpot";
    private static final List<String> STARTER_UPGRADES = List.of("cs", "gas", "wax", "tricks", "wingpot");
    private static final List<String> PREMIUM_UPGRADES = List.of("ch", "rf", "iron", "mtrix", "wingpotmax");

    public Decision decide(PlayerState player, List<Advertisement> ads) {
        return decide(player, ads, StrategyMode.SAFE_1000, Map.of(), 0);
    }

    /** Keeps manual guidance and automation on the same server-side policy. */
    public Decision decide(PlayerState player, List<Advertisement> ads, StrategyMode mode,
                           Map<String, Integer> purchasedItems, int consecutiveBoardRefreshes) {
        if (player.isFinished()) return Decision.stop("No lives remain.");
        return mode == StrategyMode.HIGH_SCORE
                ? decideHighScore(player, ads, purchasedItems)
                : decideSafe(player, ads, consecutiveBoardRefreshes);
    }

    private Decision decideSafe(PlayerState player, List<Advertisement> ads, int consecutiveBoardRefreshes) {
        if (player.lives() < SAFE_LIFE_BUFFER && player.gold() >= HEALING_POTION_COST) {
            return new Decision(Decision.Action.HEAL, "hpot", "Healing potion",
                    "Restore the three-life safety buffer before taking another risk.", 10_000);
        }
        if (player.gold() >= LEVEL_UPGRADE_COST) {
            return purchaseDecision(SAFE_UPGRADE_ID, "Equipment upgrade",
                    "Spend available gold on a level upgrade; healing is handled first when wounded.", 9_000);
        }

        var safeMission = safestMission(safeMissions(ads));
        if (safeMission != null) return solveDecision(safeMission,
                "%s is the safest available tier; reward %d gold, expires in %d turn%s."
                        .formatted(safeMission.probability(), safeMission.reward(), safeMission.expiresIn(),
                                safeMission.expiresIn() == 1 ? "" : "s"));
        if (consecutiveBoardRefreshes < SAFE_REFRESH_LIMIT) {
            return new Decision(Decision.Action.INVESTIGATE, null, "Refresh the message board",
                    "No mission meets the safe risk policy; refresh without risking a life.", 1_000);
        }
        var fallback = safestMission(ads);
        return fallback == null
                ? Decision.stop("No advertisement is available.")
                : solveDecision(fallback, "The refresh limit was reached; take the least-dangerous mission.");
    }

    private Decision decideHighScore(PlayerState player, List<Advertisement> ads,
                                     Map<String, Integer> purchasedItems) {
        if (player.lives() == 1 && player.gold() >= HEALING_POTION_COST) {
            return new Decision(Decision.Action.HEAL, "hpot", "Healing potion",
                    "Restore the last life before taking another mission.", 100_000);
        }

        var regularMissions = nonTerminalMissions(ads);
        var candidates = regularMissions.isEmpty() ? ads : regularMissions;
        var mission = regularMissions.isEmpty()
                ? safestMission(ads)
                : player.lives() == 1
                ? safestMission(regularMissions)
                : bestExpectedValueMission(player.turn(), regularMissions);

        if (player.lives() == 2 && player.gold() >= 150) {
            return purchaseDecision(leastPurchased(STARTER_UPGRADES, purchasedItems), "Defensive level upgrade",
                    "At two lives, strengthen the dragon while retaining 50 gold for emergency healing.", 95_000);
        }
        if (player.gold() >= 350) {
            return purchaseDecision(leastPurchased(PREMIUM_UPGRADES, purchasedItems), "Premium level upgrade",
                    "Buy the least-purchased premium upgrade before risking a life; retain 50 gold for healing.",
                    90_000);
        }
        if (mission != null) {
            var reason = regularMissions.isEmpty()
                    ? "No non-terminal mission is available; take the least-bad last resort."
                    : "Best expected-value mission among non-terminal risks.";
            return new Decision(Decision.Action.SOLVE, mission.adId(), mission.message(), reason,
                    (int) Math.round(highScoreUtility(player.turn(), mission, candidates) * 1_000));
        }
        return new Decision(Decision.Action.INVESTIGATE, null, "Refresh the message board",
                "No mission is currently available.", 1_000);
    }

    private List<Advertisement> safeMissions(List<Advertisement> ads) {
        return ads.stream()
                .filter(ad -> Probability.fromLabel(ad.probability()).safetyRank()
                        >= Probability.QUITE_LIKELY.safetyRank())
                .toList();
    }

    private List<Advertisement> nonTerminalMissions(List<Advertisement> ads) {
        return ads.stream().filter(ad -> {
            var probability = Probability.fromLabel(ad.probability());
            return probability != Probability.SUICIDE_MISSION
                    && probability != Probability.IMPOSSIBLE
                    && probability != Probability.UNKNOWN;
        }).toList();
    }

    private Decision solveDecision(Advertisement mission, String reason) {
        return new Decision(Decision.Action.SOLVE, mission.adId(), mission.message(), reason, utility(mission));
    }

    private Decision purchaseDecision(String itemId, String title, String reason, int utility) {
        return new Decision(Decision.Action.PURCHASE, itemId, title, reason, utility);
    }

    private Advertisement bestExpectedValueMission(int turn, List<Advertisement> ads) {
        return ads.stream().max(Comparator
                .comparingDouble((Advertisement ad) -> highScoreUtility(turn, ad, ads))
                .thenComparingInt(ad -> -ad.expiresIn())).orElse(null);
    }

    private Advertisement safestMission(List<Advertisement> ads) {
        return ads.stream().max(Comparator
                .comparingInt((Advertisement ad) -> Probability.fromLabel(ad.probability()).safetyRank())
                .thenComparingInt(Advertisement::reward)
                .thenComparingInt(ad -> -ad.expiresIn())).orElse(null);
    }

    private String leastPurchased(List<String> itemIds, Map<String, Integer> purchasedItems) {
        return itemIds.stream().min(Comparator.comparingInt(id -> purchasedItems.getOrDefault(id, 0))).orElseThrow();
    }

    private double highScoreUtility(int turn, Advertisement ad, List<Advertisement> ads) {
        var minReward = ads.stream().mapToInt(Advertisement::reward).min().orElse(0);
        var maxReward = ads.stream().mapToInt(Advertisement::reward).max().orElse(minReward + 1);
        var rewardRange = Math.max(1, maxReward - minReward);
        var earlyGame = turn < 15;
        var difficultyWeight = earlyGame || ad.reward() < 300 ? 50.0 : 75.0;
        return ((11.0 - difficultyOrder(ad.probability())) / 10.0) * difficultyWeight
                + ((double) (ad.reward() - minReward) / rewardRange) * (earlyGame ? 50.0 : 25.0);
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

    int utility(Advertisement ad) {
        var probability = Probability.fromLabel(ad.probability());
        return probability.safetyRank() * 1_000 + ad.reward() * 10 - Math.max(0, 3 - ad.expiresIn());
    }
}
