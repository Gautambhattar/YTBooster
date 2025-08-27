package com.ytbooster.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeHistoryDTO {

    private Long utr;
    private Long userId;

    private BigDecimal amount; // Changed from Long → BigDecimal

    private String email;
    private Long mobileNumber;

    private LocalDateTime createdAt;

    private String paymentStatus; // e.g., SUCCESS, FAILED, PENDING
    private String paymentMethod;
    private String status;        // e.g., ACTIVE, CANCELLED

    /**
     * Format createdAt for display
     */
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }
}
