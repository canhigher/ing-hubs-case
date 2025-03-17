package com.ingbrokerage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingbrokerage.exception.GlobalExceptionHandler.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.error("Authentication error: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorMessage = "Authentication failed";
        
        // Customize error message based on exception type
        if (authException instanceof BadCredentialsException) {
            errorMessage = "Invalid username or password";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                errorMessage,
                LocalDateTime.now()
        );

        final ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // To support Java 8 date/time types
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
} 