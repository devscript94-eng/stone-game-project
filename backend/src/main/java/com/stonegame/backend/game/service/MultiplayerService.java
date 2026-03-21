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
        log.info("Match state requested: matchId={}, userId={}, username={}",
                matchId, user.getId(), user.getUsername());

        MultiplayerMatch match = findAuthorizedMatch(matchId, user);

        log.info("Match state returned successfully: matchId={}, userId={}, status={}, playerOneMove={}, playerTwoMove={}, result={}",
                match.getId(),
                user.getId(),
                match.getStatus(),
                match.getPlayerOneMove(),
                match.getPlayerTwoMove(),
                match.getResult());

        return toResponse(match);
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
        log.info("Move submission requested: matchId={}, userId={}, username={}, move={}",
                matchId, user.getId(), user.getUsername(), move);

        MultiplayerMatch match = findAuthorizedMatch(matchId, user);

        if (match.getStatus() != MatchStatus.WAITING_FOR_MOVES) {
            log.warn("Move submission rejected because match is not ready: matchId={}, userId={}, currentStatus={}",
                    matchId, user.getId(), match.getStatus());
            throw new IllegalArgumentException("Match is not ready for moves");
        }

        if (user.getId().equals(match.getPlayerOneId())) {
            if (match.getPlayerOneMove() != null) {
                log.warn("Duplicate move submission rejected for player one: matchId={}, userId={}, existingMove={}",
                        matchId, user.getId(), match.getPlayerOneMove());
                throw new IllegalArgumentException("Player one already submitted a move");
            }
            match.setPlayerOneMove(move);
            log.info("Player one move recorded: matchId={}, userId={}, move={}",
                    matchId, user.getId(), move);
        } else if (user.getId().equals(match.getPlayerTwoId())) {
            if (match.getPlayerTwoMove() != null) {
                log.warn("Duplicate move submission rejected for player two: matchId={}, userId={}, existingMove={}",
                        matchId, user.getId(), match.getPlayerTwoMove());
                throw new IllegalArgumentException("Player two already submitted a move");
            }
            match.setPlayerTwoMove(move);
            log.info("Player two move recorded: matchId={}, userId={}, move={}",
                    matchId, user.getId(), move);
        }

        if (match.getPlayerOneMove() != null && match.getPlayerTwoMove() != null) {
            log.info("Both moves submitted. Evaluating match result: matchId={}, playerOneMove={}, playerTwoMove={}",
                    matchId, match.getPlayerOneMove(), match.getPlayerTwoMove());

            GameResult playerOneResult =
                    gameRulesService.evaluate(match.getPlayerOneMove(), match.getPlayerTwoMove());

            MultiplayerResult result = switch (playerOneResult) {
                case WIN -> MultiplayerResult.PLAYER_ONE_WIN;
                case LOSE -> MultiplayerResult.PLAYER_TWO_WIN;
                case DRAW -> MultiplayerResult.DRAW;
            };

            match.setResult(result);
            match.setStatus(MatchStatus.COMPLETED);

            log.info("Match completed: matchId={}, result={}, status={}",
                    matchId, result, match.getStatus());
        } else {
            log.info("Move recorded, waiting for the second player: matchId={}, playerOneMove={}, playerTwoMove={}, status={}",
                    matchId, match.getPlayerOneMove(), match.getPlayerTwoMove(), match.getStatus());
        }

        MultiplayerMatch saved = multiplayerMatchRepository.save(match);
        log.info("Match state saved successfully: matchId={}, status={}, result={}, playerOneMove={}, playerTwoMove={}",
                saved.getId(),
                saved.getStatus(),
                saved.getResult(),
                saved.getPlayerOneMove(),
                saved.getPlayerTwoMove());

        MultiplayerMatchResponse response = toResponse(saved);

        if (saved.getStatus() == MatchStatus.COMPLETED) {
            log.info("Sending MATCH_COMPLETED notification: matchId={}, result={}",
                    saved.getId(), saved.getResult());

            multiplayerNotificationService.sendMatchUpdateToTopic(
                    saved.getId(),
                    new MultiplayerEventResponse("MATCH_COMPLETED", saved.getId(), response)
            );

            log.info("MATCH_COMPLETED notification sent successfully: matchId={}", saved.getId());
        } else {
            log.info("Sending MOVE_SUBMITTED notification: matchId={}", saved.getId());

            multiplayerNotificationService.sendMatchUpdateToTopic(
                    saved.getId(),
                    new MultiplayerEventResponse("MOVE_SUBMITTED", saved.getId(), response)
            );

            log.info("MOVE_SUBMITTED notification sent successfully: matchId={}", saved.getId());
        }

        return response;
    }

    private MultiplayerMatch findAuthorizedMatch(String matchId, User user) {
        log.info("Loading match for authorization: matchId={}, userId={}", matchId, user.getId());

        MultiplayerMatch match = multiplayerMatchRepository.findById(matchId)
                .orElseThrow(() -> {
                    log.warn("Match not found: matchId={}, userId={}", matchId, user.getId());
                    return new IllegalArgumentException("Match not found");
                });

        boolean isAllowed =
                user.getId().equals(match.getPlayerOneId()) ||
                        user.getId().equals(match.getPlayerTwoId());

        if (!isAllowed) {
            log.warn("Unauthorized match access attempt: matchId={}, userId={}, username={}, playerOneId={}, playerTwoId={}",
                    matchId,
                    user.getId(),
                    user.getUsername(),
                    match.getPlayerOneId(),
                    match.getPlayerTwoId());
            throw new UnauthorizedException("User not allowed to access this match");
        }

        log.info("User authorized for match access: matchId={}, userId={}", matchId, user.getId());
        return match;
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
