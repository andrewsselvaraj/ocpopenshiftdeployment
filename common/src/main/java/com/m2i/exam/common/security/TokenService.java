package com.m2i.exam.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Issues signed JWTs: user access tokens (auth-service) and short-lived service tokens (any service). */
public class TokenService {

    private static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(5);

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final AtomicReference<CachedToken> serviceToken = new AtomicReference<>();

    public TokenService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public IssuedToken issue(String subject, List<Role> roles, Map<String, Object> extraClaims) {
        return issue(subject, roles, extraClaims, props.accessTokenTtl());
    }

    public IssuedToken issue(String subject, List<Role> roles, Map<String, Object> extraClaims, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(SecurityConfig.ROLES_CLAIM, roles.stream().map(Role::name).toList());
        extraClaims.forEach((k, v) -> {
            if (v != null) {
                claims.claim(k, v);
            }
        });
        String value = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new IssuedToken(value, ttl.toSeconds(), now.plus(ttl));
    }

    /** Cached SERVICE-role token for calling another service's {@code /internal/**} endpoints. */
    public String serviceToken(String serviceName) {
        CachedToken current = serviceToken.get();
        if (current == null || current.expiresAt().isBefore(Instant.now().plusSeconds(30))) {
            IssuedToken t = issue("service:" + serviceName, List.of(Role.SERVICE), Map.of(), SERVICE_TOKEN_TTL);
            current = new CachedToken(t.value(), t.expiresAt());
            serviceToken.set(current);
        }
        return current.value();
    }

    public record IssuedToken(String value, long expiresInSeconds, Instant expiresAt) {
    }

    private record CachedToken(String value, Instant expiresAt) {
    }
}
