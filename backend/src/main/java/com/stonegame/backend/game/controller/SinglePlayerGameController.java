package com.stonegame.backend.game.controller;

import com.stonegame.backend.game.dto.PlayMoveRequest;
import com.stonegame.backend.game.dto.SinglePlayerGameResponse;
import com.stonegame.backend.game.service.SinglePlayerGameService;
import com.stonegame.backend.user.model.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for single-player games.
 */
@RestController
@RequestMapping("/games/single-player")
public class SinglePlayerGameController {

    private final SinglePlayerGameService singlePlayerGameService;

    public SinglePlayerGameController(SinglePlayerGameService singlePlayerGameService) {
        this.singlePlayerGameService = singlePlayerGameService;
    }

    /**
     * Plays one round against the computer.
     *
     * @param request player move request
     * @param user authenticated user
     * @return game round response
     */
    @PostMapping("/play")
    public SinglePlayerGameResponse play(@Valid @RequestBody PlayMoveRequest request,
                                         @AuthenticationPrincipal User user) {
        return singlePlayerGameService.play(user, request.getMove());
    }
}
