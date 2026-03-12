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
        user.setEmail("john@example.com");
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
}
