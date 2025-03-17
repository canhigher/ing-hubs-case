package com.ingbrokerage.controller;

import com.ingbrokerage.dto.auth.JwtResponse;
import com.ingbrokerage.dto.auth.LoginRequest;
import com.ingbrokerage.dto.auth.MessageResponse;
import com.ingbrokerage.dto.auth.SignupRequest;
import com.ingbrokerage.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for user: {}", loginRequest.getUsername());
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Registration request received for user: {}", signupRequest.getUsername());
        
        boolean success = authService.registerUser(signupRequest);
        
        if (!success) {
            String errorMessage = "Username or email is already taken!";
            log.warn(errorMessage);
            return ResponseEntity.badRequest().body(new MessageResponse(errorMessage));
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}