package com.ingbrokerage.service;

import com.ingbrokerage.dto.CreateOrderRequest;
import com.ingbrokerage.dto.OrderDto;
import com.ingbrokerage.dto.OrderFilterRequest;

import java.util.List;

public interface OrderService {
    
    /**
     * Create a new order
     *
     * @param createOrderRequest the order creation request
     * @return the created order
     */
    OrderDto createOrder(CreateOrderRequest createOrderRequest);
    
    /**
     * Get orders by filter
     *
     * @param filterRequest the filter request
     * @return list of orders matching the filter
     */
    List<OrderDto> getOrdersByFilter(OrderFilterRequest filterRequest);
    
    /**
     * Get a single order by ID
     *
     * @param orderId the order ID
     * @return the order if found
     */
    OrderDto getOrderById(Long orderId);
    
    /**
     * Cancel an order
     *
     * @param orderId the order ID
     * @return the canceled order
     */
    OrderDto cancelOrder(Long orderId);
    
    /**
     * Match an order (bonus feature)
     *
     * @param orderId the order ID
     * @return the matched order
     */
    OrderDto matchOrder(Long orderId);
} 