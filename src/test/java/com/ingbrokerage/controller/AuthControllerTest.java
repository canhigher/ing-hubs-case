package com.ingbrokerage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingbrokerage.dto.auth.JwtResponse;
import com.ingbrokerage.dto.auth.LoginRequest;
import com.ingbrokerage.dto.auth.SignupRequest;
import com.ingbrokerage.security.jwt.JwtUtils;
import com.ingbrokerage.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**").permitAll()
                    .anyRequest().authenticated()
                );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    
    @MockBean
    private JwtUtils jwtUtils;
    
    @MockBean
    private UserDetailsService userDetailsService;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private JwtResponse jwtResponse;

    @BeforeEach
    void setUp() {
        // Create login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Create signup request
        signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRoles(new HashSet<>(Collections.singletonList("USER")));

        // Create JWT response
        jwtResponse = new JwtResponse(
                "test-jwt-token",
                1L,
                "testuser",
                "test@example.com",
                Collections.singletonList("ROLE_USER")
        );
    }

    @Test
    void authenticateUser_WhenValidCredentials_ShouldReturnToken() throws Exception {
        // Given
        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(jwtResponse);

        // When/Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(authService).authenticateUser(any(LoginRequest.class));
    }

    @Test
    void registerUser_WhenSuccessful_ShouldReturnSuccessMessage() throws Exception {
        // Given
        when(authService.registerUser(any(SignupRequest.class))).thenReturn(true);

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        verify(authService).registerUser(any(SignupRequest.class));
    }

    @Test
    void registerUser_WhenUserExists_ShouldReturnError() throws Exception {
        // Given
        when(authService.registerUser(any(SignupRequest.class))).thenReturn(false);

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username or email is already taken!"));

        verify(authService).registerUser(any(SignupRequest.class));
    }
} 