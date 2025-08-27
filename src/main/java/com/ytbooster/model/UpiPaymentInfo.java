package com.ytbooster.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "upi_payment_info", indexes = {
    @Index(name = "idx_adminid", columnList = "admin_id"),
    @Index(name = "idx_upi_admin", columnList = "upi_id, admin_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpiPaymentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upi_id", nullable = false, length = 100)
    @NotBlank(message = "UPI ID is required")
    @Size(max = 100, message = "UPI ID cannot exceed 100 characters")
    private String upiId;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    @NotBlank(message = "Account holder name is required")
    @Size(max = 255, message = "Account holder name cannot exceed 255 characters")
    private String accountHolderName;

    @Column(name = "note", length = 500)
    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;

    // 🔧 CRITICAL FIX: Specify LONGBLOB to handle large QR code images
    @Lob
    @Column(name = "qr_code")
    private byte[] qrCode;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
