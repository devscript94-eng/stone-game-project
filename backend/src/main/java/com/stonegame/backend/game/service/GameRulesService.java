package com.stonegame.backend.game.service;

import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.Move;
import org.springframework.stereotype.Service;

/**
 * Service responsible for evaluating game rules.
 */
@Service
public class GameRulesService {

    /**
     * Determines the game result from the player's perspective.
     *
     * @param playerMove player move
     * @param opponentMove opponent move
     * @return round result
     */
    public GameResult evaluate(Move playerMove, Move opponentMove) {
        if (playerMove == null || opponentMove == null) {
            throw new IllegalArgumentException("Moves must not be null");
        }

        if (playerMove == opponentMove) {
            return GameResult.DRAW;
        }

        return switch (playerMove) {
            case STONE -> opponentMove == Move.SCISSORS ? GameResult.WIN : GameResult.LOSE;
            case PAPER -> opponentMove == Move.STONE ? GameResult.WIN : GameResult.LOSE;
            case SCISSORS -> opponentMove == Move.PAPER ? GameResult.WIN : GameResult.LOSE;
        };
    }
}
