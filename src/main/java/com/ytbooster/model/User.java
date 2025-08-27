package com.ytbooster.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ytbooster.Util.ReferralCodeGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optimized User entity for concurrency & maintainability.
 * - Uses @Version for optimistic locking on wallet/updates
 * - wallet uses BigDecimal for money safety (instead of Long)
 * - createdAt / updatedAt handled by Hibernate annotations
 * - minimal logic in @PrePersist (lightweight defaults only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "UserDetails",
    indexes = {
        @Index(name = "idx_user_email", columnList = "UEmail", unique = true),
        @Index(name = "idx_user_referral", columnList = "UReferCode")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UId")
    private Long id;

    @Column(name = "UName", nullable = false)
    private String name;

    @Column(name = "UEmail", nullable = false, unique = true, length = 150)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Column(name = "UPassword", nullable = false)
    private String password;

    @Column(name = "UReferCode", length = 20, unique = true)
    private String referCode;

    /**
     * Wallet represented as BigDecimal for monetary accuracy.
     * Default = 0.00 (set in PrePersist if null).
     */
    @Column(name = "UWallet", nullable = false, precision = 19, scale = 2)
    private BigDecimal wallet;

    @Column(name = "URole", nullable = false, length = 50)
    private String role;

    @Column(name = "UStatus", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private boolean authenticated;
    private boolean online;
    @UpdateTimestamp
    private LocalDateTime lastSeen;

    /**
     * Optimistic lock field — ensures safe concurrent updates
     */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.referCode == null) {
            this.referCode = new ReferralCodeGenerator().generateReferralCode();
        }
        if (this.wallet == null) {
            this.wallet = BigDecimal.ZERO;
        }
    }
}
