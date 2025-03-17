package com.ingbrokerage.dto;

import com.ingbrokerage.model.enums.OrderSide;
import com.ingbrokerage.model.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    
    private Long id;
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotBlank(message = "Asset name is required")
    private String assetName;
    
    @NotNull(message = "Order side is required")
    private OrderSide orderSide;
    
    @NotNull(message = "Size is required")
    @Positive(message = "Size must be positive")
    private BigDecimal size;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    private OrderStatus status;
    
    private LocalDateTime createDate;
} 