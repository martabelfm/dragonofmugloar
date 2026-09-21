package com.mugloar.domain;

import java.util.Arrays;

public enum Probability {
    SURE_THING("Sure thing", 11),
    PIECE_OF_CAKE("Piece of cake", 10),
    WALK_IN_THE_PARK("Walk in the park", 9),
    QUITE_LIKELY("Quite likely", 8),
    HMMM("Hmmm....", 7),
    GAMBLE("Gamble", 6),
    RISKY("Risky", 5),
    PLAYING_WITH_FIRE("Playing with fire", 4),
    RATHER_DETRIMENTAL("Rather detrimental", 3),
    SUICIDE_MISSION("Suicide mission", 2),
    IMPOSSIBLE("Impossible", 1),
    UNKNOWN("Unknown", 0);

    private final String label;
    private final int safetyRank;

    Probability(String label, int safetyRank) {
        this.label = label;
        this.safetyRank = safetyRank;
    }

    public static Probability fromLabel(String label) {
        if (label == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(value -> value.label.equalsIgnoreCase(label.trim()))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public int safetyRank() { return safetyRank; }
}
