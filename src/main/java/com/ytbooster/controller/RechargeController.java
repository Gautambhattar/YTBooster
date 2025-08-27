package com.ytbooster.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ytbooster.model.dto.RechargeDTO;
import com.ytbooster.model.dto.RechargeHistoryDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.service.RechargeHistoryService;
import com.ytbooster.serviceImple.UserServiceImpl;

@Controller
@RequestMapping("/recharge")
public class RechargeController {

    @Autowired
    private RechargeHistoryService rechargeService;

    @Autowired
    private UserServiceImpl userService;

    @ModelAttribute("recharge")
    public RechargeDTO getRechargeDTO() {
        return new RechargeDTO(); // default object for form binding
    }

    // 🔧 NEW: Add user profile attributes for profile section display
    @ModelAttribute
    public void addUserProfileAttributes(Model model, Principal principal) {
        if (principal != null) {
            try {
                String username = principal.getName();
                UserDTO userDTO = userService.findByEmail(username);
                
                if (userDTO != null) {
                    // Profile attributes for display - using getName() only
                    model.addAttribute("username", userDTO.getName() != null ? userDTO.getName() : username);
                    model.addAttribute("userEmail", username);
                    model.addAttribute("wallet", userDTO.getWallet() != null ? userDTO.getWallet().doubleValue() : 0.0);
                    model.addAttribute("userRole", userDTO.getRole() != null ? userDTO.getRole() : "USER");
                    model.addAttribute("userId", userDTO.getId());
                }
            } catch (Exception e) {
                // Fallback values if user loading fails
                model.addAttribute("username", principal.getName());
                model.addAttribute("wallet", 0.0);
                model.addAttribute("userRole", "USER");
            }
        }
    }

    // FIXED: Added GET mapping to handle direct access
    @GetMapping("/showRechargeForm")
    public String showRechargeForm(Model model, Authentication authentication) {
        
        System.out.println("GET REQUEST - showRechargeForm");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        UserDTO user = userService.findByEmail(email);

        if (user == null) {
            model.addAttribute("message", "User not found!");
            model.addAttribute("messageType", "error");
            return "dashboard";
        }

        // Pass common user info
        model.addAttribute("username", user.getName() != null ? user.getName() : email);
        model.addAttribute("wallet", user.getWallet());
        model.addAttribute("tab", "recharge");
        model.addAttribute("recharge", new RechargeDTO());

        rechargeService.loadUpiInfo(model);
        return "dashboard";
    }

    @PostMapping("/showRechargeForm")
    public String processRecharge(@ModelAttribute("recharge") RechargeDTO dto,
                                  @RequestParam(required = false) String action,
                                  Model model,
                                  Authentication authentication) {

        System.out.println("POST REQUEST - Action received: " + action);
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        UserDTO user = userService.findByEmail(email);
        
        if (user == null) {
            model.addAttribute("message", "User not found!");
            model.addAttribute("messageType", "error");
            return "dashboard";
        }

        dto.setEmail(email);
        dto.setUserId(user.getId());

        // Always pass common user info
        model.addAttribute("username", user.getName() != null ? user.getName() : email);
        model.addAttribute("wallet", user.getWallet());
        model.addAttribute("tab", "recharge");

        // Handle null or empty action
        if (action == null || action.trim().isEmpty()) {
            action = "show";
            System.out.println("Action was null/empty, defaulting to 'show'");
        }

        System.out.println("Processing action: " + action);

        if ("show".equals(action)) {
            model.addAttribute("recharge", new RechargeDTO());
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }

        if ("start".equals(action)) {
            return handleStartAction(dto, model);
        }

        if ("confirm".equals(action)) {
            return handleConfirmAction(dto, model);
        }

        // Default fallback
        System.out.println("Unknown action: " + action + ", defaulting to show");
        model.addAttribute("recharge", new RechargeDTO());
        rechargeService.loadUpiInfo(model);
        return "dashboard";
    }

