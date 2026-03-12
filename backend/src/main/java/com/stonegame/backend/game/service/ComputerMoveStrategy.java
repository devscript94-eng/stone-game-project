package com.stonegame.backend.game.service;

import com.stonegame.backend.game.model.Move;

/**
 * Strategy used to generate the computer move.
 */
public interface ComputerMoveStrategy {

    /**
     * Generates the next computer move.
     *
     * @return generated move
     */
    Move nextMove();
}
