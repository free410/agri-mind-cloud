package com.smartagri.security;

import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.JwtProperties;
import com.smartagri.common.result.ResultCode;
import com.smartagri.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthenticationInterceptor(JwtUtils jwtUtils, JwtProperties jwtProperties) {
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String requestPath = request.getRequestURI().replaceFirst(request.getContextPath(), "");
        if (isIgnoredPath(requestPath)) {
            return true;
        }

        String header = request.getHeader(jwtProperties.getHeaderName());
        String token = jwtUtils.extractToken(header);
        if (!jwtUtils.isTokenValid(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        Claims claims = jwtUtils.parseToken(token);
        Object adminIdValue = claims.get("adminId");
        Long adminId = adminIdValue == null ? null : Long.parseLong(String.valueOf(adminIdValue));
        String username = claims.get("username", String.class);
        LoginAdminContext.setAdmin(adminId, username);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginAdminContext.clear();
    }

    private boolean isIgnoredPath(String requestPath) {
        return jwtProperties.getIgnoreUrls().stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, requestPath));
    }
}
