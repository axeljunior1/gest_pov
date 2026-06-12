package com.erp.products.service.alert;

import com.erp.products.domain.entity.Alert;
import com.erp.products.domain.entity.Notification;
import com.erp.products.domain.enums.NotificationChannel;
import com.erp.products.domain.enums.NotificationStatus;
import com.erp.products.repository.NotificationRepository;
import com.erp.products.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;

    @Transactional
    public void dispatchForAlert(Alert alert) {
        List<com.erp.products.domain.entity.UserNotificationPreference> prefs =
                preferenceRepository.findByAlertTypeAndEnabledTrue(alert.getType());

        if (prefs.isEmpty()) {
            createAndSend(alert, "admin@erp.local", NotificationChannel.IN_APP);
            return;
        }

        for (var pref : prefs) {
            createAndSend(alert, pref.getUserId(), pref.getChannel());
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> findByUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }

    @Transactional
    public Notification markRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.erp.products.exception.ResourceNotFoundException(
                        "Notification non trouvee: " + notificationId));
        n.setStatus(NotificationStatus.READ);
        n.setReadAt(Instant.now());
        return notificationRepository.save(n);
    }

    private void createAndSend(Alert alert, String userId, NotificationChannel channel) {
        Notification notification = Notification.builder()
                .alert(alert)
                .userId(userId)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .build();
        notification = notificationRepository.save(notification);

        try {
            switch (channel) {
                case IN_APP -> log.info("Notification IN_APP user={} alert={} : {}",
                        userId, alert.getType(), alert.getMessage());
                case EMAIL -> log.info("Notification EMAIL (stub) -> {} : {}", userId, alert.getMessage());
                case SMS, PUSH, SLACK, WHATSAPP -> log.info("Notification {} (stub) -> {}", channel, userId);
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }
        notificationRepository.save(notification);
    }
}
