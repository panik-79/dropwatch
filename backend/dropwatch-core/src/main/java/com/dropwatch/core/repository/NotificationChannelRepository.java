package com.dropwatch.core.repository;

import com.dropwatch.core.domain.NotificationChannelEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationChannelRepository extends MongoRepository<NotificationChannelEntity, String> {
    List<NotificationChannelEntity> findByUserId(String userId);
    List<NotificationChannelEntity> findByUserIdAndEnabledTrue(String userId);
}
