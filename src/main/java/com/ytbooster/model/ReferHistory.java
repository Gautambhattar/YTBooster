package com.ytbooster.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "refer_history")
public class ReferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;          // The referrer (who owns the referral code)
    private Long referredUserId;  // The user who registered with the referral

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal totalRecharge = BigDecimal.ZERO;  // Total recharge done by referred user

    @Builder.Default
    private boolean rewarded = false; // True once reward credited

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;    // Reward amount (default 0, set 20 when eligible)

    private String status;        // PENDING, CREDITED

    private LocalDate lastUpdate; // Tracks last calculation date
}
