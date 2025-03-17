package com.ingbrokerage.service;

import com.ingbrokerage.dto.auth.JwtResponse;
import com.ingbrokerage.dto.auth.LoginRequest;
import com.ingbrokerage.dto.auth.SignupRequest;

public interface AuthService {
    
    /**
     * Sign in with the given credentials and return a JWT token
     * 
     * @param loginRequest the login request containing username and password
     * @return JWT response with token and user details
     */
    JwtResponse authenticateUser(LoginRequest loginRequest);
    
    /**
     * Register a new user with the given details
     * 
     * @param signupRequest the signup request with user details
     * @return true if successful, false otherwise
     */
    boolean registerUser(SignupRequest signupRequest);
} 