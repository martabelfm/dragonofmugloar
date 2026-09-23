package com.mugloar.application.strategy;

import com.mugloar.domain.Advertisement;
import com.mugloar.domain.Decision;
import com.mugloar.domain.PlayerState;
import com.mugloar.domain.Probability;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental higher-risk policy: balances upgrades against mission expected value.
 *
 * <p>Rules are checked in this exact order; the first one that applies wins:
 * <ol>
 *   <li>Heal whenever lives drop to {@link #CRITICAL_LIVES} or below and a potion is affordable.
 *       This always comes first, no matter how good a streak is going &mdash; staying alive is
 *       non-negotiable.</li>
 *   <li>While every mission on the board is rated "Sure thing", keep solving the best-paying one
 *       instead of shopping. A late-game "Sure thing" can be worth far more than the cost of any
 *       upgrade, so pausing to buy one is a waste of a turn. See {@link #isBoardEntirelySureThing}.</li>
 *   <li>Otherwise (the board is no longer all "Sure thing"), invest in up to
 *       {@link #STARTER_UPGRADE_INVESTMENT} starter upgrades total (least-purchased first) whenever
 *       one is affordable. Once that many have been bought, starter upgrades are left alone and gold
 *       is saved for premium upgrades instead.</li>
 *   <li>Otherwise, buy the least-purchased premium upgrade if it's affordable while keeping a
 *       50-gold healing reserve. Combined with the rule above, this means: once a mission harder than
 *       "Sure thing" appears, spend down the gold pile on premium upgrades, turn after turn, until the
 *       board is back to being entirely "Sure thing" again (rule 2 then takes back over).</li>
 *   <li>Otherwise, solve a mission. Early in the game (before {@link #EARLY_GAME_TURN_LIMIT}), only
 *       "green" missions &mdash; rated {@link #GREEN_MISSION_MINIMUM_RANK} or safer &mdash; are
 *       considered, falling back to the usual pick below if none is on the board. Later, terminal-risk
 *       missions ("Suicide mission", "Impossible", unknown probabilities) are excluded whenever a safer
 *       alternative exists, and if every remaining mission's reward is at least
 *       {@link #DECENT_REWARD_MIN}, the safest one is taken outright, however high the rewards go
 *       &mdash; the extra a riskier pick offers isn't worth chasing once the payout is already decent.
 *       Otherwise the mission with the highest calculated utility is chosen.</li>
 *   <li>Missions containing "steal" are excluded while any non-steal mission is rated Gamble or
 *       safer. They become eligible only when every alternative is in the red Risky-or-worse tier.</li>
 *   <li>If the board has no missions at all, refresh it instead.</li>
 * </ol>
 *
 * <p>To tune this policy, adjust the constants below; the order of the rules lives in {@link #decide}
 * and {@link #chooseMission}, and the mission-scoring formula lives in {@link #highScoreUtility}.
 */
public class HighRiskStrategy implements GameStrategy {

    private static final List<String> STARTER_UPGRADES = List.of("cs", "gas", "wax", "tricks", "wingpot");
    private static final List<String> PREMIUM_UPGRADES = List.of("ch", "rf", "iron", "mtrix", "wingpotmax");

    private static final int INVESTIGATE_UTILITY = 1_000;
    private static final int HEAL_UTILITY = 100_000;
    private static final int STARTER_UPGRADE_UTILITY = 95_000;
    private static final int PREMIUM_UPGRADE_UTILITY = 90_000;

    /** Heal as soon as lives fall to this many or fewer, instead of waiting until the last life. */
    private static final int CRITICAL_LIVES = 2;

    /** Total starter upgrades to buy as an early investment before saving gold for premium ones. */
    private static final int STARTER_UPGRADE_INVESTMENT = 2;

    private static final int PREMIUM_UPGRADE_GOLD_THRESHOLD =
            ShopPrices.PREMIUM_UPGRADE_COST + ShopPrices.HEALING_POTION_COST;

    // --- Mission-scoring knobs (see chooseMission and highScoreUtility) ---

    /** Before this turn, only "green" missions are solved and reward is weighted less; see below. */
    private static final int EARLY_GAME_TURN_LIMIT = 15;
    /** Missions at or above this safety rank are the "green" tier the early game sticks to. */
    private static final int GREEN_MISSION_MINIMUM_RANK = Probability.WALK_IN_THE_PARK.safetyRank();
    /** Matches the UI's boundary between the amber and red risk tiers. */
    private static final int RED_MISSION_MAXIMUM_RANK = Probability.RISKY.safetyRank();
    /**
     * Once every available mission's reward is at least this much, the safest one is taken outright
     * instead of chasing expected value, however high the rewards go &mdash; the outsized rewards worth
     * actually risking a life for only show up once the board is entirely "Sure thing" (see rule 2 in
     * the class doc), so there is no upper bound here.
     */
    private static final int DECENT_REWARD_MIN = 200;
    /** A mission worth less than this always uses the early-game (more cautious) safety weight. */
    private static final int LOW_REWARD_THRESHOLD = 300;
    private static final double HIGH_DIFFICULTY_WEIGHT = 75.0;
    private static final double LOW_DIFFICULTY_WEIGHT = 50.0;
    private static final double EARLY_GAME_REWARD_WEIGHT = 50.0;
    private static final double LATE_GAME_REWARD_WEIGHT = 25.0;
    /** {@link #difficultyOrder} ranges from 1 (Sure thing) to 11 (Impossible/Unknown); see that method. */
    private static final double MAX_DIFFICULTY_ORDER = 11.0;
    private static final double DIFFICULTY_ORDER_RANGE = 10.0;

    @Override
    public Decision decide(PlayerState player, List<Advertisement> ads, Map<String, Integer> purchasedItems,
                            int consecutiveBoardRefreshes) {
        var reputationSafeAds = avoidStealUnlessAlternativesAreRed(ads);
        if (needsHealing(player)) {
            return new Decision(Decision.Action.HEAL, "hpot", "Healing potion",
                    "Restore lives above the critical buffer before spending gold on upgrades.", HEAL_UTILITY);
        }
        if (isBoardEntirelySureThing(reputationSafeAds)) {
            var mission = MissionRanking.safest(reputationSafeAds);
            return solveDecision(mission,
                    "Every mission is a sure thing; keep cashing in instead of pausing to shop.",
                    highScoreUtility(player.turn(), mission, reputationSafeAds));
        }
        if (shouldInvestInStarterUpgrade(player, purchasedItems)) {
            return purchaseDecision(leastPurchased(STARTER_UPGRADES, purchasedItems), "Starter equipment investment",
                    "Bank an early, cheap upgrade before committing gold to premium equipment.",
                    STARTER_UPGRADE_UTILITY);
        }
        if (canAffordPremiumUpgrade(player)) {
            return purchaseDecision(leastPurchased(PREMIUM_UPGRADES, purchasedItems), "Premium level upgrade",
                    "Buy the least-purchased premium upgrade before risking a life; retain 50 gold for healing.",
                    PREMIUM_UPGRADE_UTILITY);
        }
        return chooseMission(player, reputationSafeAds);
    }

    private boolean needsHealing(PlayerState player) {
        return player.lives() <= CRITICAL_LIVES && player.gold() >= ShopPrices.HEALING_POTION_COST;
    }

    private boolean shouldInvestInStarterUpgrade(PlayerState player, Map<String, Integer> purchasedItems) {
        var starterUpgradesSoFar = STARTER_UPGRADES.stream()
                .mapToInt(id -> purchasedItems.getOrDefault(id, 0))
                .sum();
        return starterUpgradesSoFar < STARTER_UPGRADE_INVESTMENT && player.gold() >= ShopPrices.LEVEL_UPGRADE_COST;
    }

    private boolean canAffordPremiumUpgrade(PlayerState player) {
        return player.gold() >= PREMIUM_UPGRADE_GOLD_THRESHOLD;
    }

    private List<Advertisement> avoidStealUnlessAlternativesAreRed(List<Advertisement> ads) {
        var hasNonStealAlternativeOutsideRedTier = ads.stream()
                .filter(ad -> !isStealMission(ad))
                .anyMatch(ad -> Probability.fromLabel(ad.probability()).safetyRank() > RED_MISSION_MAXIMUM_RANK);
        if (!hasNonStealAlternativeOutsideRedTier) return ads;
        return ads.stream().filter(ad -> !isStealMission(ad)).toList();
    }

    private boolean isStealMission(Advertisement ad) {
        return ad.message() != null && ad.message().toLowerCase(Locale.ROOT).contains("steal");
    }

    /** True once the board has at least one mission and every one of them is rated "Sure thing". */
    private boolean isBoardEntirelySureThing(List<Advertisement> ads) {
        return !ads.isEmpty()
                && ads.stream().allMatch(ad -> Probability.fromLabel(ad.probability()) == Probability.SURE_THING);
    }

    /** Picks the best mission to solve, preferring non-terminal risks, or refreshes if the board is empty. */
    private Decision chooseMission(PlayerState player, List<Advertisement> ads) {
        if (player.turn() < EARLY_GAME_TURN_LIMIT) {
            var greenMission = MissionRanking.safest(greenMissions(ads));
            if (greenMission != null) {
                return solveDecision(greenMission,
                        "Early game: stick to a safe, green-rated mission while building up gold.",
                        highScoreUtility(player.turn(), greenMission, ads));
            }
        }

        var nonTerminal = nonTerminalMissions(ads);
        if (!nonTerminal.isEmpty()) {
            Advertisement mission;
            String reason;
            if (player.lives() == 1) {
                mission = MissionRanking.safest(nonTerminal);
                reason = "One life left: choose the safest non-terminal mission.";
            } else if (rewardsAreAllDecent(nonTerminal)) {
                mission = MissionRanking.safest(nonTerminal);
                reason = "Every reward is already decent; the extra risk elsewhere isn't worth it.";
            } else {
                mission = bestExpectedValueMission(player.turn(), nonTerminal);
                reason = "Best expected-value mission among non-terminal risks.";
            }
            return solveDecision(mission, reason, highScoreUtility(player.turn(), mission, nonTerminal));
        }

        var lastResort = MissionRanking.safest(ads);
        if (lastResort != null) {
            return solveDecision(lastResort,
                    "No non-terminal mission is available; take the least-bad last resort.",
                    highScoreUtility(player.turn(), lastResort, ads));
        }
        return new Decision(Decision.Action.INVESTIGATE, null, "Refresh the message board",
                "No mission is currently available.", INVESTIGATE_UTILITY);
    }

    /** True when every mission's reward is at least {@link #DECENT_REWARD_MIN}, with no upper bound. */
    private boolean rewardsAreAllDecent(List<Advertisement> ads) {
        return ads.stream().allMatch(ad -> ad.reward() >= DECENT_REWARD_MIN);
    }

    /** Missions rated {@link #GREEN_MISSION_MINIMUM_RANK} or safer, in board order. */
    private List<Advertisement> greenMissions(List<Advertisement> ads) {
        return ads.stream()
                .filter(ad -> Probability.fromLabel(ad.probability()).safetyRank() >= GREEN_MISSION_MINIMUM_RANK)
                .toList();
    }

    /** Missions that cannot end the run outright: excludes Suicide mission, Impossible, and Unknown. */
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

    private Decision solveDecision(Advertisement mission, String reason, double rawUtility) {
        return new Decision(Decision.Action.SOLVE, mission.adId(), mission.message(), reason,
                (int) Math.round(rawUtility * 1_000));
    }

    private Advertisement bestExpectedValueMission(int turn, List<Advertisement> ads) {
        return ads.stream().max(Comparator
                .comparingDouble((Advertisement ad) -> highScoreUtility(turn, ad, ads))
                .thenComparingInt(ad -> -ad.expiresIn())).orElse(null);
    }

    /** Picks whichever of {@code itemIds} has been bought the fewest times so far. */
    private String leastPurchased(List<String> itemIds, Map<String, Integer> purchasedItems) {
        return itemIds.stream().min(Comparator.comparingInt(id -> purchasedItems.getOrDefault(id, 0))).orElseThrow();
    }

    /**
     * Scores a mission by blending two normalized components, each in roughly [0, 1]:
     * <ul>
     *   <li><b>safety</b> &mdash; how far the mission's difficulty is from "Impossible" (safer scores higher)</li>
     *   <li><b>reward</b> &mdash; how the mission's payout compares to the rest of {@code ads} on the board</li>
     * </ul>
     * Early in the game (before {@link #EARLY_GAME_TURN_LIMIT}) or for a low-reward mission (below
     * {@link #LOW_REWARD_THRESHOLD}), safety and reward are weighted about equally. Later, with a
     * worthwhile reward, safety is weighted more heavily and reward less &mdash; see the *_WEIGHT constants.
     */
    private double highScoreUtility(int turn, Advertisement ad, List<Advertisement> ads) {
        var minReward = ads.stream().mapToInt(Advertisement::reward).min().orElse(0);
        var maxReward = ads.stream().mapToInt(Advertisement::reward).max().orElse(minReward + 1);
        var rewardRange = Math.max(1, maxReward - minReward);

        var earlyGame = turn < EARLY_GAME_TURN_LIMIT;
        var lowReward = ad.reward() < LOW_REWARD_THRESHOLD;
        var difficultyWeight = earlyGame || lowReward ? LOW_DIFFICULTY_WEIGHT : HIGH_DIFFICULTY_WEIGHT;
        var rewardWeight = earlyGame ? EARLY_GAME_REWARD_WEIGHT : LATE_GAME_REWARD_WEIGHT;

        var safetyScore = (MAX_DIFFICULTY_ORDER - difficultyOrder(ad.probability())) / DIFFICULTY_ORDER_RANGE;
        var normalizedReward = (double) (ad.reward() - minReward) / rewardRange;

        return safetyScore * difficultyWeight + normalizedReward * rewardWeight;
    }

    /** 1 = safest ("Sure thing") through 11 = most dangerous (Impossible/Unknown). */
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
