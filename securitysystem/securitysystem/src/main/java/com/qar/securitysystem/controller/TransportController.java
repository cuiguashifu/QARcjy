package com.qar.securitysystem.controller;

import com.qar.securitysystem.transport.TransportHandshakeRequest;
import com.qar.securitysystem.transport.TransportHandshakeResponse;
import com.qar.securitysystem.transport.TransportSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transport")
public class TransportController {
    private final TransportSessionService sessionService;

    public TransportController(TransportSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/handshake")
    public ResponseEntity<TransportHandshakeResponse> handshake(Authentication authentication, @RequestBody TransportHandshakeRequest req) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        TransportSessionService.HandshakeResult r = sessionService.handshake(req == null ? null : req.getProtocol());
        TransportHandshakeResponse resp = new TransportHandshakeResponse();
        resp.setProtocol(r.protocol);
        resp.setSuite(r.suite);
        resp.setServerPublicKey(r.serverPublicKeyBase64);
        return ResponseEntity.ok(resp);
    }
}

