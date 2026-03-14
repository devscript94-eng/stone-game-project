package com.stonegame.backend.game.dto;

import com.stonegame.backend.game.model.MatchStatus;
import com.stonegame.backend.game.model.Move;
import com.stonegame.backend.game.model.MultiplayerResult;

/**
 * Response payload representing multiplayer match state.
 *
 * @param matchId match identifier
 * @param playerOneUsername first player username
 * @param playerTwoUsername second player username
 * @param playerOneMove first player move
 * @param playerTwoMove second player move
 * @param status match status
 * @param result final result
 */
public record MultiplayerMatchResponse(
        String matchId,
        String playerOneUsername,
        String playerTwoUsername,
        Move playerOneMove,
        Move playerTwoMove,
        MatchStatus status,
        MultiplayerResult result
) {
}
