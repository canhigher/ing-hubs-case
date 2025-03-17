package com.ingbrokerage.service;

import com.ingbrokerage.dto.CreateOrderRequest;
import com.ingbrokerage.dto.OrderDto;
import com.ingbrokerage.dto.OrderFilterRequest;
import com.ingbrokerage.model.Order;
import com.ingbrokerage.model.enums.OrderSide;
import com.ingbrokerage.model.enums.OrderStatus;
import com.ingbrokerage.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private CreateOrderRequest createOrderRequest;
    private Order order;
    private OrderFilterRequest filterRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        createOrderRequest = CreateOrderRequest.builder()
                .customerId(1L)
                .assetName("BTC")
                .orderSide(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .size(new BigDecimal("0.1"))
                .build();

        order = Order.builder()
                .id(1L)
                .customerId(1L)
                .assetName("BTC")
                .orderSide(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .size(new BigDecimal("0.1"))
                .status(OrderStatus.PENDING)
                .createDate(now)
                .build();

        filterRequest = OrderFilterRequest.builder()
                .customerId(1L)
                .startDate(now.minusDays(7))
                .endDate(now)
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void createOrder_WhenBalanceSufficient_ShouldCreateOrder() {
        // Given
        when(assetService.hasSufficientBalance(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        
        // When
        OrderDto result = orderService.createOrder(createOrderRequest);
        
        // Then
        assertNotNull(result);
        assertEquals("BTC", result.getAssetName());
        assertEquals(OrderSide.BUY, result.getOrderSide());
        
        verify(assetService).hasSufficientBalance(
                eq(1L), eq("BTC"), eq(OrderSide.BUY), 
                eq(new BigDecimal("0.1")), eq(new BigDecimal("50000.00")));
        verify(assetService).updateAssetUsableBalanceForOrderCreation(
                eq(1L), eq("BTC"), eq(OrderSide.BUY), 
                eq(new BigDecimal("0.1")), eq(new BigDecimal("50000.00")));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_WhenBalanceInsufficient_ShouldThrowException() {
        // Given
        when(assetService.hasSufficientBalance(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(false);
        
        // When/Then
        assertThrows(RuntimeException.class, () -> orderService.createOrder(createOrderRequest));
        
        verify(assetService).hasSufficientBalance(
                eq(1L), eq("BTC"), eq(OrderSide.BUY), 
                eq(new BigDecimal("0.1")), eq(new BigDecimal("50000.00")));
        verify(assetService, never()).updateAssetUsableBalanceForOrderCreation(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersByFilter_ShouldReturnFilteredOrders() {
        // Given
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByCustomerIdAndCreateDateBetweenAndStatus(
                eq(1L), eq(filterRequest.getStartDate()), eq(filterRequest.getEndDate()), eq(OrderStatus.PENDING)))
                .thenReturn(orders);
        
        // When
        List<OrderDto> result = orderService.getOrdersByFilter(filterRequest);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BTC", result.get(0).getAssetName());
        
        verify(orderRepository).findByCustomerIdAndCreateDateBetweenAndStatus(
                eq(1L), eq(filterRequest.getStartDate()), eq(filterRequest.getEndDate()), eq(OrderStatus.PENDING));
    }

    @Test
    void getOrdersByFilter_WhenNoStatusSpecified_ShouldFilterWithoutStatus() {
        // Given
        filterRequest.setStatus(null);
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByCustomerIdAndCreateDateBetween(
                eq(1L), eq(filterRequest.getStartDate()), eq(filterRequest.getEndDate())))
                .thenReturn(orders);
        
        // When
        List<OrderDto> result = orderService.getOrdersByFilter(filterRequest);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(orderRepository).findByCustomerIdAndCreateDateBetween(
                eq(1L), eq(filterRequest.getStartDate()), eq(filterRequest.getEndDate()));
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // When
        OrderDto result = orderService.getOrderById(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("BTC", result.getAssetName());
        
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_WhenNotExists_ShouldThrowException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(RuntimeException.class, () -> orderService.getOrderById(1L));
        
        verify(orderRepository).findById(1L);
    }

    @Test
    void cancelOrder_WhenPending_ShouldCancelOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        OrderDto result = orderService.cancelOrder(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELED, result.getStatus());
        
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(orderCaptor.capture());
        verify(assetService).updateAssetUsableBalanceForOrderCancellation(
                eq(1L), eq("BTC"), eq(OrderSide.BUY), 
                eq(new BigDecimal("0.1")), eq(new BigDecimal("50000.00")));
        
        Order capturedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.CANCELED, capturedOrder.getStatus());
    }

    @Test
    void cancelOrder_WhenAlreadyCanceled_ShouldThrowException() {
        // Given
        order.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // When/Then
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(1L));
        
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(assetService, never()).updateAssetUsableBalanceForOrderCancellation(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void cancelOrder_WhenAlreadyMatched_ShouldThrowException() {
        // Given
        order.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // When/Then
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(1L));
        
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(assetService, never()).updateAssetUsableBalanceForOrderCancellation(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void matchOrder_WhenPending_ShouldMatchOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        OrderDto result = orderService.matchOrder(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.MATCHED, result.getStatus());
        
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(orderCaptor.capture());
        verify(assetService).updateAssetBalanceForOrderMatching(
                eq(1L), eq("BTC"), eq(OrderSide.BUY), 
                eq(new BigDecimal("0.1")), eq(new BigDecimal("50000.00")));
        
        Order capturedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.MATCHED, capturedOrder.getStatus());
    }

    @Test
    void matchOrder_WhenAlreadyMatched_ShouldThrowException() {
        // Given
        order.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // When/Then
        assertThrows(RuntimeException.class, () -> orderService.matchOrder(1L));
        
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(assetService, never()).updateAssetBalanceForOrderMatching(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void matchOrder_WhenCanceled_ShouldThrowException() {
        // Given
        order.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // When/Then
        assertThrows(RuntimeException.class, () -> orderService.matchOrder(1L));
        
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(assetService, never()).updateAssetBalanceForOrderMatching(
                anyLong(), anyString(), any(OrderSide.class), any(BigDecimal.class), any(BigDecimal.class));
    }
} 