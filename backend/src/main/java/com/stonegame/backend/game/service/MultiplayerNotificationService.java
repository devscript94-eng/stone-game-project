package com.stonegame.backend.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending multiplayer WebSocket notifications.
 */
@Service
public class MultiplayerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MultiplayerNotificationService.class);

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
        String destination = "/queue/match-updates";

        log.info("Sending user-specific WebSocket notification: username={}, destination={}",
                username, destination);

        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);

            log.info("User notification sent successfully: username={}, destination={}",
                    username, destination);

        } catch (Exception ex) {
            log.error("Failed to send user notification: username={}, destination={}, error={}",
                    username, destination, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Sends a match event to all subscribers of the match topic.
     *
     * @param matchId match identifier
     * @param payload event payload
     */
    public void sendMatchUpdateToTopic(String matchId, Object payload) {
        String destination = "/topic/matches/" + matchId;

        log.info("Sending match event to topic: matchId={}, destination={}",
                matchId, destination);

        try {
            messagingTemplate.convertAndSend(destination, payload);

            log.info("Match event sent successfully: matchId={}, destination={}",
                    matchId, destination);

        } catch (Exception ex) {
            log.error("Failed to send match event: matchId={}, destination={}, error={}",
                    matchId, destination, ex.getMessage(), ex);
            throw ex;
        }
    }
}