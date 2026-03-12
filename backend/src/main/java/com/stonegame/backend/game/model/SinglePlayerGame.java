package com.stonegame.backend.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a single-player game round.
 */
@Document(collection = "single_player_games")
@Data
public class SinglePlayerGame {

    @Id
    private String id;
    private String userId;
    private Move playerMove;
    private Move computerMove;
    private GameResult result;
    private GameMode gameMode;
    private Instant createdAt;

    public SinglePlayerGame(String userId, Move playerMove, Move computerMove, GameResult result, GameMode gameMode, Instant createdAt) {
        this.userId = userId;
        this.playerMove = playerMove;
        this.computerMove = computerMove;
        this.result = result;
        this.gameMode = gameMode;
        this.createdAt = createdAt;
    }
}
