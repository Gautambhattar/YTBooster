package com.ytbooster.controller.admin;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ytbooster.model.dto.UpiPaymentInfoDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.serviceImple.UpiPaymentInfoServiceImpl;
import com.ytbooster.serviceImple.UserServiceImpl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/upi-payments")
@PreAuthorize("hasRole('ADMIN')")
public class UpiPaymentInfoController {

    @Autowired
    private UpiPaymentInfoServiceImpl upiPaymentInfoService;
    
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private Validator validator;

    /**
     * 🔧 Main admin payments page with form and list
     */
    @GetMapping
    public String paymentsPage(Model model, Authentication authentication) {
        try {
            UserDTO admin = userService.findByEmail(authentication.getName());
            
            // Create form object with adminId pre-populated
            UpiPaymentInfoDTO upiForm = new UpiPaymentInfoDTO();
            upiForm.setAdminId(admin.getId());
            upiForm.setIsActive(true); // Default to active
            model.addAttribute("upiForm", upiForm);
            
            // Load existing UPI settings for this admin
            List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
            model.addAttribute("upiList", upiList);
            
            // Add statistics
            model.addAttribute("totalUpiSettings", upiList.size());
            long activeCount = upiList.stream()
                    .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                    .count();
            model.addAttribute("activeUpiCount", activeCount);
            
            // 🔧 CRITICAL: Always set editMode
            model.addAttribute("editMode", false); // Default to add mode
            
            return "admintabs/payment-methods";
            
        } catch (Exception e) {
            log.error("Error loading payments page", e);
            model.addAttribute("errorMessage", "Error loading payment settings: " + e.getMessage());
            model.addAttribute("upiForm", new UpiPaymentInfoDTO());
            model.addAttribute("upiList", List.of());
            model.addAttribute("totalUpiSettings", 0);
            model.addAttribute("activeUpiCount", 0L);
            model.addAttribute("editMode", false); // Set even in error case
            return "admintabs/payment-methods";
        }
    }

    /**
     * 🔧 Save UPI settings with enhanced validation and file handling
     */
    @PostMapping("/save")
    public String saveUpiSettings(
            @ModelAttribute("upiForm") UpiPaymentInfoDTO upiForm,
            @RequestParam(value = "qrImageFile", required = false) MultipartFile qrImageFile,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            UserDTO admin = userService.findByEmail(authentication.getName());
            upiForm.setAdminId(admin.getId());

            // Set default values if not provided
            if (upiForm.getIsActive() == null) {
                upiForm.setIsActive(true);
            }

            // Manual validation after setting adminId
            Set<ConstraintViolation<UpiPaymentInfoDTO>> violations = validator.validate(upiForm);
            
            if (!violations.isEmpty()) {
                log.warn("Validation errors: {}", violations);
                
                // Add validation errors to model
                for (ConstraintViolation<UpiPaymentInfoDTO> violation : violations) {
                    model.addAttribute("fieldError_" + violation.getPropertyPath().toString(), 
                                     violation.getMessage());
                }
                
                // Reload data and return to form
                List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
                model.addAttribute("upiList", upiList);
                model.addAttribute("totalUpiSettings", upiList.size());
                long activeCount = upiList.stream()
                        .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                        .count();
                model.addAttribute("activeUpiCount", activeCount);
                model.addAttribute("editMode", false);
                model.addAttribute("errorMessage", "Please fix the validation errors");
                return "admintabs/payment-methods";
            }

            // Enhanced file validation and processing
            try {
                validateAndProcessQrFile(qrImageFile, upiForm);
            } catch (IllegalArgumentException e) {
                List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
                model.addAttribute("upiList", upiList);
                model.addAttribute("totalUpiSettings", upiList.size());
                long activeCount = upiList.stream()
                        .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                        .count();
                model.addAttribute("activeUpiCount", activeCount);
                model.addAttribute("editMode", false);
                model.addAttribute("errorMessage", e.getMessage());
                return "admintabs/payment-methods";
            }

            // Save UPI settings
            UpiPaymentInfoDTO saved = upiPaymentInfoService.create(upiForm);
            log.info("Successfully created UPI payment method with ID: {}", saved.getId());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ UPI payment settings saved successfully!");
            
            return "redirect:/admin/upi-payments";

        } catch (Exception e) {
            log.error("Error saving UPI settings", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error saving settings: " + e.getMessage());
            return "redirect:/admin/upi-payments";
        }
    }

