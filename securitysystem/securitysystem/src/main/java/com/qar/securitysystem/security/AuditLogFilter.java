package com.qar.securitysystem.security;

import com.qar.securitysystem.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuditLogFilter extends OncePerRequestFilter {
    private final AuditLogService auditLogService;

    public AuditLogFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (!uri.startsWith("/api/")) {
            return true;
        }
        return uri.startsWith("/api/auth/") || uri.equals("/api/csrf");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        StatusCaptureResponseWrapper wrapped = new StatusCaptureResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapped);
        } finally {
            long duration = System.currentTimeMillis() - start;
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            AppPrincipal p = a != null && a.getPrincipal() instanceof AppPrincipal ? (AppPrincipal) a.getPrincipal() : null;
            auditLogService.logApi(p, request.getMethod(), request.getRequestURI(), wrapped.getStatus(), duration, request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
    }

    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int status = 200;

        public StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.status = 302;
            super.sendRedirect(location);
        }

        public int getStatus() {
            return status;
        }
    }
}

