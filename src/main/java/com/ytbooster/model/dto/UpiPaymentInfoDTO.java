package com.ytbooster.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpiPaymentInfoDTO {

    private Long id;

    @NotBlank(message = "UPI ID is required")
    @Size(max = 100, message = "UPI ID cannot exceed 100 characters")
    private String upiId;

    @NotBlank(message = "Account holder name is required")
    @Size(max = 255, message = "Account holder name cannot exceed 255 characters")
    private String accountHolderName;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;

    @JsonIgnore
    private byte[] qrCode;

    private String qrBase64;

    @NotNull(message = "Admin ID is required")
    private Long adminId;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper method to set QR code and automatically generate base64
    public void setQrCode(byte[] qrCode) {
        this.qrCode = qrCode;
        if (qrCode != null && qrCode.length > 0) {
            this.qrBase64 = java.util.Base64.getEncoder().encodeToString(qrCode);
        } else {
            this.qrBase64 = null;
        }
    }

    // Custom builder method
    public static class UpiPaymentInfoDTOBuilder {
        public UpiPaymentInfoDTOBuilder withCurrentTimestamp() {
            this.createdAt = LocalDateTime.now();
            return this;
        }

        public UpiPaymentInfoDTOBuilder withQrCode(byte[] qrCode) {
            this.qrCode = qrCode;
            if (qrCode != null && qrCode.length > 0) {
                this.qrBase64 = java.util.Base64.getEncoder().encodeToString(qrCode);
            }
            return this;
        }
    }
}
