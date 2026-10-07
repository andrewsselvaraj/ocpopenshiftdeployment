package com.m2i.exam.exam.client;

import com.m2i.exam.common.security.TokenService;
import com.m2i.exam.common.web.RequestIdFilter;
import java.time.Duration;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    RestClient questionRestClient(RestClient.Builder builder, TokenService tokens,
                                  @Value("${app.clients.question-service.url}") String baseUrl,
                                  @Value("${app.clients.connect-timeout:2s}") Duration connectTimeout,
                                  @Value("${app.clients.read-timeout:5s}") Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);
        return builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokens.serviceToken("exam-service"));
                    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
                    if (requestId != null) {
                        request.getHeaders().set(RequestIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
