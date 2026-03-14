package com.stonegame.backend.game.service;

import com.stonegame.backend.common.UnauthorizedException;
import com.stonegame.backend.game.dto.MultiplayerEventResponse;
import com.stonegame.backend.game.dto.MultiplayerMatchResponse;
import com.stonegame.backend.game.model.*;
import com.stonegame.backend.game.repository.MultiplayerMatchRepository;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MultiplayerService}.
 */
class MultiplayerServiceTest {

    private final MultiplayerMatchRepository multiplayerMatchRepository = mock(MultiplayerMatchRepository.class);
    private final GameRulesService gameRulesService = new GameRulesService();
    private final MultiplayerNotificationService multiplayerNotificationService = mock(MultiplayerNotificationService.class);

    private final MultiplayerService multiplayerService =
            new MultiplayerService(multiplayerMatchRepository, gameRulesService, multiplayerNotificationService);

    @Test
    @DisplayName("getMatch should return match state for authorized player")
    void getMatch_shouldReturnMatchStateForAuthorizedPlayer() {
        User user = buildUser("user-1", "john", "john@example.com");
        MultiplayerMatch match = buildMatch();

        when(multiplayerMatchRepository.findById("match-1")).thenReturn(Optional.of(match));

        MultiplayerMatchResponse response = multiplayerService.getMatch("match-1", user);

        assertNotNull(response);
        assertEquals("match-1", response.matchId());
        assertEquals("john", response.playerOneUsername());
        assertEquals("jane", response.playerTwoUsername());
        assertEquals(MatchStatus.WAITING_FOR_MOVES, response.status());
        assertNull(response.result());
    }

    @Test
    @DisplayName("getMatch should throw when user is not allowed")
    void getMatch_shouldThrowWhenUserNotAllowed() {
        User user = buildUser("user-3", "mark", "mark@example.com");
        MultiplayerMatch match = buildMatch();

        when(multiplayerMatchRepository.findById("match-1")).thenReturn(Optional.of(match));

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> multiplayerService.getMatch("match-1", user)
        );

        assertEquals("User not allowed to access this match", ex.getMessage());
    }

    @Test
    @DisplayName("submitMove should store first player move and notify topic")
    void submitMove_shouldStoreFirstPlayerMoveAndNotifyTopic() {
        User user = buildUser("user-1", "john", "john@example.com");
        MultiplayerMatch match = buildMatch();

        MultiplayerMatch savedMatch = buildMatch();
        savedMatch.setPlayerOneMove(Move.STONE);

        when(multiplayerMatchRepository.findById("match-1")).thenReturn(Optional.of(match));
        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class))).thenReturn(savedMatch);

        MultiplayerMatchResponse response = multiplayerService.submitMove("match-1", user, Move.STONE);

        assertNotNull(response);
        assertEquals(Move.STONE, response.playerOneMove());
        assertNull(response.playerTwoMove());
        assertEquals(MatchStatus.WAITING_FOR_MOVES, response.status());

        verify(multiplayerNotificationService).sendMatchUpdateToTopic(
                eq("match-1"),
                argThat(event -> {
                    MultiplayerEventResponse e = (MultiplayerEventResponse) event;
                    return "MOVE_SUBMITTED".equals(e.type()) &&
                            "match-1".equals(e.matchId());
                })
        );
    }

    @Test
    @DisplayName("submitMove should complete match when both moves are present")
    void submitMove_shouldCompleteMatchWhenBothMovesPresent() {
        User user = buildUser("user-2", "jane", "jane@example.com");

        MultiplayerMatch existing = buildMatch();
        existing.setPlayerOneMove(Move.STONE);

        MultiplayerMatch saved = buildMatch();
        saved.setPlayerOneMove(Move.STONE);
        saved.setPlayerTwoMove(Move.SCISSORS);
        saved.setStatus(MatchStatus.COMPLETED);
        saved.setResult(MultiplayerResult.PLAYER_ONE_WIN);

        when(multiplayerMatchRepository.findById("match-1")).thenReturn(Optional.of(existing));
        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class))).thenReturn(saved);

        MultiplayerMatchResponse response = multiplayerService.submitMove("match-1", user, Move.SCISSORS);

        assertNotNull(response);
        assertEquals(MatchStatus.COMPLETED, response.status());
        assertEquals(MultiplayerResult.PLAYER_ONE_WIN, response.result());
        assertEquals(Move.STONE, response.playerOneMove());
        assertEquals(Move.SCISSORS, response.playerTwoMove());

        verify(multiplayerNotificationService).sendMatchUpdateToTopic(
                eq("match-1"),
                argThat(event -> {
                    MultiplayerEventResponse e = (MultiplayerEventResponse) event;
                    return "MATCH_COMPLETED".equals(e.type()) &&
                            "match-1".equals(e.matchId());
                })
        );
    }

    @Test
    @DisplayName("submitMove should reject duplicate move from player one")
    void submitMove_shouldRejectDuplicateMoveFromPlayerOne() {
        User user = buildUser("user-1", "john", "john@example.com");
        MultiplayerMatch match = buildMatch();
        match.setPlayerOneMove(Move.PAPER);

        when(multiplayerMatchRepository.findById("match-1")).thenReturn(Optional.of(match));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> multiplayerService.submitMove("match-1", user, Move.STONE)
        );

        assertEquals("Player one already submitted a move", ex.getMessage());
        verify(multiplayerMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitMove should reject match not ready for moves")
    void submitMove_shouldRejectMatchNotReadyForMoves() {
        User user = buildUser("user-1", "john", "john@example.com");
        MultiplayerMatch match = buildMatch();
        match.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        when(multiplayerMatchRepository.findById("match-1")).thenReturn(Optional.of(match));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> multiplayerService.submitMove("match-1", user, Move.STONE)
        );

        assertEquals("Match is not ready for moves", ex.getMessage());
    }

    private User buildUser(String id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.USER);
        return user;
    }

    private MultiplayerMatch buildMatch() {
        MultiplayerMatch match = new MultiplayerMatch();
        match.setId("match-1");
        match.setPlayerOneId("user-1");
        match.setPlayerOneUsername("john");
        match.setPlayerTwoId("user-2");
        match.setPlayerTwoUsername("jane");
        match.setStatus(MatchStatus.WAITING_FOR_MOVES);
        match.setCreatedAt(Instant.now());
        return match;
    }
}
