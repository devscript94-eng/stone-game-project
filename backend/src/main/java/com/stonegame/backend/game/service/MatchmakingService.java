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
        log.info("Matchmaking join requested: userId={}, username={}", user.getId(), user.getUsername());
        WaitingPlayer queued = waitingPlayer.get();

        if (queued == null) {
            log.info("No player currently waiting. Creating a new waiting match for userId={}, username={}",
                    user.getId(), user.getUsername());
            MultiplayerMatch match = new MultiplayerMatch();
            match.setPlayerOneId(user.getId());
            match.setPlayerOneUsername(user.getUsername());
            match.setStatus(MatchStatus.WAITING_FOR_PLAYER);
            match.setCreatedAt(Instant.now());

            MultiplayerMatch saved = multiplayerMatchRepository.save(match);
            log.info("Waiting match created successfully: matchId={}, playerOneId={}, playerOneUsername={}, status={}",
                    saved.getId(), saved.getPlayerOneId(), saved.getPlayerOneUsername(), saved.getStatus());

            waitingPlayer.set(new WaitingPlayer(user, saved.getId()));
            log.info("User added to matchmaking queue: userId={}, username={}, matchId={}",
                    user.getId(), user.getUsername(), saved.getId());

            return new JoinMatchResponse(
                    saved.getId(),
                    MatchStatus.WAITING_FOR_PLAYER,
                    "Waiting for another player"
            );
        }

        log.info("Existing waiting player found: queuedUserId={}, queuedUsername={}, matchId={}",
                queued.user().getId(), queued.user().getUsername(), queued.matchId());

        if (queued.user().getId().equals(user.getId())) {
            log.warn("User attempted to join matchmaking while already waiting: userId={}, username={}, matchId={}",
                    user.getId(), user.getUsername(), queued.matchId());
            return new JoinMatchResponse(
                    queued.matchId(),
                    MatchStatus.WAITING_FOR_PLAYER,
                    "Already waiting for another player"
            );
        }

        log.info("Matching players: playerOneId={}, playerOneUsername={}, playerTwoId={}, playerTwoUsername={}, matchId={}",
                queued.user().getId(), queued.user().getUsername(), user.getId(), user.getUsername(), queued.matchId());

        MultiplayerMatch match = multiplayerMatchRepository.findById(queued.matchId())
                .orElseThrow(() -> {
                    log.error("Waiting match not found in database: matchId={}, queuedUserId={}, queuedUsername={}",
                            queued.matchId(), queued.user().getId(), queued.user().getUsername());
                    return new IllegalArgumentException("Waiting match not found");
                });

        match.setPlayerTwoId(user.getId());
        match.setPlayerTwoUsername(user.getUsername());
        match.setStatus(MatchStatus.WAITING_FOR_MOVES);

        MultiplayerMatch saved = multiplayerMatchRepository.save(match);
        log.info("Match completed successfully: matchId={}, playerOneId={}, playerOneUsername={}, playerTwoId={}, playerTwoUsername={}, status={}",
                saved.getId(),
                saved.getPlayerOneId(),
                saved.getPlayerOneUsername(),
                saved.getPlayerTwoId(),
                saved.getPlayerTwoUsername(),
                saved.getStatus());

        waitingPlayer.set(null);
        log.info("Matchmaking queue cleared after successful match: matchId={}", saved.getId());

        MultiplayerMatchResponse response = new MultiplayerMatchResponse(
                saved.getId(),
                saved.getPlayerOneUsername(),
                saved.getPlayerTwoUsername(),
                saved.getPlayerOneMove(),
                saved.getPlayerTwoMove(),
                saved.getStatus(),
                saved.getResult()
        );

        log.info("Sending MATCH_FOUND notification: matchId={}, playerOneUsername={}, playerTwoUsername={}",
                saved.getId(), saved.getPlayerOneUsername(), saved.getPlayerTwoUsername());

        multiplayerNotificationService.sendMatchUpdateToTopic(
                saved.getId(),
                new MultiplayerEventResponse(
                        "MATCH_FOUND",
                        saved.getId(),
                        response
                )
        );

        log.info("MATCH_FOUND notification sent successfully: matchId={}", saved.getId());
        return new JoinMatchResponse(
                saved.getId(),
                MatchStatus.WAITING_FOR_MOVES,
                "Match found"
        );
    }
}