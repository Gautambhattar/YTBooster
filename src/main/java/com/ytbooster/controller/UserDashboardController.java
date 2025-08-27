package com.ytbooster.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

import com.ytbooster.model.dto.OrdersDTO;
import com.ytbooster.model.dto.RechargeDTO;
import com.ytbooster.model.dto.ReferHistoryDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.service.RechargeHistoryService;
import com.ytbooster.service.ReferHistoryService;
import com.ytbooster.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserService userService;
    private final RechargeHistoryService rechargeHistoryService;
    private final ReferHistoryService referHistoryService;
    private final com.ytbooster.service.OrdersService orderService;

    // 🔧 FIX: Add global model attributes for user profile and wallet
    @ModelAttribute
    public void addUserAttributes(Model model, Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                UserDTO userDTO = userService.findByEmail(email);
                
                if (userDTO != null) {
                    // User profile attributes
                    model.addAttribute("username", userDTO.getName());
                    model.addAttribute("userEmail", email);
                    model.addAttribute("userId", userDTO.getId());
                    model.addAttribute("userRole", userDTO.getRole() != null ? userDTO.getRole() : "USER");
                    
                    // Wallet attributes  
                    model.addAttribute("wallet", userDTO.getWallet() != null ? userDTO.getWallet() : 0.0);
                    model.addAttribute("walletBalance", userDTO.getWallet() != null ? userDTO.getWallet() : 0.0);
                    
                    // Full user object
                    model.addAttribute("currentUser", userDTO);
                    
                    log.debug("User attributes loaded for: {}", email);
                } else {
                    log.warn("User not found for email: {}", email);
                    setDefaultAttributes(model);
                }
            } else {
                log.warn("No authentication found");
                setDefaultAttributes(model);
            }
        } catch (Exception e) {
            log.error("Error loading user attributes", e);
            setDefaultAttributes(model);
        }
    }
    
    private void setDefaultAttributes(Model model) {
        model.addAttribute("username", "Guest User");
        model.addAttribute("userEmail", "");
        model.addAttribute("userId", null);
        model.addAttribute("userRole", "USER");
        model.addAttribute("wallet", 0.0);
        model.addAttribute("walletBalance", 0.0);
        model.addAttribute("currentUser", null);
    }

    // Form binding DTOs
    @ModelAttribute("order")
    public OrdersDTO orderDTO() {
        return new OrdersDTO();
    }

    @ModelAttribute("subsorder")
    public OrdersDTO subsOrderDTO() {
        return new OrdersDTO();
    }

    @ModelAttribute("recharge")
    public RechargeDTO rechargeDTO() {
        return new RechargeDTO();
    }

    /**
     * Main dashboard endpoint
     */
    @GetMapping
    public String showDashboard(
            @RequestParam(name = "tab", required = false, defaultValue = "subs") String tab,
            Model model,
            Authentication authentication) {

        // Redirect if not authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            // Get current user (set by @ModelAttribute)
            UserDTO userDTO = (UserDTO) model.getAttribute("currentUser");
            
            if (userDTO == null) {
                log.error("Current user not found in model");
                return "redirect:/login";
            }

            // Load tab-specific data
            switch (tab) {
                case "orders":
                    List<OrdersDTO> orders = orderService.getByUserId(userDTO.getId());
                    model.addAttribute("orders", orders);
                    break;

                case "recharge":
                    model.addAttribute("showPaymentStep", false);
                    rechargeHistoryService.loadUpiInfo(model);
                    break;

                case "rechargeHistory":
                    model.addAttribute("rechargeHistory", rechargeHistoryService.findByUserId(userDTO.getId()));
                    break;

                case "refer":
                    List<ReferHistoryDTO> referralHistory = referHistoryService.findByUserId(userDTO.getId());
                    model.addAttribute("referralCode", userDTO.getReferCode());
                    model.addAttribute("referralHistory", referralHistory);
                    break;

                case "subs":
                case "views":
                default:
                    // Default content
                    break;
            }

            model.addAttribute("tab", tab);
            return "dashboard";
            
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("errorMessage", "Error loading dashboard");
            return "dashboard";
        }
    }
}
