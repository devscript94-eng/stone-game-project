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
import org.springframework.stereotype.Service;

/**
 * Service responsible for multiplayer match lifecycle.
 */
@Service
public class MultiplayerService {

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
        MultiplayerMatch match = findAuthorizedMatch(matchId, user);
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
        MultiplayerMatch match = findAuthorizedMatch(matchId, user);

        if (match.getStatus() != MatchStatus.WAITING_FOR_MOVES) {
            throw new IllegalArgumentException("Match is not ready for moves");
        }

        if (user.getId().equals(match.getPlayerOneId())) {
            if (match.getPlayerOneMove() != null) {
                throw new IllegalArgumentException("Player one already submitted a move");
            }
            match.setPlayerOneMove(move);
        } else if (user.getId().equals(match.getPlayerTwoId())) {
            if (match.getPlayerTwoMove() != null) {
                throw new IllegalArgumentException("Player two already submitted a move");
            }
            match.setPlayerTwoMove(move);
        }

        if (match.getPlayerOneMove() != null && match.getPlayerTwoMove() != null) {
            GameResult playerOneResult =
                    gameRulesService.evaluate(match.getPlayerOneMove(), match.getPlayerTwoMove());

            MultiplayerResult result = switch (playerOneResult) {
                case WIN -> MultiplayerResult.PLAYER_ONE_WIN;
                case LOSE -> MultiplayerResult.PLAYER_TWO_WIN;
                case DRAW -> MultiplayerResult.DRAW;
            };

            match.setResult(result);
            match.setStatus(MatchStatus.COMPLETED);
        }

        MultiplayerMatch saved = multiplayerMatchRepository.save(match);
        MultiplayerMatchResponse response = toResponse(saved);

        if (saved.getStatus() == MatchStatus.COMPLETED) {
            multiplayerNotificationService.sendMatchUpdateToTopic(
                    saved.getId(),
                    new MultiplayerEventResponse("MATCH_COMPLETED", saved.getId(), response)
            );
        } else {
            multiplayerNotificationService.sendMatchUpdateToTopic(
                    saved.getId(),
                    new MultiplayerEventResponse("MOVE_SUBMITTED", saved.getId(), response)
            );
        }

        return response;
    }

    private MultiplayerMatch findAuthorizedMatch(String matchId, User user) {
        MultiplayerMatch match = multiplayerMatchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        boolean isAllowed =
                user.getId().equals(match.getPlayerOneId()) ||
                        user.getId().equals(match.getPlayerTwoId());

        if (!isAllowed) {
            throw new UnauthorizedException("User not allowed to access this match");
        }

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
