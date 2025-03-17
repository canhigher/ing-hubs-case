package com.ingbrokerage.service;

import com.ingbrokerage.dto.AssetBalanceRequest;
import com.ingbrokerage.dto.AssetDto;
import com.ingbrokerage.model.Asset;
import com.ingbrokerage.model.enums.OrderSide;
import com.ingbrokerage.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private static final String TRY_ASSET_NAME = "TRY";

    private final AssetRepository assetRepository;

    @Override
    public List<AssetDto> getAssetsByCustomerId(Long customerId) {
        log.info("Getting assets for customer ID: {}", customerId);
        List<Asset> assets = assetRepository.findByCustomerId(customerId);
        return assets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long customerId, String assetName,
                                        OrderSide orderSide, BigDecimal size, BigDecimal price) {

        if (OrderSide.BUY == orderSide) {
            // For BUY orders, check TRY balance
            BigDecimal requiredAmount = size.multiply(price);
            Optional<Asset> tryAssetOpt = assetRepository.findByCustomerIdAndAssetName(customerId, TRY_ASSET_NAME);

            if (tryAssetOpt.isEmpty() || tryAssetOpt.get().getUsableSize().compareTo(requiredAmount) < 0) {
                log.warn("Insufficient TRY balance for customer ID: {}", customerId);
                return false;
            }

            return true;

        } else if (OrderSide.SELL == orderSide) {
            // For SELL orders, check the asset balance
            Optional<Asset> assetOpt = assetRepository.findByCustomerIdAndAssetName(customerId, assetName);

            if (assetOpt.isEmpty() || assetOpt.get().getUsableSize().compareTo(size) < 0) {
                log.warn("Insufficient {} balance for customer ID: {}", assetName, customerId);
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void updateAssetUsableBalanceForOrderCreation(Long customerId, String assetName,
                                                         OrderSide orderSide, BigDecimal size, BigDecimal price) {

        if (OrderSide.BUY == orderSide) {
            // For BUY orders, update TRY balance
            BigDecimal requiredAmount = size.multiply(price);
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(customerId, TRY_ASSET_NAME)
                    .orElseThrow(() -> new RuntimeException("Insufficient balance: TRY asset not found for customer ID: " + customerId));

            if (tryAsset.getUsableSize().compareTo(requiredAmount) < 0) {
                throw new RuntimeException("Insufficient balance for customer ID: " + customerId);
            }

            tryAsset.setUsableSize(tryAsset.getUsableSize().subtract(requiredAmount));
            assetRepository.save(tryAsset);
            log.info("Updated TRY usable balance for customer ID: {} after order creation", customerId);

        } else if (OrderSide.SELL == orderSide) {
            // For SELL orders, update the asset balance
            Asset asset = assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                    .orElseThrow(() -> new RuntimeException("Insufficient balance: " + assetName + " asset not found for customer ID: " + customerId));

            if (asset.getUsableSize().compareTo(size) < 0) {
                throw new RuntimeException("Insufficient balance for " + assetName + " for customer ID: " + customerId);
            }

            asset.setUsableSize(asset.getUsableSize().subtract(size));
            assetRepository.save(asset);
            log.info("Updated {} usable balance for customer ID: {} after order creation", assetName, customerId);
        }
    }

    @Override
    @Transactional
    public void updateAssetUsableBalanceForOrderCancellation(Long customerId, String assetName,
                                                             OrderSide orderSide, BigDecimal size, BigDecimal price) {

        if (OrderSide.BUY == orderSide) {
            // For BUY orders, restore TRY balance
            BigDecimal restoreAmount = size.multiply(price);
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(customerId, TRY_ASSET_NAME)
                    .orElseThrow(() -> new RuntimeException("TRY asset not found for customer ID: " + customerId));

            tryAsset.setUsableSize(tryAsset.getUsableSize().add(restoreAmount));
            assetRepository.save(tryAsset);
            log.info("Restored TRY usable balance for customer ID: {} after order cancellation", customerId);

        } else if (OrderSide.SELL == orderSide) {
            // For SELL orders, restore the asset balance
            Asset asset = assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                    .orElseThrow(() -> new RuntimeException(assetName + " asset not found for customer ID: " + customerId));

            asset.setUsableSize(asset.getUsableSize().add(size));
            assetRepository.save(asset);
            log.info("Restored {} usable balance for customer ID: {} after order cancellation", assetName, customerId);
        }
    }

    @Override
    @Transactional
    public void updateAssetBalanceForOrderMatching(Long customerId, String assetName,
                                                   OrderSide orderSide, BigDecimal size, BigDecimal price) {

        if (OrderSide.BUY == orderSide) {
            // For BUY orders:
            // 1. Reduce TRY total balance (already reduced usable balance during order creation)
            // 2. Increase the asset total and usable balance

            // Update TRY asset
            BigDecimal totalAmount = size.multiply(price);
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(customerId, TRY_ASSET_NAME)
                    .orElseThrow(() -> new RuntimeException("TRY asset not found for customer ID: " + customerId));

            tryAsset.setSize(tryAsset.getSize().subtract(totalAmount));
            assetRepository.save(tryAsset);

            // Update or create the asset
            Asset asset = assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                    .orElse(Asset.builder()
                            .customerId(customerId)
                            .assetName(assetName)
                            .size(BigDecimal.ZERO)
                            .usableSize(BigDecimal.ZERO)
                            .build());

            asset.setSize(asset.getSize().add(size));
            asset.setUsableSize(asset.getUsableSize().add(size));
            assetRepository.save(asset);

            log.info("Updated balances for BUY order match: customer ID {}, asset {}", customerId, assetName);

        } else if (OrderSide.SELL == orderSide) {
            // For SELL orders:
            // 1. Reduce the asset total balance (already reduced usable balance during order creation)
            // 2. Increase TRY total and usable balance

            // Update asset
            Asset asset = assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                    .orElseThrow(() -> new RuntimeException(assetName + " asset not found for customer ID: " + customerId));

            asset.setSize(asset.getSize().subtract(size));
            assetRepository.save(asset);

            // Update TRY asset
            BigDecimal totalAmount = size.multiply(price);
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(customerId, TRY_ASSET_NAME)
                    .orElse(Asset.builder()
                            .customerId(customerId)
                            .assetName(TRY_ASSET_NAME)
                            .size(BigDecimal.ZERO)
                            .usableSize(BigDecimal.ZERO)
                            .build());

            tryAsset.setSize(tryAsset.getSize().add(totalAmount));
            tryAsset.setUsableSize(tryAsset.getUsableSize().add(totalAmount));
            assetRepository.save(tryAsset);

            log.info("Updated balances for SELL order match: customer ID {}, asset {}", customerId, assetName);
        }
    }

    @Override
    @Transactional
    public AssetDto addAssetBalance(AssetBalanceRequest request) {
        log.info("Adding balance for customer ID: {}, asset: {}, amount: {}",
                request.getCustomerId(), request.getAssetName(), request.getAmount());

        Asset asset = assetRepository.findByCustomerIdAndAssetName(
                        request.getCustomerId(), request.getAssetName())
                .orElseGet(() -> {
                    // Create new asset if it doesn't exist
                    Asset newAsset = new Asset();
                    newAsset.setCustomerId(request.getCustomerId());
                    newAsset.setAssetName(request.getAssetName());
                    newAsset.setSize(BigDecimal.ZERO);
                    newAsset.setUsableSize(BigDecimal.ZERO);
                    return newAsset;
                });

        // Update asset balance
        BigDecimal currentSize = asset.getSize();
        BigDecimal currentUsableSize = asset.getUsableSize();
        BigDecimal newAmount = BigDecimal.valueOf(request.getAmount());

        asset.setSize(currentSize.add(newAmount));
        asset.setUsableSize(currentUsableSize.add(newAmount));

        Asset savedAsset = assetRepository.save(asset);
        log.info("Asset balance updated: {}", savedAsset);

        return mapToDto(savedAsset);
    }

    private AssetDto mapToDto(Asset asset) {
        return AssetDto.builder()
                .id(asset.getId())
                .customerId(asset.getCustomerId())
                .assetName(asset.getAssetName())
                .size(asset.getSize())
                .usableSize(asset.getUsableSize())
                .build();
    }
} 