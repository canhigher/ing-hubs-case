package com.ingbrokerage.repository;

import com.ingbrokerage.model.Order;
import com.ingbrokerage.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find orders for a customer within a date range
     * 
     * @param customerId the ID of the customer
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return a list of orders for the customer within the date range
     */
    List<Order> findByCustomerIdAndCreateDateBetween(Long customerId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find orders for a customer within a date range with a specific status
     * 
     * @param customerId the ID of the customer
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param status the status of the orders
     * @return a list of orders with the given status for the customer within the date range
     */
    List<Order> findByCustomerIdAndCreateDateBetweenAndStatus(Long customerId, LocalDateTime startDate, 
                                                              LocalDateTime endDate, OrderStatus status);
} 