    private String handleStartAction(RechargeDTO dto, Model model) {
        System.out.println("=== HANDLING START ACTION ===");
        System.out.println("Amount: " + dto.getAmount());
        System.out.println("Mobile: " + dto.getMobileNumber());
        System.out.println("Email: " + dto.getEmail());
        
        // Validate amount
        if (dto.getAmount() == null) {
            System.out.println("Amount is null!");
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Please enter an amount!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        // Convert to double for easier comparison
        double amountValue;
        try {
            amountValue = dto.getAmount().doubleValue();
        } catch (Exception e) {
            System.out.println("Error converting amount: " + e.getMessage());
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Invalid amount format!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        if (amountValue <= 0) {
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Please enter a valid amount!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        if (amountValue < 1) {
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Minimum recharge amount is ₹1");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        if (amountValue > 50000) {
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Maximum recharge amount is ₹50,000");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        // Validate mobile number (long type)
        if (dto.getMobileNumber() == null || dto.getMobileNumber() <= 0) {
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Mobile number is required!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        // Validate mobile number format and length
        String mobileString = String.valueOf(dto.getMobileNumber());
        if (mobileString.length() != 10) {
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Mobile number must be exactly 10 digits!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        // FIXED: Corrected regex pattern
        if (!mobileString.matches("^[6-9]\\d{9}$")) {
            model.addAttribute("recharge", dto);
            model.addAttribute("message", "Please enter a valid Indian mobile number starting with 6-9");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }
        
        // Set default values
        dto.setPaymentMethod("UPI");
        dto.setStatus("PENDING");
        
        // All validation passed - show payment step
        System.out.println("All validations passed - proceeding to payment step");
        System.out.println("Setting showPaymentStep to true");
        System.out.println("Final DTO data - Amount: " + dto.getAmount() + ", Mobile: " + dto.getMobileNumber());
        
        model.addAttribute("recharge", dto);
        model.addAttribute("showPaymentStep", true);
        rechargeService.loadUpiInfo(model);
        
        return "dashboard";
    }

    private String handleConfirmAction(RechargeDTO dto, Model model) {
        System.out.println("=== HANDLING CONFIRM ACTION ===");
        System.out.println("Amount: " + dto.getAmount());
        System.out.println("UTR: " + dto.getUtr());
        System.out.println("Email: " + dto.getEmail());
        System.out.println("Mobile: " + dto.getMobileNumber());

        // Check if amount is null and restore from session if needed
        if (dto.getAmount() == null) {
            System.out.println("Amount is null in confirm step!");
            model.addAttribute("recharge", dto);
            model.addAttribute("showPaymentStep", true);
            model.addAttribute("message", "Session expired. Please start over.");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }

        // Validate amount (BigDecimal safe)
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("recharge", dto);
            model.addAttribute("showPaymentStep", true);
            model.addAttribute("message", "Invalid recharge amount!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }

        // Validate UTR (long type)
        if (dto.getUtr() == null || dto.getUtr() <= 0) {
            model.addAttribute("recharge", dto);
            model.addAttribute("showPaymentStep", true);
            model.addAttribute("message", "UTR / Transaction ID is required!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }

        // Validate UTR has minimum digits
        String utrString = String.valueOf(dto.getUtr());
        if (utrString.length() < 8) {
            model.addAttribute("recharge", dto);
            model.addAttribute("showPaymentStep", true);
            model.addAttribute("message", "UTR / Transaction ID must be at least 8 digits!");
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
            return "dashboard";
        }

        // Convert DTO to HistoryDTO for saving
        RechargeHistoryDTO historyDto = new RechargeHistoryDTO();
        historyDto.setAmount(dto.getAmount());
        historyDto.setEmail(dto.getEmail());
        historyDto.setMobileNumber(dto.getMobileNumber());
        historyDto.setUtr(dto.getUtr());
        historyDto.setUserId(dto.getUserId());
        historyDto.setPaymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "UPI");
        historyDto.setStatus("PENDING"); // Always set as PENDING for new requests

        // Save recharge
        try {
            rechargeService.save(historyDto);
            System.out.println("Recharge saved successfully");
            model.addAttribute("message", "Recharge request submitted successfully! You will receive confirmation once verified.");
            model.addAttribute("messageType", "success");
            model.addAttribute("recharge", new RechargeDTO()); // reset form
        } catch (Exception e) {
            System.out.println("Error saving recharge: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("recharge", dto);
            model.addAttribute("showPaymentStep", true);
            model.addAttribute("message", "Failed to save recharge. Please try again: " + e.getMessage());
            model.addAttribute("messageType", "error");
            rechargeService.loadUpiInfo(model);
        }

        rechargeService.loadUpiInfo(model);
        return "dashboard";
    }

    // FIXED: Add test endpoint for debugging
    @GetMapping("/test")
    public String testRecharge(Model model, Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        UserDTO user = userService.findByEmail(email);

        model.addAttribute("message", "Recharge controller is working! User: " + email);
        model.addAttribute("messageType", "success");
        model.addAttribute("tab", "recharge");
        model.addAttribute("username", user.getName() != null ? user.getName() : email);
        model.addAttribute("wallet", user.getWallet());
        model.addAttribute("recharge", new RechargeDTO());
        
        rechargeService.loadUpiInfo(model);
        return "dashboard";
    }
}
