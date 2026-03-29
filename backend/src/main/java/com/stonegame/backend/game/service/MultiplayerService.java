package com.stonegame.backend.game.service;

import com.stonegame.backend.common.UnauthorizedException;
import com.stonegame.backend.game.dto.MultiplayerEventResponse;
import com.stonegame.backend.game.dto.MultiplayerMatchResponse;
import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.MatchStatus;
import com.stonegame.backend.game.model.Move;
import com.stonegame.backend.game.model.MultiplayerMatch;
import com.stonegame.backend.game.model.MultiplayerResult;
import com.stonegame.backend.game.repository.MultiplayerMatchRepository;
import com.stonegame.backend.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for multiplayer match lifecycle.
 */
@Service
public class MultiplayerService {

    private static final Logger log = LoggerFactory.getLogger(MultiplayerService.class);

    private static final String EVENT_MATCH_COMPLETED = "MATCH_COMPLETED";
    private static final String EVENT_MOVE_SUBMITTED = "MOVE_SUBMITTED";

    private final MultiplayerMatchRepository multiplayerMatchRepository;
    private final GameRulesService gameRulesService;
    private final MultiplayerNotificationService multiplayerNotificationService;

    public MultiplayerService(MultiplayerMatchRepository multiplayerMatchRepository,
                              GameRulesService gameRulesService,
                              MultiplayerNotificationService multiplayerNotificationService) {
        this.multiplayerMatchRepository = multiplayerMatchRepository;
        this.gameRulesService = gameRulesService;
        this.multiplayerNotificationService = multiplayerNotificationService;
    }

    /**
     * Returns the current state of a match.
     *
     * @param matchId match identifier
     * @param user authenticated user
     * @return match state
     */
    public MultiplayerMatchResponse getMatch(String matchId, User user) {
        log.info("Match state requested: matchId={}, userId={}", matchId, user.getId());

        MultiplayerMatch match = findAuthorizedMatch(matchId, user);
        MultiplayerMatchResponse response = toResponse(match);

        log.info("Match state returned: matchId={}, userId={}, status={}, result={}",
                match.getId(), user.getId(), match.getStatus(), match.getResult());

        return response;
    }

    /**
     * Submits a move for the authenticated player.
     *
     * @param matchId match identifier
     * @param user authenticated user
     * @param move player move
     * @return updated match state
     */
    public MultiplayerMatchResponse submitMove(String matchId, User user, Move move) {
        log.info("Move submission requested: matchId={}, userId={}, move={}", matchId, user.getId(), move);

        MultiplayerMatch match = findAuthorizedMatch(matchId, user);

        validateMatchIsReadyForMoves(match, user);
        applyPlayerMove(match, user, move);
        completeMatchIfReady(match);

        MultiplayerMatch savedMatch = saveMatch(match);
        MultiplayerMatchResponse response = toResponse(savedMatch);

        publishMatchEvent(savedMatch, response);

        return response;
    }

    private MultiplayerMatch findAuthorizedMatch(String matchId, User user) {
        MultiplayerMatch match = loadMatch(matchId, user.getId());
        ensureUserCanAccessMatch(match, user);
        return match;
    }

    private MultiplayerMatch loadMatch(String matchId, String userId) {
        return multiplayerMatchRepository.findById(matchId)
                .orElseThrow(() -> {
                    log.warn("Match not found: matchId={}, userId={}", matchId, userId);
                    return new IllegalArgumentException("Match not found");
                });
    }

    private void ensureUserCanAccessMatch(MultiplayerMatch match, User user) {
        boolean isAllowed = isPlayerOne(match, user) || isPlayerTwo(match, user);

        if (!isAllowed) {
            log.warn("Unauthorized match access attempt: matchId={}, userId={}, playerOneId={}, playerTwoId={}",
                    match.getId(), user.getId(), match.getPlayerOneId(), match.getPlayerTwoId());
            throw new UnauthorizedException("User not allowed to access this match");
        }
    }

    private void validateMatchIsReadyForMoves(MultiplayerMatch match, User user) {
        if (match.getStatus() != MatchStatus.WAITING_FOR_MOVES) {
            log.warn("Move submission rejected: match not ready for moves, matchId={}, userId={}, status={}",
                    match.getId(), user.getId(), match.getStatus());
            throw new IllegalArgumentException("Match is not ready for moves");
        }
    }

