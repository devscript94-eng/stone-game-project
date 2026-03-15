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

import java.util.Optional;

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
        verifyNoInteractions(multiplayerNotificationService);
    }

    @Test
    @DisplayName("join should return same waiting match if same player joins again")
    void join_shouldKeepSamePlayerWaitingIfAlreadyQueued() {
        User user = buildUser("user-1", "john", "john@example.com");

        MultiplayerMatch savedMatch = new MultiplayerMatch();
        savedMatch.setId("match-1");
        savedMatch.setPlayerOneId("user-1");
        savedMatch.setPlayerOneUsername("john");
        savedMatch.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class))).thenReturn(savedMatch);

        matchmakingService.join(user);
        JoinMatchResponse response = matchmakingService.join(user);

        assertNotNull(response);
        assertEquals("match-1", response.matchId());
        assertEquals(MatchStatus.WAITING_FOR_PLAYER, response.status());
        assertEquals("Already waiting for another player", response.message());

        verify(multiplayerMatchRepository, times(1)).save(any(MultiplayerMatch.class));
        verifyNoInteractions(multiplayerNotificationService);
    }

    @Test
    @DisplayName("join should match second player with waiting player using same match")
    void join_shouldMatchSecondPlayerWithWaitingPlayer() {
        User user1 = buildUser("user-1", "john", "john@example.com");
        User user2 = buildUser("user-2", "jane", "jane@example.com");

        MultiplayerMatch waitingMatch = new MultiplayerMatch();
        waitingMatch.setId("match-1");
        waitingMatch.setPlayerOneId("user-1");
        waitingMatch.setPlayerOneUsername("john");
        waitingMatch.setStatus(MatchStatus.WAITING_FOR_PLAYER);

        MultiplayerMatch completedMatch = new MultiplayerMatch();
        completedMatch.setId("match-1");
        completedMatch.setPlayerOneId("user-1");
        completedMatch.setPlayerOneUsername("john");
        completedMatch.setPlayerTwoId("user-2");
        completedMatch.setPlayerTwoUsername("jane");
        completedMatch.setStatus(MatchStatus.WAITING_FOR_MOVES);

        when(multiplayerMatchRepository.save(any(MultiplayerMatch.class)))
                .thenReturn(waitingMatch)
                .thenReturn(completedMatch);

        when(multiplayerMatchRepository.findById("match-1"))
                .thenReturn(Optional.of(waitingMatch));

        matchmakingService.join(user1);
        JoinMatchResponse response = matchmakingService.join(user2);

        assertNotNull(response);
        assertEquals("match-1", response.matchId());
        assertEquals(MatchStatus.WAITING_FOR_MOVES, response.status());
        assertEquals("Match found", response.message());

        verify(multiplayerNotificationService).sendMatchUpdateToTopic(
                eq("match-1"),
                argThat(event -> {
                    MultiplayerEventResponse e = (MultiplayerEventResponse) event;
                    return "MATCH_FOUND".equals(e.type()) &&
                            "match-1".equals(e.matchId());
                })
        );
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
