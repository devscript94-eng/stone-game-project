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

    private static final String EVENT_MATCH_FOUND = "MATCH_FOUND";
    private static final String MESSAGE_WAITING = "Waiting for another player";
    private static final String MESSAGE_ALREADY_WAITING = "Already waiting for another player";
    private static final String MESSAGE_MATCH_FOUND = "Match found";

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

        WaitingPlayer queuedPlayer = waitingPlayer.get();

        if (noPlayerWaiting(queuedPlayer)) {
            return createWaitingMatch(user);
        }

        if (isSameQueuedUser(queuedPlayer, user)) {
            return alreadyWaitingResponse(user, queuedPlayer);
        }

        return matchPlayers(user, queuedPlayer);
    }

    private JoinMatchResponse createWaitingMatch(User user) {
        log.info("No player waiting. Creating waiting match: userId={}, username={}",
                user.getId(), user.getUsername());

        MultiplayerMatch matchToCreate = buildWaitingMatch(user);
        MultiplayerMatch savedMatch = saveNewWaitingMatch(matchToCreate);

        addPlayerToQueue(user, savedMatch.getId());

        return new JoinMatchResponse(
                savedMatch.getId(),
                MatchStatus.WAITING_FOR_PLAYER,
                MESSAGE_WAITING
        );
    }

    private MultiplayerMatch buildWaitingMatch(User user) {
        MultiplayerMatch match = new MultiplayerMatch();
        match.setPlayerOneId(user.getId());
        match.setPlayerOneUsername(user.getUsername());
        match.setStatus(MatchStatus.WAITING_FOR_PLAYER);
        match.setCreatedAt(Instant.now());
        return match;
    }

    private MultiplayerMatch saveNewWaitingMatch(MultiplayerMatch match) {
        MultiplayerMatch savedMatch = multiplayerMatchRepository.save(match);

        log.info("Waiting match created: matchId={}, playerOneId={}, playerOneUsername={}, status={}",
                savedMatch.getId(),
                savedMatch.getPlayerOneId(),
                savedMatch.getPlayerOneUsername(),
                savedMatch.getStatus());

        return savedMatch;
    }

    private void addPlayerToQueue(User user, String matchId) {
        waitingPlayer.set(new WaitingPlayer(user, matchId));

        log.info("User added to matchmaking queue: userId={}, username={}, matchId={}",
                user.getId(), user.getUsername(), matchId);
    }

    private JoinMatchResponse alreadyWaitingResponse(User user, WaitingPlayer queuedPlayer) {
        log.warn("User already waiting in matchmaking queue: userId={}, username={}, matchId={}",
                user.getId(), user.getUsername(), queuedPlayer.matchId());

        return new JoinMatchResponse(
                queuedPlayer.matchId(),
                MatchStatus.WAITING_FOR_PLAYER,
                MESSAGE_ALREADY_WAITING
        );
    }

    private JoinMatchResponse matchPlayers(User joiningUser, WaitingPlayer queuedPlayer) {
        log.info("Matching players: playerOneId={}, playerOneUsername={}, playerTwoId={}, playerTwoUsername={}, matchId={}",
                queuedPlayer.user().getId(),
                queuedPlayer.user().getUsername(),
                joiningUser.getId(),
                joiningUser.getUsername(),
                queuedPlayer.matchId());

        MultiplayerMatch existingMatch = loadWaitingMatch(queuedPlayer);
        updateMatchWithSecondPlayer(existingMatch, joiningUser);

        MultiplayerMatch savedMatch = saveMatchedPlayers(existingMatch);
        clearQueue(savedMatch.getId());

        MultiplayerMatchResponse response = toResponse(savedMatch);
        notifyMatchFound(savedMatch, response);

        return new JoinMatchResponse(
                savedMatch.getId(),
                MatchStatus.WAITING_FOR_MOVES,
                MESSAGE_MATCH_FOUND
        );
    }

    private MultiplayerMatch loadWaitingMatch(WaitingPlayer queuedPlayer) {
        return multiplayerMatchRepository.findById(queuedPlayer.matchId())
                .orElseThrow(() -> {
                    log.error("Waiting match not found: matchId={}, queuedUserId={}, queuedUsername={}",
                            queuedPlayer.matchId(),
                            queuedPlayer.user().getId(),
                            queuedPlayer.user().getUsername());
                    return new IllegalArgumentException("Waiting match not found");
                });
    }

    private void updateMatchWithSecondPlayer(MultiplayerMatch match, User user) {
        match.setPlayerTwoId(user.getId());
        match.setPlayerTwoUsername(user.getUsername());
        match.setStatus(MatchStatus.WAITING_FOR_MOVES);
    }

    private MultiplayerMatch saveMatchedPlayers(MultiplayerMatch match) {
        MultiplayerMatch savedMatch = multiplayerMatchRepository.save(match);

        log.info("Players matched successfully: matchId={}, playerOneId={}, playerOneUsername={}, playerTwoId={}, playerTwoUsername={}, status={}",
                savedMatch.getId(),
                savedMatch.getPlayerOneId(),
                savedMatch.getPlayerOneUsername(),
                savedMatch.getPlayerTwoId(),
                savedMatch.getPlayerTwoUsername(),
                savedMatch.getStatus());

        return savedMatch;
    }

    private void clearQueue(String matchId) {
        waitingPlayer.set(null);
        log.info("Matchmaking queue cleared: matchId={}", matchId);
    }

    private void notifyMatchFound(MultiplayerMatch match, MultiplayerMatchResponse response) {
        log.info("Sending match found notification: matchId={}, playerOneUsername={}, playerTwoUsername={}",
                match.getId(), match.getPlayerOneUsername(), match.getPlayerTwoUsername());

        multiplayerNotificationService.sendMatchUpdateToTopic(
                match.getId(),
                new MultiplayerEventResponse(EVENT_MATCH_FOUND, match.getId(), response)
        );

        log.info("Match found notification sent successfully: matchId={}", match.getId());
    }

    private boolean noPlayerWaiting(WaitingPlayer queuedPlayer) {
        return queuedPlayer == null;
    }

    private boolean isSameQueuedUser(WaitingPlayer queuedPlayer, User user) {
        return queuedPlayer.user().getId().equals(user.getId());
    }

    private MultiplayerMatchResponse toResponse(MultiplayerMatch match) {
        return new MultiplayerMatchResponse(
                match.getId(),
                match.getPlayerOneUsername(),
                match.getPlayerTwoUsername(),
                match.getPlayerOneMove(),
                match.getPlayerTwoMove(),
                match.getStatus(),
                match.getResult()
        );
    }
}