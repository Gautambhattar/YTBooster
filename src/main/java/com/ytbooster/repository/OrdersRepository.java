package com.ytbooster.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ytbooster.model.Orders;

import jakarta.persistence.LockModeType;

/**
 * Repository for Orders
 * - Provides common queries
 * - Optional: Locking for concurrency-safe operations
 */
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByUserId(Long userId);

    List<Orders> findByStatus(String status);

    List<Orders> findByUserIdAndStatus(Long userId, String status);

    Orders findByOrderId(Long orderId);

    // Optional: Concurrency-safe read for updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Orders o WHERE o.orderId = :orderId")
    Orders findByOrderIdForUpdate(@Param("orderId") Long orderId);
}
