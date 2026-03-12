package com.stonegame.backend.game.service;

import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.Move;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameRulesService}.
 */
class GameRulesServiceTest {

    private final GameRulesService gameRulesService = new GameRulesService();

    @Test
    @DisplayName("evaluate should return draw when moves are equal")
    void evaluate_shouldReturnDraw() {
        assertEquals(GameResult.DRAW, gameRulesService.evaluate(Move.STONE, Move.STONE));
    }

    @Test
    @DisplayName("evaluate should return win when stone beats scissors")
    void evaluate_shouldReturnWinForStoneAgainstScissors() {
        assertEquals(GameResult.WIN, gameRulesService.evaluate(Move.STONE, Move.SCISSORS));
    }

    @Test
    @DisplayName("evaluate should return lose when stone loses to paper")
    void evaluate_shouldReturnLoseForStoneAgainstPaper() {
        assertEquals(GameResult.LOSE, gameRulesService.evaluate(Move.STONE, Move.PAPER));
    }

    @Test
    @DisplayName("evaluate should throw when player move is null")
    void evaluate_shouldThrowWhenPlayerMoveIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> gameRulesService.evaluate(null, Move.PAPER));
    }
}
