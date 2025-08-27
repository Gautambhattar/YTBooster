package com.ytbooster.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long userId;
    private String link;
    private String orderDescription;
    private String status; // PENDING, SUCCESS, FAILED

    @Column(precision = 19, scale = 2)
    private BigDecimal amount; // Changed from Long → BigDecimal
    @CreatedDate
    private LocalDateTime createdAt;

    
}
