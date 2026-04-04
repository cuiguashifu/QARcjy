package com.qar.securitysystem.config;

import com.qar.securitysystem.security.SessionAuthFilter;
import com.qar.securitysystem.security.AuditLogFilter;
import com.qar.securitysystem.service.AuditLogService;
import com.qar.securitysystem.service.SessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig {
    @Bean
    public SessionAuthFilter sessionAuthFilter(SessionService sessionService, AppSecurityProperties securityProperties) {
        return new SessionAuthFilter(sessionService, securityProperties);
    }

    @Bean
    public AuditLogFilter auditLogFilter(AuditLogService auditLogService) {
        return new AuditLogFilter(auditLogService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionAuthFilter sessionAuthFilter, AuditLogFilter auditLogFilter) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .ignoringRequestMatchers("/h2/**")
                        .ignoringRequestMatchers("/api/auth/**")
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/", "/auth", "/h2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/workbench", "/workbench.html").authenticated()
                        .requestMatchers(HttpMethod.GET, "/feedback", "/feedback.html").authenticated()
                        .requestMatchers(HttpMethod.GET, "/admin", "/admin.html").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/auth.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/files").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(sessionAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditLogFilter, SessionAuthFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                )
                .cors(Customizer.withDefaults());

        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            if (isApi(request)) {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                PrintWriter w = response.getWriter();
                w.write("{\"code\":401,\"message\":\"unauthorized\"}");
                w.flush();
                return;
            }

            String target = UriComponentsBuilder.fromPath("/auth").toUriString();
            response.sendRedirect(target);
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            if (isApi(request)) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                PrintWriter w = response.getWriter();
                w.write("{\"code\":403,\"message\":\"forbidden\"}");
                w.flush();
                return;
            }
            response.setStatus(403);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("forbidden");
        };
    }

    private boolean isApi(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }
}
