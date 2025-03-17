package com.ingbrokerage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingbrokerage.dto.AssetBalanceRequest;
import com.ingbrokerage.dto.AssetDto;
import com.ingbrokerage.exception.AccessDeniedException;
import com.ingbrokerage.security.jwt.JwtUtils;
import com.ingbrokerage.security.services.AuthorizationService;
import com.ingbrokerage.service.AssetService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@Import(AssetControllerTest.TestSecurityConfig.class)
class AssetControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssetService assetService;

    @MockBean
    private AuthorizationService authService;
    
    @MockBean
    private JwtUtils jwtUtils;
    
    @MockBean
    private UserDetailsService userDetailsService;

    private List<AssetDto> testAssets;
    private AssetBalanceRequest balanceRequest;

    @BeforeEach
    void setUp() {
        // Create test assets
        testAssets = Arrays.asList(
                AssetDto.builder()
                        .id(1L)
                        .customerId(1L)
                        .assetName("TRY")
                        .size(new BigDecimal("10000.00"))
                        .usableSize(new BigDecimal("8000.00"))
                        .build(),
                AssetDto.builder()
                        .id(2L)
                        .customerId(1L)
                        .assetName("BTC")
                        .size(new BigDecimal("1.5"))
                        .usableSize(new BigDecimal("1.0"))
                        .build()
        );

        // Create balance request
        balanceRequest = new AssetBalanceRequest();
        balanceRequest.setCustomerId(1L);
        balanceRequest.setAssetName("TRY");
        balanceRequest.setAmount(1000.00);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getAssets_WhenAuthorized_ShouldReturnAssets() throws Exception {
        // Given
        Long customerId = 1L;
        when(authService.isOwnerOrAdmin(customerId)).thenReturn(true);
        when(assetService.getAssetsByCustomerId(customerId)).thenReturn(testAssets);

        // When/Then
        mockMvc.perform(get("/assets")
                        .param("customerId", customerId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetName").value("TRY"))
                .andExpect(jsonPath("$[0].size").value(10000.00))
                .andExpect(jsonPath("$[1].assetName").value("BTC"))
                .andExpect(jsonPath("$[1].size").value(1.5));

        verify(assetService).getAssetsByCustomerId(customerId);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getAssets_WhenUnauthorized_ShouldThrowException() throws Exception {
        // Given
        Long customerId = 1L;
        when(authService.isOwnerOrAdmin(customerId)).thenReturn(false);

        // When/Then
        mockMvc.perform(get("/assets")
                        .param("customerId", customerId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addAssetBalance_WhenAdmin_ShouldAddBalance() throws Exception {
        // Given
        when(authService.isAdmin()).thenReturn(true);

        AssetDto updatedAsset = AssetDto.builder()
                .id(1L)
                .customerId(1L)
                .assetName("TRY")
                .size(new BigDecimal("11000.00"))  // 10000 + 1000
                .usableSize(new BigDecimal("9000.00"))  // 8000 + 1000
                .build();

        when(assetService.addAssetBalance(any(AssetBalanceRequest.class))).thenReturn(updatedAsset);

        // When/Then
        mockMvc.perform(post("/assets/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetName").value("TRY"))
                .andExpect(jsonPath("$.size").value(11000.00))
                .andExpect(jsonPath("$.usableSize").value(9000.00));

        verify(assetService).addAssetBalance(any(AssetBalanceRequest.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void addAssetBalance_WhenNotAdmin_ShouldThrowException() throws Exception {
        // Given
        when(authService.isAdmin()).thenReturn(false);

        // When/Then
        mockMvc.perform(post("/assets/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceRequest)))
                .andExpect(status().isForbidden());
    }
} 