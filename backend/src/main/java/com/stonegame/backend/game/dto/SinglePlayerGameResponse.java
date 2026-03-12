package com.stonegame.backend.game.dto;

import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.Move;

/**
 * Response payload for a single-player round.
 */
public record SinglePlayerGameResponse(
        String gameId,
        Move playerMove,
        Move computerMove,
        GameResult result
) {
}
