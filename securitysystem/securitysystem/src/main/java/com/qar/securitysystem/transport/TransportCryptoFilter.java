package com.qar.securitysystem.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qar.securitysystem.util.AesGcmUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class TransportCryptoFilter extends OncePerRequestFilter {
    public static final String HEADER_TRANSPORT = "X-QAR-Transport";
    public static final String HEADER_WRAPPED_KEY = "X-QAR-Wrapped-Key";
    public static final String HEADER_ENCRYPTED = "X-QAR-Encrypted";
    public static final String HEADER_ENCRYPTED_RESPONSE = "X-QAR-Encrypted-Response";

    private final TransportSessionService sessionService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public TransportCryptoFilter(TransportSessionService sessionService, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return true;
        }
        if ("/api/transport/handshake".equals(uri)) {
            return true;
        }
        String method = request.getMethod();
        return method != null && method.equalsIgnoreCase("OPTIONS");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {
        boolean wantsEncrypted = "1".equals(request.getHeader(HEADER_ENCRYPTED));
        if (!wantsEncrypted) {
            filterChain.doFilter(request, response);
            return;
        }

        String protocol = normalize(request.getHeader(HEADER_TRANSPORT));
        if (!TransportSessionService.PROTOCOL_TLS.equals(protocol)) {
            writeError(response, 400, "unsupported_transport");
            return;
        }

        String wrappedKey = request.getHeader(HEADER_WRAPPED_KEY);
        if (wrappedKey == null || wrappedKey.isBlank()) {
            writeError(response, 401, "transport_wrapped_key_required");
            return;
        }
        javax.crypto.spec.SecretKeySpec aesKey = sessionService.unwrapTransportKey(wrappedKey);

        byte[] aad = buildAad(request);
        HttpServletRequest reqToUse = request;
        if (hasBody(request)) {
            byte[] raw = request.getInputStream().readAllBytes();
            TransportEnvelope env = objectMapper.readValue(raw, TransportEnvelope.class);
            byte[] iv = Base64.getDecoder().decode(env.getIv());
            byte[] ciphertext = Base64.getDecoder().decode(env.getCiphertext());
            byte[] plaintext = AesGcmUtil.decrypt(aesKey, iv, ciphertext, aad);
            reqToUse = new CachedBodyRequest(request, plaintext);
        }

        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(reqToUse, wrappedResp);

        byte[] respBody = wrappedResp.getContentAsByteArray();
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);
        byte[] ciphertext = AesGcmUtil.encrypt(aesKey, iv, respBody == null ? new byte[0] : respBody, aad);

        TransportEnvelope out = new TransportEnvelope();
        out.setIv(Base64.getEncoder().encodeToString(iv));
        out.setCiphertext(Base64.getEncoder().encodeToString(ciphertext));

        byte[] outBytes = objectMapper.writeValueAsBytes(out);
        wrappedResp.resetBuffer();
        wrappedResp.setHeader(HEADER_ENCRYPTED_RESPONSE, "1");
        wrappedResp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        wrappedResp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        wrappedResp.getOutputStream().write(outBytes);
        wrappedResp.copyBodyToResponse();
    }

    private static boolean hasBody(HttpServletRequest request) {
        String m = request.getMethod();
        if (m == null) {
            return false;
        }
        String method = m.toUpperCase();
        if (method.equals("GET") || method.equals("HEAD")) {
            return false;
        }
        int len = request.getContentLength();
        return len > 0 || request.getHeader("Transfer-Encoding") != null;
    }

    private static String normalize(String v) {
        return v == null ? "" : v.trim().toLowerCase();
    }

    private static byte[] buildAad(HttpServletRequest request) {
        String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase();
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        return (method + " " + uri).getBytes(StandardCharsets.UTF_8);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        public CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body == null ? new byte[0] : body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
