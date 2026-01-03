/*
 * TeraAPI - Identity Service
 * Copyright (c) 2026 YiStudIo Software Inc. All rights reserved.
 * Licensed under proprietary license.
 */
package com.teraapi.identity.controller;

import com.teraapi.identity.dto.AuthenticationRequest;
import com.teraapi.identity.dto.AuthenticationResponse;
import com.teraapi.identity.dto.TokenValidationRequest;
import com.teraapi.identity.dto.TokenValidationResponse;
import com.teraapi.identity.entity.User;
import com.teraapi.identity.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Login request for user: {}", request.getUsername());
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Register request for user: {}", request.getUsername());
        User newUser = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getUsername() + "@example.com")
                .isActive(true)
                .isLocked(false)
                .build();
        
        AuthenticationResponse response = authenticationService.register(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Identity Service is running");
    }

    @PostMapping("/introspect")
    public ResponseEntity<TokenValidationResponse> introspect(
            @Valid @RequestBody TokenValidationRequest request) {
        log.info("Token introspection request received from downstream service");
        TokenValidationResponse response = authenticationService.validateToken(request.token());
        HttpStatus status = response.isActive() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }
}
