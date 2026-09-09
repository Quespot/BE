package com.quespot.domain.user.service;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.OAuth2ProviderUnlinkException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class OAuth2ProviderUnlinkService {

    private static final String NAVER_REVOKE_URI = "https://nid.naver.com/oauth2.0/revoke";
    private static final String NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token";
    private static final String KAKAO_UNLINK_URI = "https://kapi.kakao.com/v1/user/unlink";
    private static final String GOOGLE_REVOKE_URI = "https://oauth2.googleapis.com/revoke";

    private final RestClient restClient;
    private final OAuth2TokenCipher oAuth2TokenCipher;
    private final String naverClientId;
    private final String naverClientSecret;
    private final String kakaoAdminKey;

    public OAuth2ProviderUnlinkService(
            RestClient.Builder restClientBuilder,
            OAuth2TokenCipher oAuth2TokenCipher,
            @Value("${app.oauth2.naver.client-id:}") String naverClientId,
            @Value("${app.oauth2.naver.client-secret:}") String naverClientSecret,
            @Value("${app.oauth2.kakao.admin-key:}") String kakaoAdminKey
    ) {
        this.restClient = restClientBuilder.build();
        this.oAuth2TokenCipher = oAuth2TokenCipher;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.kakaoAdminKey = kakaoAdminKey;
    }

    // 제공자별 API를 호출하여 Quespot에 부여된 OAuth 연동을 해제하는 로직
    public void unlink(OAuth2UnlinkCommand command) {
        try {
            switch (command.provider()) {
                case NAVER -> unlinkNaver(command);
                case KAKAO -> unlinkKakao(command);
                case GOOGLE -> unlinkGoogle(command);
                default -> throw OAuth2ProviderUnlinkException.permanent(
                        new AuthException(AuthErrorCode.UNSUPPORTED_LOGIN_PROVIDER)
                );
            }
        } catch (OAuth2ProviderUnlinkException exception) {
            throw exception;
        } catch (RestClientException exception) {
            if (isRetryable(exception)) {
                throw OAuth2ProviderUnlinkException.retryable(exception);
            }
            throw OAuth2ProviderUnlinkException.permanent(exception);
        } catch (AuthException exception) {
            throw OAuth2ProviderUnlinkException.permanent(exception);
        }
    }

    private void unlinkNaver(OAuth2UnlinkCommand command) {
        String refreshToken = oAuth2TokenCipher.decrypt(command.encryptedRefreshToken());
        String accessToken = oAuth2TokenCipher.decrypt(command.encryptedAccessToken());
        if (naverClientId.isBlank() || naverClientSecret.isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
        }
        if (refreshToken != null) {
            try {
                accessToken = refreshNaverAccessToken(refreshToken);
            } catch (AuthException | RestClientException ignored) {
            }
        }
        if (accessToken == null) {
            throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("token", accessToken);
        body.add("token_type_hint", "access_token");

        postForm(NAVER_REVOKE_URI, body, null);
    }

    private String refreshNaverAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("refresh_token", refreshToken);

        Map<String, Object> response = restClient.post()
                .uri(NAVER_TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        Object accessToken = response == null ? null : response.get("access_token");
        if (!(accessToken instanceof String value) || value.isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
        }
        return value;
    }

    private void unlinkKakao(OAuth2UnlinkCommand command) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        String authorization;
        if (!kakaoAdminKey.isBlank()) {
            authorization = "KakaoAK " + kakaoAdminKey;
            body.add("target_id_type", "user_id");
            body.add("target_id", command.providerUserId());
        } else {
            String accessToken = oAuth2TokenCipher.decrypt(command.encryptedAccessToken());
            if (accessToken == null) {
                throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
            }
            authorization = "Bearer " + accessToken;
        }

        postForm(KAKAO_UNLINK_URI, body, authorization);
    }

    private void unlinkGoogle(OAuth2UnlinkCommand command) {
        String refreshToken = oAuth2TokenCipher.decrypt(command.encryptedRefreshToken());
        String accessToken = oAuth2TokenCipher.decrypt(command.encryptedAccessToken());
        String token = refreshToken != null ? refreshToken : accessToken;
        if (token == null) {
            throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);
        postForm(GOOGLE_REVOKE_URI, body, null);
    }

    private void postForm(
            String uri,
            MultiValueMap<String, String> body,
            String authorization
    ) {
        RestClient.RequestBodySpec request = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        request.body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private boolean isRetryable(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value() == 429
                    || responseException.getStatusCode().is5xxServerError();
        }
        return true;
    }
}
