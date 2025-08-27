package com.ytbooster.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeDTO {

    private Long utr;
    private Long userId;

    private BigDecimal amount; // Changed from Long → BigDecimal

    private String email;
    private Long mobileNumber;

    private String paymentStatus; // e.g., SUCCESS, FAILED, PENDING
    private String paymentMethod;
    private String status;        // e.g., ACTIVE, CANCELLED
}
