package com.ingbrokerage.service;

import com.ingbrokerage.dto.auth.JwtResponse;
import com.ingbrokerage.dto.auth.LoginRequest;
import com.ingbrokerage.dto.auth.SignupRequest;
import com.ingbrokerage.model.Role;
import com.ingbrokerage.model.User;
import com.ingbrokerage.model.enums.RoleType;
import com.ingbrokerage.repository.RoleRepository;
import com.ingbrokerage.repository.UserRepository;
import com.ingbrokerage.security.jwt.JwtUtils;
import com.ingbrokerage.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private User user;
    private Role userRole;
    private Authentication authentication;
    private UserDetailsImpl userDetails;

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
        signupRequest.setRoles(new HashSet<>(Collections.singletonList("CUSTOMER")));

        // Create user
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");

        // Create role
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleType.ROLE_CUSTOMER);

        // Create user details
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(RoleType.ROLE_CUSTOMER.name()));
        
        userDetails = new UserDetailsImpl(
                1L,
                "testuser",
                "test@example.com",
                "encodedPassword",
                authorities
        );

        // Create authentication
        authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }

    @Test
    void authenticateUser_WhenValidCredentials_ShouldReturnJwtResponse() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("test-jwt-token");

        // When
        JwtResponse response = authService.authenticateUser(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_CUSTOMER"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(authentication);
    }

    @Test
    void registerUser_WhenUserDoesNotExist_ShouldRegisterSuccessfully() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_CUSTOMER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // When
        boolean result = authService.registerUser(signupRequest);

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(roleRepository).findByName(RoleType.ROLE_CUSTOMER);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WhenUsernameExists_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // When
        boolean result = authService.registerUser(signupRequest);

        // Then
        assertFalse(result);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WhenEmailExists_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // When
        boolean result = authService.registerUser(signupRequest);

        // Then
        assertFalse(result);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
} 