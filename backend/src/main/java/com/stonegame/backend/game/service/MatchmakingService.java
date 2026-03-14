package com.stonegame.backend.game.service;

import com.stonegame.backend.game.dto.JoinMatchResponse;
import com.stonegame.backend.game.dto.MultiplayerEventResponse;
import com.stonegame.backend.game.model.MatchStatus;
import com.stonegame.backend.game.model.MultiplayerMatch;
import com.stonegame.backend.game.repository.MultiplayerMatchRepository;
import com.stonegame.backend.user.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for multiplayer matchmaking.
 */
@Service
public class MatchmakingService {

    private final MultiplayerMatchRepository multiplayerMatchRepository;
    private final MultiplayerNotificationService multiplayerNotificationService;
    private final AtomicReference<User> waitingPlayer = new AtomicReference<>(null);

    public MatchmakingService(MultiplayerMatchRepository multiplayerMatchRepository,
                              MultiplayerNotificationService multiplayerNotificationService) {
        this.multiplayerMatchRepository = multiplayerMatchRepository;
        this.multiplayerNotificationService = multiplayerNotificationService;
    }

    /**
     * Places a player in the queue or matches them with another waiting player.
     *
     * @param user authenticated user
     * @return matchmaking response
     */
    public synchronized JoinMatchResponse join(User user) {
        User queuedPlayer = waitingPlayer.get();

        if (queuedPlayer == null) {
            waitingPlayer.set(user);

            MultiplayerMatch match = new MultiplayerMatch();
            match.setPlayerOneId(user.getId());
            match.setPlayerOneUsername(user.getUsername());
            match.setStatus(MatchStatus.WAITING_FOR_PLAYER);
            match.setCreatedAt(Instant.now());

            MultiplayerMatch saved = multiplayerMatchRepository.save(match);

            JoinMatchResponse response = new JoinMatchResponse(
                    saved.getId(),
                    MatchStatus.WAITING_FOR_PLAYER,
                    "Waiting for another player"
            );

            multiplayerNotificationService.sendMatchUpdateToUser(
                    user.getUsername(),
                    new MultiplayerEventResponse("WAITING_FOR_PLAYER", saved.getId(), response)
            );

            return response;
        }

        if (queuedPlayer.getId().equals(user.getId())) {
            MultiplayerMatch match = new MultiplayerMatch();
            match.setPlayerOneId(user.getId());
            match.setPlayerOneUsername(user.getUsername());
            match.setStatus(MatchStatus.WAITING_FOR_PLAYER);
            match.setCreatedAt(Instant.now());

            MultiplayerMatch saved = multiplayerMatchRepository.save(match);

            JoinMatchResponse response = new JoinMatchResponse(
                    saved.getId(),
                    MatchStatus.WAITING_FOR_PLAYER,
                    "Already waiting for another player"
            );

            multiplayerNotificationService.sendMatchUpdateToUser(
                    user.getUsername(),
                    new MultiplayerEventResponse("WAITING_FOR_PLAYER", saved.getId(), response)
            );

            return response;
        }

        waitingPlayer.set(null);

        MultiplayerMatch match = new MultiplayerMatch();
        match.setPlayerOneId(queuedPlayer.getId());
        match.setPlayerOneUsername(queuedPlayer.getUsername());
        match.setPlayerTwoId(user.getId());
        match.setPlayerTwoUsername(user.getUsername());
        match.setStatus(MatchStatus.WAITING_FOR_MOVES);
        match.setCreatedAt(Instant.now());

        MultiplayerMatch saved = multiplayerMatchRepository.save(match);

        JoinMatchResponse response = new JoinMatchResponse(
                saved.getId(),
                MatchStatus.WAITING_FOR_MOVES,
                "Match found"
        );

        MultiplayerEventResponse event = new MultiplayerEventResponse(
                "MATCH_FOUND",
                saved.getId(),
                response
        );

        multiplayerNotificationService.sendMatchUpdateToUser(queuedPlayer.getUsername(), event);
        multiplayerNotificationService.sendMatchUpdateToUser(user.getUsername(), event);

        return response;
    }
}
