package com.dropwatch.core.repository;

import com.dropwatch.core.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByTelegramChatId(String telegramChatId);
    boolean existsByEmail(String email);
}
