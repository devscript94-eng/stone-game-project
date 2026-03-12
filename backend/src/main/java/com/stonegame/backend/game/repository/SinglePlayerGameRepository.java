package com.stonegame.backend.game.repository;

import com.stonegame.backend.game.model.SinglePlayerGame;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for single-player game documents.
 */
public interface SinglePlayerGameRepository extends MongoRepository<SinglePlayerGame, String> {
}
