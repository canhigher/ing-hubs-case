package com.ingbrokerage.security.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.ingbrokerage.repository.UserRepository;

/**
 * Service to handle authorization checks for data access
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    /**
     * Check if the current authenticated user is the owner of the resource or an admin
     *
     * @param customerId The customer ID of the resource owner
     * @return true if the current user is the owner or an admin, false otherwise
     */
    public boolean isOwnerOrAdmin(Long customerId) {
        if (customerId == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }


        // Check if user has ADMIN role
        if (hasAdminRole(authentication)) {
            return true;
        }

        // Get the current user from UserDetails
        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            // Check if user ID matches the customer ID
            return userDetails.getId().equals(customerId);
        }

        return false;
    }

    /**
     * Check if the current authenticated user has admin role
     *
     * @return true if the user has admin role, false otherwise
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return hasAdminRole(authentication);
    }

    /**
     * Helper method to check if authentication has admin role
     */
    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }
} 