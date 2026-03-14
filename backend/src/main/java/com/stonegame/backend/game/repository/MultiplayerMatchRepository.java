package com.stonegame.backend.game.repository;

import com.stonegame.backend.game.model.MultiplayerMatch;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for multiplayer match documents.
 */
public interface MultiplayerMatchRepository extends MongoRepository<MultiplayerMatch, String> {
}
