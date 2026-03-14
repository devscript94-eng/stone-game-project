package com.stonegame.backend.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MultiplayerNotificationService}.
 */
class MultiplayerNotificationServiceTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final MultiplayerNotificationService multiplayerNotificationService =
            new MultiplayerNotificationService(messagingTemplate);

    @Test
    @DisplayName("sendMatchUpdateToUser should publish message to user queue")
    void sendMatchUpdateToUser_shouldPublishMessageToUserQueue() {
        Object payload = new Object();

        multiplayerNotificationService.sendMatchUpdateToUser("john", payload);

        verify(messagingTemplate).convertAndSendToUser("john", "/queue/match-updates", payload);
    }

    @Test
    @DisplayName("sendMatchUpdateToTopic should publish message to match topic")
    void sendMatchUpdateToTopic_shouldPublishMessageToMatchTopic() {
        Object payload = new Object();

        multiplayerNotificationService.sendMatchUpdateToTopic("match-1", payload);

        verify(messagingTemplate).convertAndSend("/topic/matches/match-1", payload);
    }
}
