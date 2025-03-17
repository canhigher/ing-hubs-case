package com.ingbrokerage.controller;

import com.ingbrokerage.dto.CreateOrderRequest;
import com.ingbrokerage.dto.OrderDto;
import com.ingbrokerage.dto.OrderFilterRequest;
import com.ingbrokerage.exception.AccessDeniedException;
import com.ingbrokerage.model.Order;
import com.ingbrokerage.model.enums.OrderStatus;
import com.ingbrokerage.repository.OrderRepository;
import com.ingbrokerage.security.services.AuthorizationService;
import com.ingbrokerage.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final AuthorizationService authService;

    /**
     * Create a new order
     *
     * @param createOrderRequest the order creation request
     * @return the created order
     */
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody @Valid CreateOrderRequest createOrderRequest) {
        log.info("POST /orders request received for customer: {}", createOrderRequest.getCustomerId());

        // Ensure user only creates orders for themselves
        if (!authService.isOwnerOrAdmin(createOrderRequest.getCustomerId())) {
            throw new AccessDeniedException("You are not authorized to create orders for this customer");
        }

        OrderDto createdOrder = orderService.createOrder(createOrderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    /**
     * Get orders with optional filtering
     * Returns all orders if no filters are specified and user is admin
     * Regular users can only see their own orders
     *
     * @param customerId   the customer ID (optional)
     * @param startDateStr the start date (optional)
     * @param endDateStr   the end date (optional)
     * @param status       order status (optional)
     * @return list of orders matching the filters or all orders if no filters specified
     */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr,
            @RequestParam(required = false) OrderStatus status) {

        // If no filters provided at all, return all orders for admin, or reject for non-admin
        if (customerId == null && startDateStr == null && endDateStr == null && status == null) {
            log.info("GET /orders request received without filters");

            // Only admins can see all orders
            if (!authService.isAdmin()) {
                throw new AccessDeniedException("Access denied: You must specify your own customerId");
            }

            List<Order> allOrders = orderRepository.findAll();
            List<OrderDto> orderDtos = allOrders.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(orderDtos);
        }

        // Regular users can only see their own orders
        if (customerId != null && !authService.isOwnerOrAdmin(customerId)) {
            throw new AccessDeniedException("You are not authorized to view orders for this customer");
        }

        // Handle query parameter filtering
        log.info("GET /orders request received with query parameters for customer: {}", customerId);

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        try {
            // Parse dates if provided
            if (startDateStr != null) {
                startDate = LocalDateTime.parse(startDateStr);
            } else {
                startDate = LocalDateTime.now().minusDays(30); // Default to 30 days ago
            }

            if (endDateStr != null) {
                endDate = LocalDateTime.parse(endDateStr);
            } else {
                endDate = LocalDateTime.now(); // Default to current time
            }

            OrderFilterRequest filterRequest = OrderFilterRequest.builder()
                    .customerId(customerId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .status(status)
                    .build();

            List<OrderDto> orders = orderService.getOrdersByFilter(filterRequest);
            return ResponseEntity.ok(orders);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid date format. Use ISO format (yyyy-MM-ddTHH:mm:ss)");
        }
    }

    /**
     * Get a single order by ID
     *
     * @param orderId the order ID
     * @return the order if found
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long orderId) {
        log.info("GET /orders/{} request received", orderId);
        OrderDto order = orderService.getOrderById(orderId);

        // Ensure user can only access their own orders unless they're an admin
        if (!authService.isOwnerOrAdmin(order.getCustomerId())) {
            throw new AccessDeniedException("You are not authorized to access this order");
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Cancel an order
     *
     * @param orderId the order ID
     * @return the canceled order
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long orderId) {
        log.info("DELETE /orders/ request received for order: {}", orderId);

        // Check ownership before canceling
        OrderDto order = orderService.getOrderById(orderId);
        if (!authService.isOwnerOrAdmin(order.getCustomerId())) {
            throw new AccessDeniedException("You are not authorized to cancel this order");
        }

        OrderDto canceledOrder = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(canceledOrder);
    }

    /**
     * Match an order (admin only)
     *
     * @param orderId the order ID
     * @return the matched order
     */
    @PutMapping("/{orderId}/match")
    public ResponseEntity<OrderDto> matchOrder(@PathVariable Long orderId) {
        log.info("PUT /orders/{}/match request received", orderId);

        // Ensure only admins can match orders
        if (!authService.isAdmin()) {
            throw new AccessDeniedException("Access denied: Admin role required");
        }

        OrderDto matchedOrder = orderService.matchOrder(orderId);
        return ResponseEntity.ok(matchedOrder);
    }

    /**
     * Map Order entity to OrderDto
     */
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