package com.stonegame.backend.game.service;

import com.stonegame.backend.game.model.Move;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Random implementation of the computer move strategy.
 */
@Component
public class RandomComputerMoveStrategy implements ComputerMoveStrategy {

    private static final Move[] MOVES = Move.values();

    /**
     * Generates a random move for the computer.
     *
     * @return random move
     */
    @Override
    public Move nextMove() {
        int index = ThreadLocalRandom.current().nextInt(MOVES.length);
        return MOVES[index];
    }
}
