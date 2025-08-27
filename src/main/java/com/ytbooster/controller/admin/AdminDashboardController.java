package com.ytbooster.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ytbooster.model.dto.OrdersDTO;
import com.ytbooster.model.dto.RechargeDTO;
import com.ytbooster.model.dto.RechargeHistoryDTO;
import com.ytbooster.model.dto.UpiPaymentInfoDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.service.OrdersService;
import com.ytbooster.service.RechargeHistoryService;
import com.ytbooster.serviceImple.UpiPaymentInfoServiceImpl;
import com.ytbooster.serviceImple.UserServiceImpl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * 🚀 Complete Admin Dashboard Controller with Integrated User Management
 * 
 * Features:
 * - Global user attributes loading
 * - Admin referral code display  
 * - Orders management
 * - Recharge history management
 * - UPI payment methods management
 * - COMPLETE USER MANAGEMENT
 * - Wallet balance tracking
 * - File upload handling
 * - Comprehensive error handling
 * - AJAX API endpoints for user operations
 */
@Slf4j
@Controller
public class AdminDashboardController {

    @Autowired
    private OrdersService ordersService;
    
    @Autowired
    private RechargeHistoryService rechargeService;
    
    @Autowired
    private UpiPaymentInfoServiceImpl upiPaymentInfoService;
    
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private Validator validator;

