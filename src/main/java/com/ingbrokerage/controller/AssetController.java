package com.ingbrokerage.controller;

import com.ingbrokerage.dto.AssetDto;
import com.ingbrokerage.dto.AssetBalanceRequest;
import com.ingbrokerage.exception.AccessDeniedException;
import com.ingbrokerage.security.services.AuthorizationService;
import com.ingbrokerage.service.AssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AssetController {

    private final AssetService assetService;
    private final AuthorizationService authService;

    /**
     * Get assets for a customer
     *
     * @param customerId the customer ID
     * @return list of assets
     */
    @GetMapping
    public ResponseEntity<List<AssetDto>> getAssets(@RequestParam @NotNull Long customerId) {
        log.info("GET /assets request received with customerId: {}", customerId);

        // Ensure user can only access their own assets unless they're an admin
        if (!authService.isOwnerOrAdmin(customerId)) {
            throw new AccessDeniedException("You are not authorized to view these assets");
        }

        List<AssetDto> assets = assetService.getAssetsByCustomerId(customerId);
        return ResponseEntity.ok(assets);
    }

    /**
     * Add balance to a customer's asset (admin only)
     *
     * @param request the asset balance request
     * @return the updated asset
     */
    @PostMapping("/balance")
    public ResponseEntity<AssetDto> addAssetBalance(@RequestBody @Valid AssetBalanceRequest request) {
        log.info("POST /assets/balance request received for customer: {}", request.getCustomerId());

        // Only admins can add balance
        if (!authService.isAdmin()) {
            throw new AccessDeniedException("Access denied: Admin role required");
        }

        AssetDto updatedAsset = assetService.addAssetBalance(request);
        return ResponseEntity.ok(updatedAsset);
    }
}