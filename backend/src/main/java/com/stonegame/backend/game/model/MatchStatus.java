package com.stonegame.backend.game.model;

/**
 * Current lifecycle status of a multiplayer match.
 */
public enum MatchStatus {
    WAITING_FOR_PLAYER,
    WAITING_FOR_MOVES,
    COMPLETED
}
