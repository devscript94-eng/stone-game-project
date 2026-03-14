package com.stonegame.backend.game.controller;

import com.stonegame.backend.game.dto.JoinMatchResponse;
import com.stonegame.backend.game.dto.MultiplayerMatchResponse;
import com.stonegame.backend.game.dto.PlayMoveRequest;
import com.stonegame.backend.game.service.MatchmakingService;
import com.stonegame.backend.game.service.MultiplayerService;
import com.stonegame.backend.user.model.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for multiplayer matchmaking and gameplay.
 */
@RestController
@RequestMapping("/multiplayer")
public class MultiplayerController {

    private final MatchmakingService matchmakingService;
    private final MultiplayerService multiplayerService;

    public MultiplayerController(MatchmakingService matchmakingService,
                                 MultiplayerService multiplayerService) {
        this.matchmakingService = matchmakingService;
        this.multiplayerService = multiplayerService;
    }

    /**
     * Joins the authenticated player to multiplayer matchmaking.
     *
     * @param user authenticated user
     * @return matchmaking response
     */
    @PostMapping("/join")
    public JoinMatchResponse join(@AuthenticationPrincipal User user) {
        return matchmakingService.join(user);
    }

    /**
     * Returns the current state of a multiplayer match.
     *
     * @param matchId match identifier
     * @param user authenticated user
     * @return match state
     */
    @GetMapping("/{matchId}")
    public MultiplayerMatchResponse getMatch(@PathVariable String matchId,
                                             @AuthenticationPrincipal User user) {
        return multiplayerService.getMatch(matchId, user);
    }

    /**
     * Submits a move for the authenticated player.
     *
     * @param matchId match identifier
     * @param request move request
     * @param user authenticated user
     * @return updated match state
     */
    @PostMapping("/{matchId}/move")
    public MultiplayerMatchResponse submitMove(@PathVariable String matchId,
                                               @Valid @RequestBody PlayMoveRequest request,
                                               @AuthenticationPrincipal User user) {
        return multiplayerService.submitMove(matchId, user, request.getMove());
    }
}
