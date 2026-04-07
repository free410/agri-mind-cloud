package com.smartagri.common.util;

import com.smartagri.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
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

    private final JwtProperties jwtProperties;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
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
                .signWith(buildSecretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(buildSecretKey())
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

    private SecretKey buildSecretKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return Keys.hmacShaKeyFor(keyBytes);
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode("c21hcnQtYWdyaWN1bHR1cmUtZGVtby1zZWNyZXQta2V5LTEyMzQ1Njc4OTA="));
    }
}
