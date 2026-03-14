package com.stonegame.backend.game.dto;

/**
 * WebSocket event payload for multiplayer updates.
 *
 * @param type event type
 * @param matchId match identifier
 * @param payload event payload
 */
public record MultiplayerEventResponse(
        String type,
        String matchId,
        Object payload
) {
}
