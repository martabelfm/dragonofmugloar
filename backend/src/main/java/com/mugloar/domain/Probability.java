package com.mugloar.domain;

import java.util.Arrays;

public enum Probability {
    SURE_THING("Sure thing", 11, true),
    PIECE_OF_CAKE("Piece of cake", 10, true),
    WALK_IN_THE_PARK("Walk in the park", 9, true),
    QUITE_LIKELY("Quite likely", 8, true),
    HMMM("Hmmm....", 7, false),
    GAMBLE("Gamble", 6, false),
    RISKY("Risky", 5, false),
    PLAYING_WITH_FIRE("Playing with fire", 4, false),
    RATHER_DETRIMENTAL("Rather detrimental", 3, false),
    SUICIDE_MISSION("Suicide mission", 2, false),
    IMPOSSIBLE("Impossible", 1, false),
    UNKNOWN("Unknown", 0, false);

    private final String label;
    private final int safetyRank;
    private final boolean acceptedByAutomation;

    Probability(String label, int safetyRank, boolean acceptedByAutomation) {
        this.label = label;
        this.safetyRank = safetyRank;
        this.acceptedByAutomation = acceptedByAutomation;
    }

    public static Probability fromLabel(String label) {
        if (label == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(value -> value.label.equalsIgnoreCase(label.trim()))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public String label() { return label; }
    public int safetyRank() { return safetyRank; }
    public boolean acceptedByAutomation() { return acceptedByAutomation; }
}
