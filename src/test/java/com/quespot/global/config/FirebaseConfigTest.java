package com.quespot.global.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseConfig.class);

    @Test
    void doesNotRegisterFirebaseBeansWhenCredentialsPathIsBlank() {
        contextRunner
                .withPropertyValues("app.firebase.credentials-path=")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FirebaseApp.class);
                    assertThat(context).doesNotHaveBean(FirebaseMessaging.class);
                });
    }
}
