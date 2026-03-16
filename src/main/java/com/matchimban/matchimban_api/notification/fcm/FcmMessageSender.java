package com.matchimban.matchimban_api.notification.fcm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.matchimban.matchimban_api.notification.entity.Notification;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmMessageSender {

    private static final int DATA_PAYLOAD_LIMIT_BYTES = 4000;
    private static final int CONTENT_MAX_LENGTH = 180;

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final ObjectMapper objectMapper;

    public SendResult send(Notification notification, String tokenSnapshot) {
        if (!StringUtils.hasText(tokenSnapshot)) {
            return SendResult.permanentFailure("TOKEN_MISSING", "fcm token is empty");
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            return SendResult.temporaryFailure("FCM_DISABLED", "firebase messaging is not configured");
        }

        try {
            Map<String, String> data = buildDataPayload(notification);
            Message message = Message.builder()
                    .setToken(tokenSnapshot)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(notification.getTitle())
                                    .setBody(truncate(notification.getContent(), CONTENT_MAX_LENGTH))
                                    .build()
                    )
                    .putAllData(data)
                    .build();

            firebaseMessaging.send(message);
            return SendResult.succeeded();
        } catch (FirebaseMessagingException ex) {
            MessagingErrorCode code = ex.getMessagingErrorCode();
            String errorCode = code != null
                    ? code.name()
                    : (ex.getErrorCode() == null ? "FCM_ERROR" : ex.getErrorCode().name());
            boolean permanent = code == MessagingErrorCode.UNREGISTERED
                    || code == MessagingErrorCode.INVALID_ARGUMENT;

            if (permanent) {
                return SendResult.permanentFailure(errorCode, safeMessage(ex));
            }
            return SendResult.temporaryFailure(errorCode, safeMessage(ex));
        } catch (Exception ex) {
            log.warn("Failed to send FCM message. notificationId={}", notification.getId(), ex);
            return SendResult.temporaryFailure("FCM_SEND_FAILED", safeMessage(ex));
        }
    }

    private Map<String, String> buildDataPayload(Notification notification) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("notiType", notification.getNotiType().name());
        putIfNotBlank(data, "targetType", notification.getTargetType());
        putIfNotNull(data, "targetId", notification.getTargetId());
        putIfNotNull(data, "subTargetId", notification.getSubTargetId());
        putIfNotBlank(data, "deeplinkPath", notification.getDeeplinkPath());

        if (StringUtils.hasText(notification.getPayloadJson())
                && notification.getPayloadJson().length() <= 1024) {
            data.put("payloadJson", notification.getPayloadJson());
        }

        trimPayloadIfExceeded(data);
        return data;
    }

    private void trimPayloadIfExceeded(Map<String, String> data) {
        try {
            int bytes = objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8).length;
            if (bytes <= DATA_PAYLOAD_LIMIT_BYTES) {
                return;
            }
            data.remove("payloadJson");
        } catch (Exception ignored) {
            data.remove("payloadJson");
        }
    }

    private void putIfNotBlank(Map<String, String> data, String key, String value) {
        if (StringUtils.hasText(value)) {
            data.put(key, value);
        }
    }

    private void putIfNotNull(Map<String, String> data, String key, Long value) {
        if (value != null) {
            data.put(key, String.valueOf(value));
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String safeMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "notification send failure";
        }
        return ex.getMessage();
    }

    public record SendResult(boolean success, boolean permanentFailure, String errorCode, String errorMessage) {

        public static SendResult succeeded() {
            return new SendResult(true, false, null, null);
        }

        public static SendResult temporaryFailure(String errorCode, String errorMessage) {
            return new SendResult(false, false, errorCode, errorMessage);
        }

        public static SendResult permanentFailure(String errorCode, String errorMessage) {
            return new SendResult(false, true, errorCode, errorMessage);
        }
    }
}
