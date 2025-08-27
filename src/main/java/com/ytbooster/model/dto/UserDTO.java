package com.ytbooster.model.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring User data outside the persistence layer.
 * - Keeps only business-facing fields (no @Version, timestamps, etc.)
 * - Wallet kept as BigDecimal for currency safety
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String password; // Consider omitting in responses

    private String referCode;

    /** Wallet as BigDecimal (matches entity) */
    private java.math.BigDecimal wallet;

    private String role;

    private String status;
    private boolean authenticated;
    private boolean online;
  
    private LocalDateTime lastSeen;
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }
}
