package com.matchimban.matchimban_api.notification.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.notification.dto.request.NotificationTokenDeactivateRequest;
import com.matchimban.matchimban_api.notification.dto.request.NotificationTokenUpsertRequest;
import com.matchimban.matchimban_api.notification.dto.response.NotificationListResponse;
import com.matchimban.matchimban_api.notification.service.NotificationCommandService;
import com.matchimban.matchimban_api.notification.service.NotificationQueryService;
import com.matchimban.matchimban_api.notification.service.NotificationTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final NotificationTokenService notificationTokenService;

    @GetMapping
    public ResponseEntity<ApiResult<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) Instant cursorCreatedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        NotificationListResponse response = notificationQueryService
                .getNotifications(principal.memberId(), cursorCreatedAt, cursorId, size);
        return ResponseEntity.ok(ApiResult.of("notifications_loaded", response));
    }

    @CsrfRequired
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResult<Void>> markRead(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long notificationId
    ) {
        notificationCommandService.markRead(principal.memberId(), notificationId);
        return ResponseEntity.ok(ApiResult.of("notification_read"));
    }

    @CsrfRequired
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResult<ReadAllResult>> markReadAll(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        int updatedCount = notificationCommandService.markAllRead(principal.memberId());
        return ResponseEntity.ok(ApiResult.of("notifications_read_all", new ReadAllResult(updatedCount)));
    }

    @CsrfRequired
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResult<Void>> softDelete(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long notificationId
    ) {
        notificationCommandService.softDelete(principal.memberId(), notificationId);
        return ResponseEntity.ok(ApiResult.of("notification_deleted"));
    }

    @CsrfRequired
    @PostMapping("/tokens")
    public ResponseEntity<ApiResult<Void>> upsertToken(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody NotificationTokenUpsertRequest request
    ) {
        notificationTokenService.upsertToken(principal.memberId(), request);
        return ResponseEntity.ok(ApiResult.of("notification_token_upserted"));
    }

    @CsrfRequired
    @PostMapping("/tokens/deactivate")
    public ResponseEntity<ApiResult<Void>> deactivateToken(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody NotificationTokenDeactivateRequest request
    ) {
        notificationTokenService.deactivateToken(principal.memberId(), request);
        return ResponseEntity.ok(ApiResult.of("notification_token_deactivated"));
    }

    public record ReadAllResult(int updatedCount) {
    }
}
