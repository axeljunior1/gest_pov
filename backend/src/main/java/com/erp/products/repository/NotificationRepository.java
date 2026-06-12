package com.erp.products.repository;

import com.erp.products.domain.entity.Notification;
import com.erp.products.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, NotificationStatus status);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndStatus(String userId, NotificationStatus status);
}
