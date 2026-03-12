package com.stonegame.backend.game.dto;

import com.stonegame.backend.game.model.Move;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for a single-player move.
 */
@Getter
@Setter
public class PlayMoveRequest {

    @NotNull
    private Move move;
}
