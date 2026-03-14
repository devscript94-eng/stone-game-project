package com.stonegame.backend.game.service;

import com.stonegame.backend.game.dto.JoinMatchResponse;
import com.stonegame.backend.game.dto.MultiplayerEventResponse;
import com.stonegame.backend.game.model.MatchStatus;
import com.stonegame.backend.game.model.MultiplayerMatch;
import com.stonegame.backend.game.repository.MultiplayerMatchRepository;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MatchmakingService}.
 */
class MatchmakingServiceTest {

    private final MultiplayerMatchRepository multiplayerMatchRepository = mock(MultiplayerMatchRepository.class);
    private final MultiplayerNotificationService multiplayerNotificationService = mock(MultiplayerNotificationService.class);

    private final MatchmakingService matchmakingService =
            new MatchmakingService(multiplayerMatchRepository, multiplayerNotificationService);

    @Test
    @DisplayName("join should put first player in waiting state")
    void join_shouldPutFirstPlayerInWaitingState() {
        User user = buildUser("user-1", "john", "john@example.com");

        MultiplayerMatch savedMatch = new MultiplayerMatch();
        savedMatch.setId("match-1");
        savedMatch.setPlayerOneId("user-1");
        savedMatch.setPlayerOneUsername("john");
        savedMatch.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class))).thenReturn(savedMatch);

        JoinMatchResponse response = matchmakingService.join(user);

        assertNotNull(response);
        assertEquals("match-1", response.matchId());
        assertEquals(MatchStatus.WAITING_FOR_PLAYER, response.status());
        assertEquals("Waiting for another player", response.message());

        verify(multiplayerMatchRepository).save(any(MultiplayerMatch.class));
        verify(multiplayerNotificationService).sendMatchUpdateToUser(
                eq("john"),
                argThat(event -> {
                    MultiplayerEventResponse e = (MultiplayerEventResponse) event;
                    return "WAITING_FOR_PLAYER".equals(e.type()) &&
                            "match-1".equals(e.matchId());
                })
        );
    }

    @Test
    @DisplayName("join should match second player with waiting player")
    void join_shouldMatchSecondPlayerWithWaitingPlayer() {
        User user1 = buildUser("user-1", "john", "john@example.com");
        User user2 = buildUser("user-2", "jane", "jane@example.com");

        MultiplayerMatch waitingMatch = new MultiplayerMatch();
        waitingMatch.setId("match-1");
        waitingMatch.setPlayerOneId("user-1");
        waitingMatch.setPlayerOneUsername("john");
        waitingMatch.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        MultiplayerMatch matched = new MultiplayerMatch();
        matched.setId("match-2");
        matched.setPlayerOneId("user-1");
        matched.setPlayerOneUsername("john");
        matched.setPlayerTwoId("user-2");
        matched.setPlayerTwoUsername("jane");
        matched.setStatus(MatchStatus.WAITING_FOR_MOVES);

        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class)))
                .thenReturn(waitingMatch)
                .thenReturn(matched);

        matchmakingService.join(user1);
        JoinMatchResponse response = matchmakingService.join(user2);

        assertNotNull(response);
        assertEquals("match-2", response.matchId());
        assertEquals(MatchStatus.WAITING_FOR_MOVES, response.status());
        assertEquals("Match found", response.message());

        verify(multiplayerNotificationService).sendMatchUpdateToUser(
                eq("john"),
                argThat(event -> {
                    MultiplayerEventResponse e = (MultiplayerEventResponse) event;
                    return "MATCH_FOUND".equals(e.type()) &&
                            "match-2".equals(e.matchId());
                })
        );

        verify(multiplayerNotificationService).sendMatchUpdateToUser(
                eq("jane"),
                argThat(event -> {
                    MultiplayerEventResponse e = (MultiplayerEventResponse) event;
                    return "MATCH_FOUND".equals(e.type()) &&
                            "match-2".equals(e.matchId());
                })
        );
    }

    @Test
    @DisplayName("join should keep same player waiting if already queued")
    void join_shouldKeepSamePlayerWaitingIfAlreadyQueued() {
        User user = buildUser("user-1", "john", "john@example.com");

        MultiplayerMatch firstSaved = new MultiplayerMatch();
        firstSaved.setId("match-1");
        firstSaved.setPlayerOneId("user-1");
        firstSaved.setPlayerOneUsername("john");
        firstSaved.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        MultiplayerMatch secondSaved = new MultiplayerMatch();
        secondSaved.setId("match-2");
        secondSaved.setPlayerOneId("user-1");
        secondSaved.setPlayerOneUsername("john");
        secondSaved.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class)))
                .thenReturn(firstSaved)
                .thenReturn(secondSaved);

        matchmakingService.join(user);
        JoinMatchResponse response = matchmakingService.join(user);

        assertNotNull(response);
        assertEquals("match-2", response.matchId());
        assertEquals(MatchStatus.WAITING_FOR_PLAYER, response.status());
        assertEquals("Already waiting for another player", response.message());

        verify(multiplayerNotificationService, times(2)).sendMatchUpdateToUser(eq("john"), any());
    }

    private User buildUser(String id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.USER);
        return user;
    }
}
