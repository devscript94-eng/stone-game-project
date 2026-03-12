package com.stonegame.backend.game.service;

import com.stonegame.backend.game.dto.SinglePlayerGameResponse;
import com.stonegame.backend.game.model.GameMode;
import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.Move;
import com.stonegame.backend.game.model.SinglePlayerGame;
import com.stonegame.backend.game.repository.SinglePlayerGameRepository;
import com.stonegame.backend.user.model.User;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Service responsible for single-player game execution.
 */
@Service
public class SinglePlayerGameService {
    private static final Logger log = LoggerFactory.getLogger(SinglePlayerGameService.class);

    private final GameRulesService gameRulesService;
    private final ComputerMoveStrategy computerMoveStrategy;
    private final SinglePlayerGameRepository singlePlayerGameRepository;

    public SinglePlayerGameService(GameRulesService gameRulesService,
                                   ComputerMoveStrategy computerMoveStrategy,
                                   SinglePlayerGameRepository singlePlayerGameRepository) {
        this.gameRulesService = gameRulesService;
        this.computerMoveStrategy = computerMoveStrategy;
        this.singlePlayerGameRepository = singlePlayerGameRepository;
    }

    /**
     * Plays a single-player round against the computer.
     *
     * @param user authenticated user
     * @param playerMove move chosen by the player
     * @return round response
     */
    public SinglePlayerGameResponse play(User user, Move playerMove) {
        log.info("Single-player game started userId={} move={}", user.getId(), playerMove);
        Move computerMove = computerMoveStrategy.nextMove();
        GameResult result = gameRulesService.evaluate(playerMove, computerMove);

        SinglePlayerGame game = new SinglePlayerGame(
                user.getId(),
                playerMove,
                computerMove,
                result,
                GameMode.SINGLE_PLAYER,
                Instant.now()
        );

        SinglePlayerGame savedGame = singlePlayerGameRepository.save(game);

        log.info("Single-player game resolved userId={} playerMove={} computerMove={} result={}",
                user.getId(), playerMove, computerMove, result);
        return new SinglePlayerGameResponse(
                savedGame.getId(),
                savedGame.getPlayerMove(),
                savedGame.getComputerMove(),
                savedGame.getResult()
        );
    }
}
