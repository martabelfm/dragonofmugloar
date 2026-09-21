package com.mugloar.application;

import com.mugloar.domain.*;
import java.util.List;

public interface GamePort {
    PlayerState startGame();
    List<Advertisement> getAds(String gameId);
    List<ShopItem> getShop(String gameId);
    SolveOutcome solve(String gameId, String adId);
    PurchaseOutcome purchase(String gameId, String itemId);
    Reputation investigate(String gameId);

    record SolveOutcome(boolean success, int lives, int gold, int score, int highScore, int turn, String message) {}
    record PurchaseOutcome(boolean success, int gold, int lives, int level, int turn) {}
}
