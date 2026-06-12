package com.erp.products.controller;

import com.erp.products.dto.NotificationResponse;
import com.erp.products.mapper.AlertMapper;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.alert.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AlertMapper alertMapper;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public List<NotificationResponse> list(@RequestParam(required = false) String userId) {
        return notificationService.findByUser(resolveUserId(userId)).stream()
                .map(alertMapper::toNotificationResponse)
                .toList();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public Map<String, Long> unreadCount(@RequestParam(required = false) String userId) {
        return Map.of("count", notificationService.countUnread(resolveUserId(userId)));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public NotificationResponse markRead(@PathVariable Long id) {
        return alertMapper.toNotificationResponse(notificationService.markRead(id));
    }

    private String resolveUserId(String userId) {
        if (currentUserService.isAuthenticated()) {
            return currentUserService.getCurrentUserEmailOrDefault();
        }
        return userId != null && !userId.isBlank() ? userId : "system";
    }
}