    /**
     * 🔧 GLOBAL MODEL ATTRIBUTES: Always add admin information to all views
     * This method is called before every handler method in this controller
     * 
     * ✅ Includes admin referral code from user details
     */
    @ModelAttribute
    public void addCommonUserAttributes(Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                String userEmail = authentication.getName();
                UserDTO admin = userService.findByEmail(userEmail);
                
                if (admin != null) {
                    // 📊 Basic Admin Information
                    model.addAttribute("username", admin.getEmail());
                    model.addAttribute("adminId", admin.getId());
                    model.addAttribute("adminFullName", admin.getName() != null ? admin.getName() : admin.getEmail());
                    model.addAttribute("isAdmin", true);
                    model.addAttribute("userRole", "ADMIN");
                    model.addAttribute("walletBalance", admin.getWallet() != null ? admin.getWallet() : 0.0);
                    
                    // 🔧 Admin Referral Code Support
                    model.addAttribute("adminReferralCode", admin.getReferCode());
                    
                    // 📧 Contact Information
                    model.addAttribute("adminEmail", admin.getEmail());
                   
                    // 🎯 Full Admin Object for Complex Operations
                    model.addAttribute("currentAdmin", admin);
                    
                    log.debug("Loaded admin user attributes: {}", admin.getEmail());
                    
                } else {
                    // ⚠️ Authenticated but user not found in database
                    log.warn("Authenticated user not found in database: {}", userEmail);
                    setFallbackAttributes(model, userEmail);
                }
            } else {
                // 🚫 Not authenticated
                log.warn("No authentication found");
                setGuestAttributes(model);
            }
        } catch (Exception e) {
            log.error("Error loading common user attributes", e);
            setErrorAttributes(model, e.getMessage());
        }
    }

    /**
     * 🔧 Set fallback attributes for authenticated user not found in DB
     */
    private void setFallbackAttributes(Model model, String email) {
        model.addAttribute("username", email);
        model.addAttribute("adminId", null);
        model.addAttribute("adminFullName", "Unknown Admin");
        model.addAttribute("isAdmin", false);
        model.addAttribute("userRole", "UNKNOWN");
        model.addAttribute("walletBalance", 0.0);
        model.addAttribute("adminReferralCode", null);
        model.addAttribute("adminEmail", email);
        model.addAttribute("currentAdmin", null);
    }

    /**
     * 🔧 Set guest attributes for unauthenticated users
     */
    private void setGuestAttributes(Model model) {
        model.addAttribute("username", "Guest");
        model.addAttribute("adminId", null);
        model.addAttribute("adminFullName", "Guest User");
        model.addAttribute("isAdmin", false);
        model.addAttribute("userRole", "GUEST");
        model.addAttribute("walletBalance", 0.0);
        model.addAttribute("adminReferralCode", null);
        model.addAttribute("adminEmail", null);
        model.addAttribute("currentAdmin", null);
    }

    /**
     * 🔧 Set error attributes when loading fails
     */
    private void setErrorAttributes(Model model, String errorMessage) {
        model.addAttribute("username", "Error Loading User");
        model.addAttribute("adminId", null);
        model.addAttribute("adminFullName", "Error Loading User");
        model.addAttribute("isAdmin", false);
        model.addAttribute("userRole", "ERROR");
        model.addAttribute("walletBalance", 0.0);
        model.addAttribute("adminReferralCode", null);
        model.addAttribute("adminEmail", null);
        model.addAttribute("currentAdmin", null);
        model.addAttribute("systemError", errorMessage);
    }

    /**
     * 🔧 Form Model Attributes - Always available for forms
     */
    @ModelAttribute("rechargeDTO")
    public RechargeDTO getRechargeDTO() {
        return new RechargeDTO();
    }

    @ModelAttribute("ordersDTO")
    public OrdersDTO getOrderDTO() {
        return new OrdersDTO();
    }

    @ModelAttribute("upiForm")
    public UpiPaymentInfoDTO getUpiForm() {
        return new UpiPaymentInfoDTO();
    }

    /**
     * 🚀 Main Admin Dashboard - Enhanced with Complete Tab Support INCLUDING USERS
     */
    @GetMapping("/admindashboard")
    public String showAdminPanel(
            @RequestParam(value = "tab", defaultValue = "orders") String tab,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "searchBy", required = false) String searchBy,
            @RequestParam(name = "utr", required = false) String utr,
            @RequestParam(name = "userId", required = false) String userId,
            // User management parameters
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String userStatus,
            Model model,
            Authentication authentication) {

        try {
            // 📊 Set tab and filter information
            model.addAttribute("tab", tab.toLowerCase());
            model.addAttribute("currentStatus", status.toUpperCase());
            model.addAttribute("statusFilter", status.toUpperCase());
            
            // 🔍 Search parameters
            model.addAttribute("searchBy", searchBy);
            model.addAttribute("searchUtr", utr);
            model.addAttribute("searchUserId", userId);
            
            // 👤 Get current admin (already loaded by @ModelAttribute)
            UserDTO admin = (UserDTO) model.getAttribute("currentAdmin");

            // 🎯 Load data based on selected tab
            switch (tab.toLowerCase()) {
                case "orders":
                    loadOrdersData(model, status);
                    break;

                case "recharges":
                    loadRechargesData(model, status, searchBy, utr, userId);
                    break;

                case "payment-methods":
                    loadPaymentMethodsData(model, admin);
                    break;

                case "wallet":
                    loadWalletData(model, admin);
                    break;

                case "users":
                    // 🆕 Fully implemented user management
                    loadUsersData(model, page, size, search, role, userStatus);
                    break;

                case "analytics":
                    loadAnalyticsData(model, admin);
                    break;

                default:
                    // 🏠 Default to orders tab
                    log.info("Unknown tab '{}', defaulting to orders", tab);
                    model.addAttribute("tab", "orders");
                    loadOrdersData(model, "ALL");
                    break;
            }

            // 📈 Load dashboard statistics
            loadDashboardStatistics(model, admin);

            return "admindashboard";
            
        } catch (Exception e) {
            log.error("Error loading admin dashboard for tab: {}", tab, e);
            model.addAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            model.addAttribute("tab", "orders");
            
            // Load minimal orders data as fallback
            try {
                loadOrdersData(model, "ALL");
            } catch (Exception fallbackError) {
                log.error("Fallback data loading also failed", fallbackError);
                model.addAttribute("orders", List.of());
            }
            
            return "admindashboard";
        }
    }

    // ======== USER MANAGEMENT ENDPOINTS (COMPLETE IMPLEMENTATION) ========

    /**
     * 🆕 View individual user details
     */
    @GetMapping("/admindashboard/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        try {
            Optional<UserDTO> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                return "redirect:/admindashboard?tab=users&error=User not found";
            }
            model.addAttribute("user", userOpt.get());
            model.addAttribute("tab", "users");
            return "admindashboard";
        } catch (Exception e) {
            log.error("Error viewing user: {}", id, e);
            return "redirect:/admindashboard?tab=users&error=Error loading user details";
        }
    }

    /**
     * 🆕 Update user status via AJAX
     */
    @PostMapping("/admindashboard/users/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateUserStatus(@PathVariable Long id,
                                                               @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status cannot be empty"));
            }
            
            boolean updated = userService.updateUserStatus(id, status.trim().toUpperCase());

            if (updated) {
                log.info("User {} status updated to: {}", id, status);
                return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to update status"));
            }
        } catch (Exception e) {
            log.error("Error updating user status for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Update user authentication status via AJAX
     */
    @PostMapping("/admindashboard/users/{id}/authentication")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateAuthentication(@PathVariable Long id,
                                                                   @RequestBody Map<String, Boolean> request) {
        try {
            Boolean authenticated = request.get("authenticated");
            
            if (authenticated == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication status is required"));
            }
            
            boolean updated = userService.updateAuthenticationStatus(id, authenticated);

            if (updated) {
                log.info("User {} authentication status updated to: {}", id, authenticated);
                return ResponseEntity.ok(Map.of("message", "Authentication status updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to update authentication status"));
            }
        } catch (Exception e) {
            log.error("Error updating authentication status for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Update user online status via AJAX
     */
    @PostMapping("/admindashboard/users/{id}/online")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateOnlineStatus(@PathVariable Long id,
                                                                 @RequestBody Map<String, Boolean> request) {
        try {
            Boolean online = request.get("online");
            
            if (online == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Online status is required"));
            }
            
            boolean updated = userService.updateOnlineStatus(id, online);

            if (updated) {
                log.info("User {} online status updated to: {}", id, online);
                return ResponseEntity.ok(Map.of("message", "Online status updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to update online status"));
            }
        } catch (Exception e) {
            log.error("Error updating online status for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Add money to user wallet via AJAX
     */
    @PostMapping("/admindashboard/users/{id}/wallet/add")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addToWallet(@PathVariable Long id,
                                                          @RequestBody Map<String, BigDecimal> request) {
        try {
            BigDecimal amount = request.get("amount");
            
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount. Amount must be greater than 0"));
            }
            
            // Validate reasonable amount limits
            if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount too large. Maximum allowed is ₹10,00,000"));
            }

            boolean updated = userService.addToWallet(id, amount);
            
            if (updated) {
                log.info("Added ₹{} to user {} wallet", amount, id);
                return ResponseEntity.ok(Map.of("message", "₹" + amount + " added to wallet successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to add amount to wallet"));
            }
        } catch (Exception e) {
            log.error("Error adding to wallet for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Deduct money from user wallet via AJAX
     */
    @PostMapping("/admindashboard/users/{id}/wallet/deduct")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deductFromWallet(@PathVariable Long id,
                                                               @RequestBody Map<String, BigDecimal> request) {
        try {
            BigDecimal amount = request.get("amount");
            
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount. Amount must be greater than 0"));
            }

            boolean updated = userService.deductFromWallet(id, amount);
            
            if (updated) {
                log.info("Deducted ₹{} from user {} wallet", amount, id);
                return ResponseEntity.ok(Map.of("message", "₹" + amount + " deducted from wallet successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to deduct amount. Check if user has sufficient balance"));
            }
        } catch (IllegalStateException e) {
            // Handle insufficient balance specifically
            log.warn("Insufficient balance for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient wallet balance"));
        } catch (Exception e) {
            log.error("Error deducting from wallet for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Delete user via AJAX
     */
    @DeleteMapping("/admindashboard/users/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        try {
            // Check if user exists first
            Optional<UserDTO> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            
            UserDTO user = userOpt.get();
            
            // Prevent deletion of admin users (safety check)
            if (user.isAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete admin users"));
            }

            boolean deleted = userService.deleteUser(id);

            if (deleted) {
                log.info("User {} ({}) deleted successfully", id, user.getEmail());
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete user"));
            }
        } catch (Exception e) {
            log.error("Error deleting user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    // ======== EXISTING METHODS (UPI PAYMENT HANDLING) ========

    /**
     * 🔧 Enhanced UPI Form Submission Handler
     */
    @PostMapping("/admindashboard/save-upi")
    public String saveUpiFromDashboard(
            @ModelAttribute("upiForm") UpiPaymentInfoDTO upiForm,
            @RequestParam(value = "qrImageFile", required = false) MultipartFile qrImageFile,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // 👤 Get current admin
            Long adminId = (Long) model.getAttribute("adminId");
            if (adminId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "❌ Admin session not found");
                return "redirect:/admindashboard?tab=payment-methods";
            }
            
            // 🔧 Set admin ID and defaults
            upiForm.setAdminId(adminId);
            if (upiForm.getIsActive() == null) {
                upiForm.setIsActive(true);
            }

            // ✅ Validate form data
            Set<ConstraintViolation<UpiPaymentInfoDTO>> violations = validator.validate(upiForm);
            
            if (!violations.isEmpty()) {
                log.warn("UPI form validation errors: {}", violations.size());
                
                // 📝 Add validation errors to redirect attributes
                StringBuilder errorMessage = new StringBuilder("Validation errors: ");
                for (ConstraintViolation<UpiPaymentInfoDTO> violation : violations) {
                    errorMessage.append(violation.getPropertyPath())
                               .append(" - ")
                               .append(violation.getMessage())
                               .append("; ");
                }
                
                redirectAttributes.addFlashAttribute("errorMessage", errorMessage.toString());
                redirectAttributes.addFlashAttribute("upiForm", upiForm);
                return "redirect:/admindashboard?tab=payment-methods";
            }

            // 📸 Handle QR code file upload
            try {
                validateAndProcessQrFile(qrImageFile, upiForm);
            } catch (IllegalArgumentException e) {
                log.warn("QR file validation failed: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
                redirectAttributes.addFlashAttribute("upiForm", upiForm);
                return "redirect:/admindashboard?tab=payment-methods";
            }

            // 💾 Save UPI settings
            UpiPaymentInfoDTO savedUpi = upiPaymentInfoService.create(upiForm);
            
            log.info("UPI settings saved successfully for admin: {}, UPI ID: {}", 
                     adminId, savedUpi.getUpiId());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ UPI payment settings saved successfully! UPI ID: " + savedUpi.getUpiId());

            return "redirect:/admindashboard?tab=payment-methods";

        } catch (Exception e) {
            log.error("Error saving UPI settings", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error saving settings: " + e.getMessage());
            return "redirect:/admindashboard?tab=payment-methods";
        }
    }

    /**
     * 🔧 Enhanced QR File Validation and Processing
     */
    private void validateAndProcessQrFile(MultipartFile qrImageFile, UpiPaymentInfoDTO upiForm) throws Exception {
        if (qrImageFile == null || qrImageFile.isEmpty()) {
            log.debug("No QR image file uploaded");
            return; // No file uploaded - this is optional
        }

        // 📝 Validate file type
        String contentType = qrImageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Please upload a valid image file (PNG, JPG, JPEG, GIF, WebP)");
        }

        // 📏 Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (qrImageFile.getSize() > maxSize) {
            throw new IllegalArgumentException(
                String.format("File size cannot exceed 10MB. Current size: %.2f MB", 
                             qrImageFile.getSize() / (1024.0 * 1024.0)));
        }

        // 📦 Validate minimum file size (avoid empty files)
        if (qrImageFile.getSize() < 100) {
            throw new IllegalArgumentException("File appears to be empty or corrupted");
        }

        // 📸 Process and store image data
        byte[] imageData = qrImageFile.getBytes();
        
        if (imageData.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }

        log.info("Processing QR image: {} bytes, type: {}, filename: {}", 
                 imageData.length, contentType, qrImageFile.getOriginalFilename());
        
        upiForm.setQrCode(imageData);
    }

    // ======== DATA LOADING METHODS ========

    /**
     * 🔧 Load Orders Data with Status Filtering
     */
    private void loadOrdersData(Model model, String status) {
        try {
            List<OrdersDTO> orders;
            
            if ("ALL".equalsIgnoreCase(status)) {
                orders = ordersService.getAllOrders();
            } else {
                orders = ordersService.getOrdersByStatus(status);
            }
            
            model.addAttribute("orders", orders);
            model.addAttribute("totalOrders", orders.size());
            
            // 📊 Calculate order statistics
            long pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
            long completedOrders = orders.stream().filter(o -> "COMPLETE".equals(o.getStatus())).count();
            long processingOrders = orders.stream().filter(o -> "PROCESSING".equals(o.getStatus())).count();
            
            model.addAttribute("pendingOrdersCount", pendingOrders);
            model.addAttribute("completedOrdersCount", completedOrders);
            model.addAttribute("processingOrdersCount", processingOrders);
            
            // 📈 Status options for filtering
            model.addAttribute("statusOptions", List.of("ALL", "PENDING", "PROCESSING", "COMPLETE", "CANCELLED"));
            
            log.debug("Loaded {} orders with status filter: {}", orders.size(), status);
            
        } catch (Exception e) {
            log.error("Error loading orders data", e);
            model.addAttribute("orders", List.of());
            model.addAttribute("totalOrders", 0);
            model.addAttribute("pendingOrdersCount", 0L);
            model.addAttribute("completedOrdersCount", 0L);
            model.addAttribute("processingOrdersCount", 0L);
            model.addAttribute("statusOptions", List.of("ALL", "PENDING", "PROCESSING", "COMPLETE", "CANCELLED"));
        }
    }

    /**
     * 🔧 Load Recharges Data with Enhanced Filtering
     */
    private void loadRechargesData(Model model, String status, String searchBy, String utr, String userId) {
        try {
            List<RechargeHistoryDTO> recharges;
            
            // 🔍 Apply search and status filters
            if ("utr".equals(searchBy) && utr != null && !utr.trim().isEmpty()) {
                RechargeHistoryDTO recharge = rechargeService.findByUtr(Long.parseLong(utr.trim()));
                recharges = recharge != null ? List.of(recharge) : List.of();
            } else if ("user".equals(searchBy) && userId != null && !userId.trim().isEmpty()) {
                recharges = rechargeService.findByUserId(Long.parseLong(userId.trim()));
            } else if ("ALL".equalsIgnoreCase(status)) {
                recharges = rechargeService.findAll();
            } else {
                recharges = rechargeService.findByStatus(status);
            }
            
            model.addAttribute("recharges", recharges);
            model.addAttribute("totalRecharges", recharges.size());
            
            // 💰 Calculate financial statistics
            double totalAmount = recharges.stream()
                    .filter(r -> "COMPLETE".equals(r.getStatus()))
                    .mapToDouble(r -> r.getAmount() != null ? r.getAmount().doubleValue() : 0.0)
                    .sum();
            
            double pendingAmount = recharges.stream()
                    .filter(r -> "PENDING".equals(r.getStatus()))
                    .mapToDouble(r -> r.getAmount() != null ? r.getAmount().doubleValue() : 0.0)
                    .sum();
                    
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("pendingAmount", pendingAmount);
            
            // 📊 Status counts
            long pendingCount = recharges.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
            long completeCount = recharges.stream().filter(r -> "COMPLETE".equals(r.getStatus())).count();
            
            model.addAttribute("pendingRechargesCount", pendingCount);
            model.addAttribute("completeRechargesCount", completeCount);
            
            // 📈 Filter options
            model.addAttribute("statusOptions", List.of("ALL", "PENDING", "COMPLETE", "FAILED"));
            
            log.debug("Loaded {} recharges with filters - status: {}, searchBy: {}", 
                     recharges.size(), status, searchBy);
            
        } catch (NumberFormatException e) {
            log.warn("Invalid number format in search parameters - utr: {}, userId: {}", utr, userId);
            model.addAttribute("recharges", List.of());
            model.addAttribute("totalRecharges", 0);
            model.addAttribute("totalAmount", 0.0);
            model.addAttribute("pendingAmount", 0.0);
            model.addAttribute("pendingRechargesCount", 0L);
            model.addAttribute("completeRechargesCount", 0L);
            model.addAttribute("statusOptions", List.of("ALL", "PENDING", "COMPLETE", "FAILED"));
            model.addAttribute("errorMessage", "Invalid search parameters. Please enter valid numbers.");
        } catch (Exception e) {
            log.error("Error loading recharges data", e);
            model.addAttribute("recharges", List.of());
            model.addAttribute("totalRecharges", 0);
            model.addAttribute("totalAmount", 0.0);
            model.addAttribute("pendingAmount", 0.0);
            model.addAttribute("pendingRechargesCount", 0L);
            model.addAttribute("completeRechargesCount", 0L);
            model.addAttribute("statusOptions", List.of("ALL", "PENDING", "COMPLETE", "FAILED"));
        }
    }

    /**
     * 🔧 Load Payment Methods Data with Enhanced Statistics
     */
    private void loadPaymentMethodsData(Model model, UserDTO admin) {
        try {
            if (admin != null) {
                List<UpiPaymentInfoDTO> upiList = upiPaymentInfoService.findByAdminId(admin.getId());
                model.addAttribute("upiList", upiList);
                model.addAttribute("totalUpiSettings", upiList.size());
                
                // 📊 Calculate statistics
                long activeCount = upiList.stream()
                        .filter(upi -> upi.getIsActive() != null && upi.getIsActive())
                        .count();
                        
                long withQrCount = upiList.stream()
                        .filter(upi -> upi.getQrCode() != null && upi.getQrCode().length > 0)
                        .count();
                        
                model.addAttribute("activeUpiCount", activeCount);
                model.addAttribute("upiWithQrCount", withQrCount);
                
                // 🔧 Form setup
                if (!model.containsAttribute("upiForm")) {
                    UpiPaymentInfoDTO upiForm = new UpiPaymentInfoDTO();
                    upiForm.setAdminId(admin.getId());
                    upiForm.setIsActive(true);
                    model.addAttribute("upiForm", upiForm);
                }
                
                model.addAttribute("editMode", false);
                
                log.debug("Loaded {} UPI settings for admin: {}", upiList.size(), admin.getId());
                
            } else {
                // 🚫 No admin found
                setEmptyPaymentMethodsData(model);
            }
        } catch (Exception e) {
            log.error("Error loading payment methods data", e);
            setEmptyPaymentMethodsData(model);
        }
    }

    /**
     * 🔧 Set empty payment methods data
     */
    private void setEmptyPaymentMethodsData(Model model) {
        model.addAttribute("upiList", List.of());
        model.addAttribute("totalUpiSettings", 0);
        model.addAttribute("activeUpiCount", 0L);
        model.addAttribute("upiWithQrCount", 0L);
        model.addAttribute("editMode", false);
        model.addAttribute("upiForm", new UpiPaymentInfoDTO());
    }

    /**
     * 🔧 Load Wallet Data
     */
    private void loadWalletData(Model model, UserDTO admin) {
        try {
            if (admin != null) {
                // Wallet data already loaded in addCommonUserAttributes()
                model.addAttribute("adminWalletBalance", admin.getWallet() != null ? admin.getWallet() : 0.0);
                model.addAttribute("walletCurrency", "INR"); // Indian Rupee
                
                log.debug("Loaded wallet data for admin: {}", admin.getId());
            } else {
                model.addAttribute("adminWalletBalance", 0.0);
                model.addAttribute("walletCurrency", "INR");
            }
        } catch (Exception e) {
            log.error("Error loading wallet data", e);
            model.addAttribute("adminWalletBalance", 0.0);
            model.addAttribute("walletCurrency", "INR");
        }
    }

    /**
     * 🆕 FULLY IMPLEMENTED: Load Users Data with Pagination and Filtering
     */
    private void loadUsersData(Model model, int page, int size, String search, String role, String userStatus) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<UserDTO> userPage;

            // 🔍 Apply search and filters
            if (search != null && !search.trim().isEmpty()) {
                userPage = userService.searchUsers(search.trim(), pageable);
            } else if (role != null && !role.isEmpty() && !"ALL".equals(role)) {
                userPage = userService.getUsersByRole(role, pageable);
            } else if (userStatus != null && !userStatus.isEmpty() && !"ALL".equals(userStatus)) {
                userPage = userService.getUsersByStatus(userStatus, pageable);
            } else {
                userPage = userService.getAllUsers(pageable);
            }

            // 📊 Add user data to model
            model.addAttribute("users", userPage.getContent()); 
            model.addAttribute("totalUsers", userService.getTotalUserCount());
            model.addAttribute("onlineUsers", userService.getOnlineUserCount());
            model.addAttribute("authenticatedUsers", userService.getAuthenticatedUserCount());
            model.addAttribute("guestUsers", userService.getGuestUserCount());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("search", search);
            model.addAttribute("role", role);
            model.addAttribute("userStatus", userStatus);

            // 📋 Add filter options
            model.addAttribute("roleOptions", List.of("ALL", "ADMIN", "USER"));
            model.addAttribute("userStatusOptions", List.of("ALL", "ACTIVE", "INACTIVE", "SUSPENDED"));

            log.debug("Loaded {} users with pagination - page: {}, size: {}, total: {}", 
                     userPage.getContent().size(), page, size, userPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error loading users data", e);
            model.addAttribute("users", List.of());
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("onlineUsers", 0L);
            model.addAttribute("authenticatedUsers", 0L);
            model.addAttribute("guestUsers", 0L);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("roleOptions", List.of("ALL", "ADMIN", "USER"));
            model.addAttribute("userStatusOptions", List.of("ALL", "ACTIVE", "INACTIVE", "SUSPENDED"));
            model.addAttribute("errorMessage", "Error loading users data: " + e.getMessage());
        }
    }

    /**
     * 🔧 Load Analytics Data (placeholder for future implementation)
     */
    private void loadAnalyticsData(Model model, UserDTO admin) {
        try {
            // TODO: Implement analytics
            model.addAttribute("analytics", Map.of());
            log.debug("Analytics data loading - not yet implemented");
        } catch (Exception e) {
            log.error("Error loading analytics data", e);
            model.addAttribute("analytics", Map.of());
        }
    }

    /**
     * 🔧 Load Dashboard Statistics
     */
    private void loadDashboardStatistics(Model model, UserDTO admin) {
        try {
            // 📊 Quick statistics for dashboard overview
            if (admin != null) {
                model.addAttribute("dashboardLoaded", true);
                log.debug("Dashboard statistics loaded for admin: {}", admin.getId());
            }
        } catch (Exception e) {
            log.error("Error loading dashboard statistics", e);
            model.addAttribute("dashboardLoaded", false);
        }
    }
    /**
     * Heartbeat endpoint to keep user online
     */
    @PostMapping("/heartbeat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> heartbeat(Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                boolean updated = userService.updateLastSeen(email);
                
                // Always return success for heartbeat - don't fail if update fails
                return ResponseEntity.ok(Map.of(
                    "status", "alive", 
                    "timestamp", LocalDateTime.now().toString(),
                    "updated", String.valueOf(updated)
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        } catch (Exception e) {
            log.warn("Heartbeat error for user: {}", 
                    authentication != null ? authentication.getName() : "unknown", e);
            // Return success anyway - heartbeat failures shouldn't be fatal
            return ResponseEntity.ok(Map.of(
                "status", "alive", 
                "warning", "Update failed but heartbeat received"
            ));
        }
    }


}
