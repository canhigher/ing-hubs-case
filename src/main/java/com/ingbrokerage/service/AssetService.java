package com.ingbrokerage.service;

import com.ingbrokerage.dto.AssetBalanceRequest;
import com.ingbrokerage.dto.AssetDto;
import com.ingbrokerage.model.enums.OrderSide;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AssetService {

    /**
     * Get assets by customer ID
     */
    List<AssetDto> getAssetsByCustomerId(Long customerId);

    @Transactional(readOnly = true)
    boolean hasSufficientBalance(Long customerId, String assetName,
                                 OrderSide orderSide, BigDecimal size, BigDecimal price);

    @Transactional
    void updateAssetUsableBalanceForOrderCreation(Long customerId, String assetName,
                                                  OrderSide orderSide, BigDecimal size, BigDecimal price);

    @Transactional
    void updateAssetUsableBalanceForOrderCancellation(Long customerId, String assetName,
                                                      OrderSide orderSide, BigDecimal size, BigDecimal price);

    @Transactional
    void updateAssetBalanceForOrderMatching(Long customerId, String assetName,
                                            OrderSide orderSide, BigDecimal size, BigDecimal price);

    @Transactional
    AssetDto addAssetBalance(AssetBalanceRequest request);
}