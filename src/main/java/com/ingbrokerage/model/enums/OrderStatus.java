package com.ingbrokerage.model.enums;

/**
 * Represents the status of an order
 * PENDING - Order is created but not matched yet
 * MATCHED - Order is matched and executed
 * CANCELED - Order is canceled by the customer
 */
public enum OrderStatus {
    PENDING, MATCHED, CANCELED
} 