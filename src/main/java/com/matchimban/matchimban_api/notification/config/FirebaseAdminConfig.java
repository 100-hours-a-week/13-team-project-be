package com.matchimban.matchimban_api.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "notification.firebase", name = "enabled", havingValue = "true")
public class FirebaseAdminConfig {

    @Bean
    public FirebaseApp notificationFirebaseApp(NotificationProperties properties) throws IOException {
        NotificationProperties.Firebase firebase = properties.getFirebase();
        GoogleCredentials credentials = loadCredentials(firebase);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(firebase.getProjectId())
                .build();

        String appName = "notification-firebase-app";
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (appName.equals(app.getName())) {
                return app;
            }
        }

        log.info("Initialize FirebaseApp for notifications. projectId={}", firebase.getProjectId());
        return FirebaseApp.initializeApp(options, appName);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp notificationFirebaseApp) {
        return FirebaseMessaging.getInstance(notificationFirebaseApp);
    }

    private GoogleCredentials loadCredentials(NotificationProperties.Firebase firebase) throws IOException {
        if (StringUtils.hasText(firebase.getServiceAccountJsonBase64())) {
            byte[] decoded = Base64.getDecoder().decode(firebase.getServiceAccountJsonBase64().trim());
            try (InputStream in = new ByteArrayInputStream(decoded)) {
                return GoogleCredentials.fromStream(in);
            }
        }

        if (StringUtils.hasText(firebase.getServiceAccountFile())) {
            try (InputStream in = new FileInputStream(firebase.getServiceAccountFile().trim())) {
                return GoogleCredentials.fromStream(in);
            }
        }

        throw new IllegalStateException(
                "Firebase credential is missing. Set FIREBASE_SERVICE_ACCOUNT_JSON_BASE64 or FIREBASE_SERVICE_ACCOUNT_FILE"
        );
    }
}
