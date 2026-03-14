package com.stonegame.backend.game.dto;

import com.stonegame.backend.game.model.MatchStatus;

/**
 * Response returned when a player joins multiplayer matchmaking.
 *
 * @param matchId match identifier
 * @param status current match status
 * @param message additional information
 */
public record JoinMatchResponse(
        String matchId,
        MatchStatus status,
        String message
) {
}
