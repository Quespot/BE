package com.quespot.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnExpression("${app.oauth2.google.enabled:false} || ${app.oauth2.kakao.enabled:false}")
public class OAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth2.google.enabled:false}") boolean googleEnabled,
            @Value("${app.oauth2.google.client-id:}") String googleClientId,
            @Value("${app.oauth2.google.client-secret:}") String googleClientSecret,
            @Value("${app.oauth2.google.redirect-uri:}") String googleRedirectUri,
            @Value("${app.oauth2.kakao.enabled:false}") boolean kakaoEnabled,
            @Value("${app.oauth2.kakao.client-id:}") String kakaoClientId,
            @Value("${app.oauth2.kakao.client-secret:}") String kakaoClientSecret,
            @Value("${app.oauth2.kakao.redirect-uri:}") String kakaoRedirectUri
    ) {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (googleEnabled) {
            registrations.add(googleClientRegistration(
                    googleClientId,
                    googleClientSecret,
                    googleRedirectUri
            ));
        }

        if (kakaoEnabled) {
            registrations.add(kakaoClientRegistration(
                    kakaoClientId,
                    kakaoClientSecret,
                    kakaoRedirectUri
            ));
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration googleClientRegistration(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .scope("openid", "email")
                .build();
    }

    private ClientRegistration kakaoClientRegistration(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
        return ClientRegistration.withRegistrationId("kakao")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }
}
