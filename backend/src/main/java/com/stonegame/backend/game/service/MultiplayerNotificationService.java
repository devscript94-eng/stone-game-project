package com.stonegame.backend.game.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending multiplayer WebSocket notifications.
 */
@Service
public class MultiplayerNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public MultiplayerNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Sends a user-specific matchmaking update.
     *
     * @param username target username
     * @param payload event payload
     */
    public void sendMatchUpdateToUser(String username, Object payload) {
        messagingTemplate.convertAndSendToUser(username, "/queue/match-updates", payload);
    }

    /**
     * Sends a match event to all subscribers of the match topic.
     *
     * @param matchId match identifier
     * @param payload event payload
     */
    public void sendMatchUpdateToTopic(String matchId, Object payload) {
        messagingTemplate.convertAndSend("/topic/matches/" + matchId, payload);
    }
}
