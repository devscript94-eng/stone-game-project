package com.stonegame.backend.game.dto;

import com.stonegame.backend.user.model.User;

/**
 * Waiting player entry for matchmaking.
 *
 * @param user waiting user
 * @param matchId waiting match identifier
 */
public record WaitingPlayer(User user, String matchId) {
}