    /**
     * 🔧 Edit form for UPI settings
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            UserDTO admin = userService.findByEmail(authentication.getName());
            UpiPaymentInfoDTO upiInfo = upiPaymentInfoService.findById(id);
            
            // Security check: ensure admin can only edit their own UPI settings
            if (!upiInfo.getAdminId().equals(admin.getId())) {
                log.warn("Admin {} attempted to edit UPI settings {} belonging to admin {}", 
                         admin.getId(), id, upiInfo.getAdminId());
                throw new SecurityException("Access denied");
            }
            
            model.addAttribute("upiForm", upiInfo);
            model.addAttribute("editMode", true); // Set to true for edit mode
            
            // Load existing UPI settings for display
            List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
            model.addAttribute("upiList", upiList);
            model.addAttribute("totalUpiSettings", upiList.size());
            long activeCount = upiList.stream()
                    .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                    .count();
            model.addAttribute("activeUpiCount", activeCount);
            
            return "admintabs/payment-methods";
            
        } catch (Exception e) {
            log.error("Error loading UPI settings for edit", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error loading settings for edit: " + e.getMessage());
            return "redirect:/admin/upi-payments";
        }
    }

    /**
     * 🔧 Update UPI settings with enhanced validation
     */
    @PostMapping("/{id}/update")
    public String updateUpiSettings(
            @PathVariable Long id,
            @ModelAttribute("upiForm") UpiPaymentInfoDTO upiForm,
            @RequestParam(value = "qrImageFile", required = false) MultipartFile qrImageFile,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            UserDTO admin = userService.findByEmail(authentication.getName());
            
            // Security check
            UpiPaymentInfoDTO existing = upiPaymentInfoService.findById(id);
            if (!existing.getAdminId().equals(admin.getId())) {
                log.warn("Admin {} attempted to update UPI settings {} belonging to admin {}", 
                         admin.getId(), id, existing.getAdminId());
                throw new SecurityException("Access denied");
            }

            // Set required fields
            upiForm.setId(id);
            upiForm.setAdminId(admin.getId());
            
            // Preserve existing values if not provided
            if (upiForm.getIsActive() == null) {
                upiForm.setIsActive(existing.getIsActive());
            }

            // Manual validation
            Set<ConstraintViolation<UpiPaymentInfoDTO>> violations = validator.validate(upiForm);
            
            if (!violations.isEmpty()) {
                log.warn("Validation errors during update: {}", violations);
                
                for (ConstraintViolation<UpiPaymentInfoDTO> violation : violations) {
                    model.addAttribute("fieldError_" + violation.getPropertyPath().toString(), 
                                     violation.getMessage());
                }
                
                // Reload data and return to form
                List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
                model.addAttribute("upiList", upiList);
                model.addAttribute("totalUpiSettings", upiList.size());
                long activeCount = upiList.stream()
                        .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                        .count();
                model.addAttribute("activeUpiCount", activeCount);
                model.addAttribute("errorMessage", "Please fix the validation errors");
                model.addAttribute("editMode", true);
                return "admintabs/payment-methods";
            }

            // Handle QR code file upload
            try {
                if (qrImageFile != null && !qrImageFile.isEmpty()) {
                    validateAndProcessQrFile(qrImageFile, upiForm);
                } else {
                    // Keep existing QR code if no new file uploaded
                    upiForm.setQrCode(existing.getQrCode());
                }
            } catch (IllegalArgumentException e) {
                List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
                model.addAttribute("upiList", upiList);
                model.addAttribute("totalUpiSettings", upiList.size());
                long activeCount = upiList.stream()
                        .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                        .count();
                model.addAttribute("activeUpiCount", activeCount);
                model.addAttribute("errorMessage", e.getMessage());
                model.addAttribute("editMode", true);
                return "admintabs/payment-methods";
            }

            // Update UPI settings
            UpiPaymentInfoDTO updated = upiPaymentInfoService.update(id, upiForm);
            log.info("Successfully updated UPI payment method with ID: {}", updated.getId());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ UPI payment settings updated successfully!");
            
            return "redirect:/admin/upi-payments";

        } catch (Exception e) {
            log.error("Error updating UPI settings", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating settings: " + e.getMessage());
            return "redirect:/admin/upi-payments";
        }
    }

    /**
     * 🔧 Delete UPI settings
     */
    @PostMapping("/{id}/delete")
    public String deleteUpiSettings(@PathVariable Long id, 
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            UserDTO admin = userService.findByEmail(authentication.getName());
            
            // Security check
            UpiPaymentInfoDTO existing = upiPaymentInfoService.findById(id);
            if (!existing.getAdminId().equals(admin.getId())) {
                log.warn("Admin {} attempted to delete UPI settings {} belonging to admin {}", 
                         admin.getId(), id, existing.getAdminId());
                throw new SecurityException("Access denied");
            }

            // Log before deletion for audit
            log.info("Deleting UPI payment method: ID={}, UPI={}, Admin={}", 
                     id, existing.getUpiId(), admin.getId());

            upiPaymentInfoService.delete(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ UPI payment settings deleted successfully!");

        } catch (Exception e) {
            log.error("Error deleting UPI settings with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error deleting settings: " + e.getMessage());
        }
        
        return "redirect:/admin/upi-payments";
    }

    /**
     * 🔧 Enhanced file validation and processing with comprehensive checks
     */
    private void validateAndProcessQrFile(MultipartFile qrImageFile, UpiPaymentInfoDTO upiForm) throws IOException {
        if (qrImageFile == null || qrImageFile.isEmpty()) {
            return; // No file uploaded
        }

        // Validate file type
        String contentType = qrImageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Please upload a valid image file (PNG, JPG, JPEG, GIF)");
        }

        // Validate specific image types
        String[] allowedTypes = {"image/png", "image/jpeg", "image/jpg", "image/gif"};
        boolean validType = false;
        for (String type : allowedTypes) {
            if (type.equalsIgnoreCase(contentType)) {
                validType = true;
                break;
            }
        }
        
        if (!validType) {
            throw new IllegalArgumentException("Only PNG, JPG, JPEG, and GIF files are allowed");
        }

        // Validate file size (max 10MB for LONGBLOB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (qrImageFile.getSize() > maxSize) {
            throw new IllegalArgumentException(
                String.format("File size cannot exceed 10MB. Current size: %.2f MB", 
                             qrImageFile.getSize() / (1024.0 * 1024.0)));
        }

        // Validate minimum file size (prevent empty or corrupt files)
        if (qrImageFile.getSize() < 100) {
            throw new IllegalArgumentException("File appears to be empty or corrupted");
        }

        // Store the image data
        byte[] imageData = qrImageFile.getBytes();
        
        // Basic validation of image data
        if (imageData.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }

        // Log file info for monitoring
        log.info("Processing QR image: {} bytes, type: {}, filename: {}", 
                 imageData.length, contentType, qrImageFile.getOriginalFilename());
        
        upiForm.setQrCode(imageData);
    }
}
