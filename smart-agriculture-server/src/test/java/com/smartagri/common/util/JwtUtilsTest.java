package com.smartagri.common.util;

import com.smartagri.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {

    @Test
    void shouldRejectBlankSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey(" ");
        assertThrows(IllegalStateException.class, () -> new JwtUtils(properties));
    }

    @Test
    void shouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("too-short");
        assertThrows(IllegalStateException.class, () -> new JwtUtils(properties));
    }

    @Test
    void shouldGenerateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("unit-test-jwt-secret-key-at-least-32-bytes");
        properties.setExpireHours(2);
        JwtUtils jwtUtils = new JwtUtils(properties);

        String token = jwtUtils.generateToken(1L, "admin");
        Claims claims = jwtUtils.parseToken(token);

        assertEquals("admin", claims.get("username", String.class));
        assertEquals(1L, ((Number) claims.get("adminId")).longValue());
    }
}
