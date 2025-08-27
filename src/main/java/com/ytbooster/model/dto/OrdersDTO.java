package com.ytbooster.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdersDTO {

    private Long orderId;
    private Long userId;
    private String link;
    private String orderDescription;
    private String status; // PENDING, SUCCESS, FAILED
    private BigDecimal amount; // Changed from Long → BigDecimal
    private LocalDateTime createdAt;
}
