package com.quespot.global.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // 외부 API가 느리거나 응답이 없을 때 스케줄러/서비스 스레드가 무기한
    // 블록되지 않게 한다. Spring이 자동구성한 모든 RestClient.Builder에 적용된다
    // (TourApiClient가 이 빈을 사용).
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect()
                .build(ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withReadTimeout(Duration.ofSeconds(10)));

        return builder -> builder.requestFactory(requestFactory);
    }
}
