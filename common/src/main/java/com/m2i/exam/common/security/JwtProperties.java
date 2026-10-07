package com.m2i.exam.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param secret         HMAC-SHA256 signing secret shared by all services (min 32 bytes), injected from a Secret
 * @param issuer         value of the {@code iss} claim; tokens with another issuer are rejected
 * @param accessTokenTtl lifetime of user access tokens
 * @param publicPaths    extra unauthenticated paths, optionally prefixed by an HTTP method ("POST /api/contact")
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
        @NotBlank String issuer,
        Duration accessTokenTtl,
        List<String> publicPaths) {

    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofHours(1);
        }
        if (publicPaths == null) {
            publicPaths = List.of();
        }
    }
}
