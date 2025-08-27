package com.ytbooster.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "recharge_history")
public class RechargeHistory {

    @Id
    private Long utr; // Unique transaction reference, set manually or via service

    private Long userId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount; // Changed from Long → BigDecimal

    private String email;
    private Long mobileNumber;
    private String paymentMethod;

    private String paymentStatus; // e.g., SUCCESS, FAILED, PENDING
    private String status;        // e.g., ACTIVE, CANCELLED

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
