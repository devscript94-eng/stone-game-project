package com.stonegame.backend.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository for accessing user documents.
 */
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Checks whether a user already exists with the given email.
     *
     * @param email normalized email
     * @return true if email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks whether a user already exists with the given username.
     *
     * @param username normalized username
     * @return true if username exists
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Finds a user by email.
     *
     * @param email normalized email
     * @return matching user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);
}
