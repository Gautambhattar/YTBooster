package com.ytbooster.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferHistoryDTO {

    private Long id;

    private Long userId;
    private Long referredUserId;

    @Builder.Default
    private BigDecimal totalRecharge = BigDecimal.ZERO;  // Total recharge done by referred user

    @Builder.Default
    private boolean rewarded = false;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;  // Reward amount (default 0, set 20 when eligible)

    private String status; // PENDING, CREDITED
    private LocalDate lastUpdate;
}
