package com.stonegame.backend.game.service;

import com.stonegame.backend.game.dto.JoinMatchResponse;
import com.stonegame.backend.game.dto.MultiplayerEventResponse;
import com.stonegame.backend.game.dto.MultiplayerMatchResponse;
import com.stonegame.backend.game.dto.WaitingPlayer;
import com.stonegame.backend.game.model.MatchStatus;
import com.stonegame.backend.game.model.MultiplayerMatch;
import com.stonegame.backend.game.repository.MultiplayerMatchRepository;
import com.stonegame.backend.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for multiplayer matchmaking.
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private final MultiplayerMatchRepository multiplayerMatchRepository;
    private final MultiplayerNotificationService multiplayerNotificationService;
    private final AtomicReference<WaitingPlayer> waitingPlayer = new AtomicReference<>(null);

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
        WaitingPlayer queued = waitingPlayer.get();

        if (queued == null) {
            MultiplayerMatch match = new MultiplayerMatch();
            match.setPlayerOneId(user.getId());
            match.setPlayerOneUsername(user.getUsername());
            match.setStatus(MatchStatus.WAITING_FOR_PLAYER);
            match.setCreatedAt(Instant.now());

            MultiplayerMatch saved = multiplayerMatchRepository.save(match);
            waitingPlayer.set(new WaitingPlayer(user, saved.getId()));

            return new JoinMatchResponse(
                    saved.getId(),
                    MatchStatus.WAITING_FOR_PLAYER,
                    "Waiting for another player"
            );
        }

        if (queued.user().getId().equals(user.getId())) {
            return new JoinMatchResponse(
                    queued.matchId(),
                    MatchStatus.WAITING_FOR_PLAYER,
                    "Already waiting for another player"
            );
        }

        MultiplayerMatch match = multiplayerMatchRepository.findById(queued.matchId())
                .orElseThrow(() -> new IllegalArgumentException("Waiting match not found"));

        match.setPlayerTwoId(user.getId());
        match.setPlayerTwoUsername(user.getUsername());
        match.setStatus(MatchStatus.WAITING_FOR_MOVES);

        MultiplayerMatch saved = multiplayerMatchRepository.save(match);
        waitingPlayer.set(null);

        MultiplayerMatchResponse response = new MultiplayerMatchResponse(
                saved.getId(),
                saved.getPlayerOneUsername(),
                saved.getPlayerTwoUsername(),
                saved.getPlayerOneMove(),
                saved.getPlayerTwoMove(),
                saved.getStatus(),
                saved.getResult()
        );

        log.info("############## RESPONSE : {}", response);

        multiplayerNotificationService.sendMatchUpdateToTopic(
                saved.getId(),
                new MultiplayerEventResponse(
                        "MATCH_FOUND",
                        saved.getId(),
                        response
                )
        );

        return new JoinMatchResponse(
                saved.getId(),
                MatchStatus.WAITING_FOR_MOVES,
                "Match found"
        );
    }
}