package com.qar.securitysystem.security;

import com.qar.securitysystem.config.AppSecurityProperties;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class SessionAuthFilter extends OncePerRequestFilter {
    private final SessionService sessionService;
    private final AppSecurityProperties securityProperties;

    public SessionAuthFilter(SessionService sessionService, AppSecurityProperties securityProperties) {
        this.sessionService = sessionService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String raw = readCookie(request, securityProperties.getCookieName());
            if (raw != null && !raw.isBlank()) {
                UserEntity user = sessionService.resolveUserFromSessionToken(raw);
                if (user != null) {
                    AppPrincipal principal = new AppPrincipal(user.getId(), user.getAccount(), user.getRole(), user.getPersonId());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
