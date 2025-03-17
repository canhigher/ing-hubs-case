package com.ingbrokerage.service;

import com.ingbrokerage.dto.AssetBalanceRequest;
import com.ingbrokerage.dto.AssetDto;
import com.ingbrokerage.model.Asset;
import com.ingbrokerage.model.enums.OrderSide;
import com.ingbrokerage.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetServiceImpl assetService;

    private Asset tryAsset;
    private Asset btcAsset;
    private AssetBalanceRequest balanceRequest;

    @BeforeEach
    void setUp() {
        tryAsset = new Asset();
        tryAsset.setId(1L);
        tryAsset.setCustomerId(1L);
        tryAsset.setAssetName("TRY");
        tryAsset.setSize(new BigDecimal("10000.00"));
        tryAsset.setUsableSize(new BigDecimal("8000.00"));

        btcAsset = new Asset();
        btcAsset.setId(2L);
        btcAsset.setCustomerId(1L);
        btcAsset.setAssetName("BTC");
        btcAsset.setSize(new BigDecimal("1.5"));
        btcAsset.setUsableSize(new BigDecimal("1.0"));

        balanceRequest = AssetBalanceRequest.builder()
                .customerId(1L)
                .assetName("TRY")
                .amount(1000.0)
                .build();
    }

    @Test
    void getAssetsByCustomerId_ShouldReturnAssets() {
        // Given
        List<Asset> assets = Arrays.asList(tryAsset, btcAsset);
        when(assetRepository.findByCustomerId(1L)).thenReturn(assets);

        // When
        List<AssetDto> result = assetService.getAssetsByCustomerId(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals("TRY", result.get(0).getAssetName());
        assertEquals(new BigDecimal("10000.00"), result.get(0).getSize());
        assertEquals("BTC", result.get(1).getAssetName());
        verify(assetRepository).findByCustomerId(1L);
    }

    @Test
    void hasSufficientBalance_ForBuy_WhenSufficient_ShouldReturnTrue() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(1L, "TRY"))
                .thenReturn(Optional.of(tryAsset));

        // When
        boolean result = assetService.hasSufficientBalance(
                1L, "BTC", OrderSide.BUY, new BigDecimal("0.1"), new BigDecimal("50000"));

        // Then
        assertTrue(result);
    }

    @Test
    void hasSufficientBalance_ForBuy_WhenInsufficient_ShouldReturnFalse() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(1L, "TRY"))
                .thenReturn(Optional.of(tryAsset));

        // When - Try to buy more than available (0.2 BTC at 50000 = 10000, but only 8000 usable)
        boolean result = assetService.hasSufficientBalance(
                1L, "BTC", OrderSide.BUY, new BigDecimal("0.2"), new BigDecimal("50000"));

        // Then
        assertFalse(result);
    }

    @Test
    void hasSufficientBalance_ForSell_WhenSufficient_ShouldReturnTrue() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(1L, "BTC"))
                .thenReturn(Optional.of(btcAsset));

        // When
        boolean result = assetService.hasSufficientBalance(
                1L, "BTC", OrderSide.SELL, new BigDecimal("0.5"), new BigDecimal("50000"));

        // Then
        assertTrue(result);
    }

    @Test
    void hasSufficientBalance_ForSell_WhenInsufficient_ShouldReturnFalse() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(1L, "BTC"))
                .thenReturn(Optional.of(btcAsset));

        // When - Try to sell more than available (1.2 BTC, but only 1.0 usable)
        boolean result = assetService.hasSufficientBalance(
                1L, "BTC", OrderSide.SELL, new BigDecimal("1.2"), new BigDecimal("50000"));

        // Then
        assertFalse(result);
    }

    @Test
    void addAssetBalance_WhenAssetExists_ShouldUpdateBalance() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(1L, "TRY"))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(tryAsset);

        // When
        AssetDto result = assetService.addAssetBalance(balanceRequest);

        // Then
        verify(assetRepository).findByCustomerIdAndAssetName(1L, "TRY");
        verify(assetRepository).save(any(Asset.class));
        
        assertEquals("TRY", result.getAssetName());
        // Verify the balance was increased by the amount in the request
        assertTrue(result.getSize().compareTo(new BigDecimal("10000.00")) > 0);
    }

    @Test
    void addAssetBalance_WhenAssetDoesNotExist_ShouldCreateNewAsset() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(1L, "TRY"))
                .thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenReturn(tryAsset);

        // When
        AssetDto result = assetService.addAssetBalance(balanceRequest);

        // Then
        verify(assetRepository).findByCustomerIdAndAssetName(1L, "TRY");
        verify(assetRepository).save(any(Asset.class));
        
        assertEquals("TRY", result.getAssetName());
    }
} 