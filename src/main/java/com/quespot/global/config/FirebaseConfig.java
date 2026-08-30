package com.quespot.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!StringUtils.hasText(credentialsPath)) {
            log.warn("app.firebase.credentials-path가 설정되지 않아 FirebaseApp을 초기화하지 않습니다.");
            return null;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return firebaseApp == null ? null : FirebaseMessaging.getInstance(firebaseApp);
    }
}