    private void applyPlayerMove(MultiplayerMatch match, User user, Move move) {
        if (isPlayerOne(match, user)) {
            applyPlayerOneMove(match, user, move);
            return;
        }

        if (isPlayerTwo(match, user)) {
            applyPlayerTwoMove(match, user, move);
        }
    }

    private void applyPlayerOneMove(MultiplayerMatch match, User user, Move move) {
        if (match.getPlayerOneMove() != null) {
            log.warn("Duplicate move submission rejected for player one: matchId={}, userId={}, existingMove={}",
                    match.getId(), user.getId(), match.getPlayerOneMove());
            throw new IllegalArgumentException("Player one already submitted a move");
        }

        match.setPlayerOneMove(move);
        log.info("Player one move recorded: matchId={}, userId={}, move={}",
                match.getId(), user.getId(), move);
    }

    private void applyPlayerTwoMove(MultiplayerMatch match, User user, Move move) {
        if (match.getPlayerTwoMove() != null) {
            log.warn("Duplicate move submission rejected for player two: matchId={}, userId={}, existingMove={}",
                    match.getId(), user.getId(), match.getPlayerTwoMove());
            throw new IllegalArgumentException("Player two already submitted a move");
        }

        match.setPlayerTwoMove(move);
        log.info("Player two move recorded: matchId={}, userId={}, move={}",
                match.getId(), user.getId(), move);
    }

    private void completeMatchIfReady(MultiplayerMatch match) {
        if (!bothMovesSubmitted(match)) {
            log.info("Move recorded, waiting for second player: matchId={}, playerOneMove={}, playerTwoMove={}",
                    match.getId(), match.getPlayerOneMove(), match.getPlayerTwoMove());
            return;
        }

        log.info("Both moves submitted. Evaluating match result: matchId={}, playerOneMove={}, playerTwoMove={}",
                match.getId(), match.getPlayerOneMove(), match.getPlayerTwoMove());

        MultiplayerResult result = evaluateMultiplayerResult(match);
        match.setResult(result);
        match.setStatus(MatchStatus.COMPLETED);

        log.info("Match completed: matchId={}, result={}", match.getId(), result);
    }

    private MultiplayerResult evaluateMultiplayerResult(MultiplayerMatch match) {
        GameResult playerOneResult = gameRulesService.evaluate(
                match.getPlayerOneMove(),
                match.getPlayerTwoMove()
        );

        return switch (playerOneResult) {
            case WIN -> MultiplayerResult.PLAYER_ONE_WIN;
            case LOSE -> MultiplayerResult.PLAYER_TWO_WIN;
            case DRAW -> MultiplayerResult.DRAW;
        };
    }

    private MultiplayerMatch saveMatch(MultiplayerMatch match) {
        MultiplayerMatch savedMatch = multiplayerMatchRepository.save(match);

        log.info("Match state saved: matchId={}, status={}, result={}, playerOneMove={}, playerTwoMove={}",
                savedMatch.getId(),
                savedMatch.getStatus(),
                savedMatch.getResult(),
                savedMatch.getPlayerOneMove(),
                savedMatch.getPlayerTwoMove());

        return savedMatch;
    }

    private void publishMatchEvent(MultiplayerMatch match, MultiplayerMatchResponse response) {
        String eventType = isCompleted(match) ? EVENT_MATCH_COMPLETED : EVENT_MOVE_SUBMITTED;

        log.info("Sending multiplayer event: eventType={}, matchId={}", eventType, match.getId());

        multiplayerNotificationService.sendMatchUpdateToTopic(
                match.getId(),
                new MultiplayerEventResponse(eventType, match.getId(), response)
        );

        log.info("Multiplayer event sent successfully: eventType={}, matchId={}", eventType, match.getId());
    }

    private boolean bothMovesSubmitted(MultiplayerMatch match) {
        return match.getPlayerOneMove() != null && match.getPlayerTwoMove() != null;
    }

    private boolean isCompleted(MultiplayerMatch match) {
        return match.getStatus() == MatchStatus.COMPLETED;
    }

    private boolean isPlayerOne(MultiplayerMatch match, User user) {
        return user.getId().equals(match.getPlayerOneId());
    }

    private boolean isPlayerTwo(MultiplayerMatch match, User user) {
        return user.getId().equals(match.getPlayerTwoId());
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