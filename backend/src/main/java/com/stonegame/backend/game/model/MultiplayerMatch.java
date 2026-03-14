package com.stonegame.backend.game.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a multiplayer match.
 */
@Document(collection = "multiplayer_matches")
@Data
public class MultiplayerMatch {

    @Id
    private String id;
    private String playerOneId;
    private String playerOneUsername;
    private String playerTwoId;
    private String playerTwoUsername;
    private Move playerOneMove;
    private Move playerTwoMove;
    private MatchStatus status;
    private MultiplayerResult result;
    private Instant createdAt;
}
