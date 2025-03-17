package com.ingbrokerage.service;

import com.ingbrokerage.dto.CreateOrderRequest;
import com.ingbrokerage.dto.OrderDto;
import com.ingbrokerage.dto.OrderFilterRequest;
import com.ingbrokerage.model.Order;
import com.ingbrokerage.model.enums.OrderStatus;
import com.ingbrokerage.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AssetService assetService;

    @Override
    @Transactional
    public OrderDto createOrder(CreateOrderRequest createOrderRequest) {
        log.info("Creating order for customer ID: {}, asset: {}", 
                createOrderRequest.getCustomerId(), createOrderRequest.getAssetName());
        
        // Check balance
        if (!assetService.hasSufficientBalance(
                createOrderRequest.getCustomerId(),
                createOrderRequest.getAssetName(),
                createOrderRequest.getOrderSide(),
                createOrderRequest.getSize(),
                createOrderRequest.getPrice())) {
            throw new RuntimeException("Insufficient balance for order creation");
        }
        
        // Update asset usable balance
        assetService.updateAssetUsableBalanceForOrderCreation(
                createOrderRequest.getCustomerId(),
                createOrderRequest.getAssetName(),
                createOrderRequest.getOrderSide(),
                createOrderRequest.getSize(),
                createOrderRequest.getPrice());
        
        // Create order
        Order order = Order.builder()
                .customerId(createOrderRequest.getCustomerId())
                .assetName(createOrderRequest.getAssetName())
                .orderSide(createOrderRequest.getOrderSide())
                .size(createOrderRequest.getSize())
                .price(createOrderRequest.getPrice())
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());
        
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByFilter(OrderFilterRequest filterRequest) {
        log.info("Getting orders for customer ID: {} between {} and {}", 
                filterRequest.getCustomerId(), filterRequest.getStartDate(), filterRequest.getEndDate());
        
        List<Order> orders;
        
        if (filterRequest.getStatus() != null) {
            orders = orderRepository.findByCustomerIdAndCreateDateBetweenAndStatus(
                    filterRequest.getCustomerId(),
                    filterRequest.getStartDate(),
                    filterRequest.getEndDate(),
                    filterRequest.getStatus());
        } else {
            orders = orderRepository.findByCustomerIdAndCreateDateBetween(
                    filterRequest.getCustomerId(),
                    filterRequest.getStartDate(),
                    filterRequest.getEndDate());
        }
        
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        log.info("Getting order with ID: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        return mapToDto(order);
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long orderId) {
        log.info("Canceling order ID: {}", orderId);
        
        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // Validate order status
        if (OrderStatus.PENDING != order.getStatus()) {
            throw new RuntimeException("Only PENDING orders can be canceled. Current status: " + order.getStatus());
        }
        
        // Update asset usable balance
        assetService.updateAssetUsableBalanceForOrderCancellation(
                order.getCustomerId(),
                order.getAssetName(),
                order.getOrderSide(),
                order.getSize(),
                order.getPrice());
        
        // Update order status
        order.setStatus(OrderStatus.CANCELED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order canceled with ID: {}", savedOrder.getId());
        
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto matchOrder(Long orderId) {
        log.info("Matching order with ID: {}", orderId);
        
        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // Validate order status
        if (OrderStatus.PENDING != order.getStatus()) {
            throw new RuntimeException("Only PENDING orders can be matched. Current status: " + order.getStatus());
        }
        
        // Update asset balance
        assetService.updateAssetBalanceForOrderMatching(
                order.getCustomerId(),
                order.getAssetName(),
                order.getOrderSide(),
                order.getSize(),
                order.getPrice());
        
        // Update order status
        order.setStatus(OrderStatus.MATCHED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order matched with ID: {}", savedOrder.getId());
        
        return mapToDto(savedOrder);
    }

    private OrderDto mapToDto(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .assetName(order.getAssetName())
                .orderSide(order.getOrderSide())
                .size(order.getSize())
                .price(order.getPrice())
                .status(order.getStatus())
                .createDate(order.getCreateDate())
                .build();
    }
} 