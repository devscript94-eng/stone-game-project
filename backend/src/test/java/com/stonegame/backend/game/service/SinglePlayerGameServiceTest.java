package com.stonegame.backend.game.service;

import com.stonegame.backend.game.dto.SinglePlayerGameResponse;
import com.stonegame.backend.game.model.GameMode;
import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.Move;
import com.stonegame.backend.game.model.SinglePlayerGame;
import com.stonegame.backend.game.repository.SinglePlayerGameRepository;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SinglePlayerGameService}.
 */
class SinglePlayerGameServiceTest {

    private final GameRulesService gameRulesService = new GameRulesService();
    private final ComputerMoveStrategy computerMoveStrategy = mock(ComputerMoveStrategy.class);
    private final SinglePlayerGameRepository repository = mock(SinglePlayerGameRepository.class);

    private final SinglePlayerGameService service =
            new SinglePlayerGameService(gameRulesService, computerMoveStrategy, repository);

    @Test
    @DisplayName("play should persist result and return response")
    void play_shouldPersistAndReturnResponse() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("john");
        user.setEmail("john@company.com");
        user.setRole(Role.USER);

        when(computerMoveStrategy.nextMove()).thenReturn(Move.SCISSORS);

        SinglePlayerGame saved = new SinglePlayerGame(
                "user-123",
                Move.STONE,
                Move.SCISSORS,
                GameResult.WIN,
                GameMode.SINGLE_PLAYER,
                java.time.Instant.now()
        );
        saved.setId("game-123");

        when(repository.save(any(SinglePlayerGame.class))).thenReturn(saved);

        SinglePlayerGameResponse response = service.play(user, Move.STONE);

        assertNotNull(response);
        assertEquals("game-123", response.gameId());
        assertEquals(Move.STONE, response.playerMove());
        assertEquals(Move.SCISSORS, response.computerMove());
        assertEquals(GameResult.WIN, response.result());

        verify(repository).save(any(SinglePlayerGame.class));
    }

    @Test
    @DisplayName("play should return DRAW when both moves are equal")
    void play_shouldReturnDraw() {
        User user = buildUser();
        when(computerMoveStrategy.nextMove()).thenReturn(Move.STONE);

        SinglePlayerGame saved = new SinglePlayerGame(
                "user-123", Move.STONE, Move.STONE, GameResult.DRAW, GameMode.SINGLE_PLAYER, Instant.now()
        );
        saved.setId("game-1");

        when(repository.save(any(SinglePlayerGame.class))).thenReturn(saved);

        SinglePlayerGameResponse response = service.play(user, Move.STONE);

        assertEquals(GameResult.DRAW, response.result());
        assertEquals(Move.STONE, response.playerMove());
        assertEquals(Move.STONE, response.computerMove());
    }

    @Test
    @DisplayName("play should persist correct game payload")
    void play_shouldPersistCorrectPayload() {
        User user = buildUser();
        when(computerMoveStrategy.nextMove()).thenReturn(Move.PAPER);

        SinglePlayerGame saved = new SinglePlayerGame(
                "user-123", Move.STONE, Move.PAPER, GameResult.LOSE, GameMode.SINGLE_PLAYER, Instant.now()
        );
        saved.setId("game-2");

        when(repository.save(any(SinglePlayerGame.class))).thenReturn(saved);

        service.play(user, Move.STONE);

        ArgumentCaptor<SinglePlayerGame> captor = ArgumentCaptor.forClass(SinglePlayerGame.class);
        verify(repository).save(captor.capture());

        SinglePlayerGame persisted = captor.getValue();
        assertEquals("user-123", persisted.getUserId());
        assertEquals(Move.STONE, persisted.getPlayerMove());
        assertEquals(Move.PAPER, persisted.getComputerMove());
        assertEquals(GameResult.LOSE, persisted.getResult());
        assertEquals(GameMode.SINGLE_PLAYER, persisted.getGameMode());
        assertNotNull(persisted.getCreatedAt());
    }

    private User buildUser() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("john");
        user.setEmail("john@company.com");
        user.setRole(Role.USER);
        return user;
    }
}
