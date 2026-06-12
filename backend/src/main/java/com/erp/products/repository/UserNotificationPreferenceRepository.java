package com.erp.products.repository;

import com.erp.products.domain.entity.UserNotificationPreference;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.domain.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    List<UserNotificationPreference> findByAlertTypeAndEnabledTrue(AlertType alertType);

    List<UserNotificationPreference> findByUserIdAndEnabledTrue(String userId);

    List<UserNotificationPreference> findByAlertTypeAndChannelAndEnabledTrue(
            AlertType alertType, NotificationChannel channel);
}
