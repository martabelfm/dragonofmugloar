package com.mugloar.application;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String gameId) {
        super("Game '%s' is not known by this application.".formatted(gameId));
    }
}
