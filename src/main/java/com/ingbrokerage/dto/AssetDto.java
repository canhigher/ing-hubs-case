package com.ingbrokerage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDto {
    
    private Long id;
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotBlank(message = "Asset name is required")
    private String assetName;
    
    @NotNull(message = "Size is required")
    @PositiveOrZero(message = "Size must be zero or positive")
    private BigDecimal size;
    
    @NotNull(message = "Usable size is required")
    @PositiveOrZero(message = "Usable size must be zero or positive")
    private BigDecimal usableSize;
} 