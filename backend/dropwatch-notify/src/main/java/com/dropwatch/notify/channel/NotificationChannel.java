package com.dropwatch.notify.channel;

import com.dropwatch.core.domain.AlertEvent;
import com.dropwatch.core.domain.NotificationChannelEntity;

public interface NotificationChannel {

    NotificationChannelEntity.ChannelType getChannelType();

    boolean sendAlert(AlertEvent alert, NotificationChannelEntity config, String productTitle, String buyUrl);
}
