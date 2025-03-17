package com.ingbrokerage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingbrokerage.dto.CreateOrderRequest;
import com.ingbrokerage.dto.OrderDto;
import com.ingbrokerage.dto.OrderFilterRequest;
import com.ingbrokerage.model.Order;
import com.ingbrokerage.model.enums.OrderSide;
import com.ingbrokerage.model.enums.OrderStatus;
import com.ingbrokerage.repository.OrderRepository;
import com.ingbrokerage.security.jwt.JwtUtils;
import com.ingbrokerage.security.services.AuthorizationService;
import com.ingbrokerage.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(OrderControllerTest.TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private OrderService orderService;
    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private AuthorizationService authService;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private UserDetailsService userDetailsService;
    private OrderDto orderDto1;
    private OrderDto orderDto2;
    private CreateOrderRequest createOrderRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        // Create sample OrderDtos
        orderDto1 = OrderDto.builder()
                .id(1L)
                .customerId(1L)
                .assetName("BTC")
                .orderSide(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .size(new BigDecimal("0.1"))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        orderDto2 = OrderDto.builder()
                .id(2L)
                .customerId(1L)
                .assetName("ETH")
                .orderSide(OrderSide.SELL)
                .price(new BigDecimal("3000.00"))
                .size(new BigDecimal("2.0"))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        // Create a sample CreateOrderRequest
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(1L);
        createOrderRequest.setAssetName("BTC");
        createOrderRequest.setOrderSide(OrderSide.BUY);
        createOrderRequest.setPrice(new BigDecimal("50000.00"));
        createOrderRequest.setSize(new BigDecimal("0.1"));

        // Create a sample Order entity
        order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setAssetName("BTC");
        order.setOrderSide(OrderSide.BUY);
        order.setPrice(new BigDecimal("50000.00"));
        order.setSize(new BigDecimal("0.1"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreateDate(LocalDateTime.now());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createOrder_WhenValid_ShouldReturnCreatedOrder() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderDto1);
        when(authService.isOwnerOrAdmin(anyLong())).thenReturn(true);

        // When/Then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.assetName").value("BTC"))
                .andExpect(jsonPath("$.orderSide").value("BUY"))
                .andExpect(jsonPath("$.price").value(50000.00))
                .andExpect(jsonPath("$.size").value(0.1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getOrders_WithCustomerId_ShouldReturnOrders() throws Exception {
        // Given
        List<OrderDto> orders = Arrays.asList(orderDto1, orderDto2);
        OrderFilterRequest filterRequest = new OrderFilterRequest();
        filterRequest.setCustomerId(1L);
        when(orderService.getOrdersByFilter(any(OrderFilterRequest.class))).thenReturn(orders);
        when(authService.isOwnerOrAdmin(anyLong())).thenReturn(true);

        // When/Then
        mockMvc.perform(get("/orders")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetName").value("BTC"))
                .andExpect(jsonPath("$[1].assetName").value("ETH"));

        verify(orderService).getOrdersByFilter(any(OrderFilterRequest.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getOrderById_WhenExists_ShouldReturnOrder() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(orderDto1);
        when(authService.isOwnerOrAdmin(anyLong())).thenReturn(true);

        // When/Then
        mockMvc.perform(get("/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetName").value("BTC"));

        verify(orderService).getOrderById(1L);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void cancelOrder_WhenAuthorized_ShouldCancelOrder() throws Exception {
        // Given
        OrderDto canceledOrder = OrderDto.builder()
                .id(1L)
                .customerId(1L)
                .assetName("BTC")
                .orderSide(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .size(new BigDecimal("0.1"))
                .status(OrderStatus.CANCELED)
                .createDate(LocalDateTime.now())
                .build();

        when(orderService.cancelOrder(1L)).thenReturn(canceledOrder);
        when(orderService.getOrderById(1L)).thenReturn(orderDto1);
        when(authService.isOwnerOrAdmin(anyLong())).thenReturn(true);

        // When/Then
        mockMvc.perform(delete("/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(orderService).cancelOrder(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void matchOrder_WhenAdmin_ShouldMatchOrder() throws Exception {
        // Given
        OrderDto matchedOrder = OrderDto.builder()
                .id(1L)
                .customerId(1L)
                .assetName("BTC")
                .orderSide(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .size(new BigDecimal("0.1"))
                .status(OrderStatus.MATCHED)
                .createDate(LocalDateTime.now())
                .build();

        when(orderService.matchOrder(1L)).thenReturn(matchedOrder);
        when(orderService.getOrderById(1L)).thenReturn(orderDto1);
        when(authService.isAdmin()).thenReturn(true);

        // When/Then
        mockMvc.perform(put("/orders/1/match")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"));

        verify(orderService).matchOrder(1L);
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
} 