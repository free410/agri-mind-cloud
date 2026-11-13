package com.smartagri.common.util;

import com.smartagri.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = StringUtils.trimToEmpty(jwtProperties.getSecretKey()).getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY 未配置或长度不足 32 字节，请通过环境变量注入至少 32 字节的随机密钥");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long adminId, String username) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getExpireHours(), ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }

    public String extractToken(String authorizationHeader) {
        if (StringUtils.isBlank(authorizationHeader)) {
            return null;
        }

        String prefix = jwtProperties.getTokenPrefix() + " ";
        if (authorizationHeader.startsWith(prefix)) {
            return authorizationHeader.substring(prefix.length());
        }
        return authorizationHeader;
    }
}
