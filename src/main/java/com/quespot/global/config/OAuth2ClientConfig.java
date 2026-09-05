package com.quespot.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(name = "app.oauth2.google.enabled", havingValue = "true")
public class OAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth2.google.client-id}") String clientId,
            @Value("${app.oauth2.google.client-secret}") String clientSecret,
            @Value("${app.oauth2.google.redirect-uri}") String redirectUri
    ) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .scope("openid", "email")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
