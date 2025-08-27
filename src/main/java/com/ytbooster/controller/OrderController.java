package com.ytbooster.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ytbooster.model.dto.OrdersDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.service.OrdersService;
import com.ytbooster.service.UserService;
import com.ytbooster.service.UserWalletServices;

import lombok.RequiredArgsConstructor;

/**
 * Handles Subscriber Orders (and similar)
 * - Deducts wallet safely under concurrency
 * - Uses constructor injection
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final UserService userService;
    private final UserWalletServices walletService;
    private final OrdersService orderService;

    @ModelAttribute("subsorder")
    public OrdersDTO subsOrderDTO() {
        return new OrdersDTO();
    }

    // 🔧 NEW: Add user profile attributes for profile section display
    @ModelAttribute
    public void addUserProfileAttributes(Model model, Principal principal) {
        if (principal != null) {
            try {
                String username = principal.getName();
                UserDTO userDTO = userService.findByEmail(username);
                
                if (userDTO != null) {
                    // Profile attributes for display
                    String displayName = "";
                    if (userDTO.getName() != null ) {
                        displayName = userDTO.getName();
                    } else if (userDTO.getName() != null) {
                        displayName = userDTO.getName();
                    } else {
                        displayName = username;
                    }
                    
                    model.addAttribute("username", displayName);
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

    /**
     * Handle Subscriber Order Submission
     * - Wallet deduction happens before order creation
     * - Atomic & concurrency-safe
     */
    @PostMapping("/subs")
    @Transactional
    public String placeSubscribersOrder(@ModelAttribute("order") OrdersDTO order,
                                        Model model, Principal principal) {

        String username = principal.getName();
        UserDTO userDTO = userService.findByEmail(username);

        // Ensure user exists
        if (userDTO == null) {
            model.addAttribute("subserror", "⚠️ User not found!");
            return "fragments/subs";
        }

        // Prepare order
        order.setOrderDescription("Subscribers");
        order.setUserId(userDTO.getId());

        // Validate amount
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("subserror", "⚠️ Invalid order amount!");
            return "fragments/subs";
        }

        // Wallet deduction
        boolean isWalletUpdated = walletService.walletDeduct(
                order.getAmount(), username
        );

        if (!isWalletUpdated) {
        	 model.addAttribute("tab", "subs");
             model.addAttribute("username", principal.getName());
             model.addAttribute("wallet", userDTO.getWallet());
            model.addAttribute("subserror", "⚠️ Insufficient wallet balance!");
        } else {
            // Save order after wallet deduction
            orderService.createOrder(order);

            BigDecimal updatedWallet = userService.findByEmail(username).getWallet();
            model.addAttribute("subssuccess", "✅ Order placed successfully!");
            model.addAttribute("wallet", updatedWallet);
            model.addAttribute("tab", "subs");
            model.addAttribute("username", principal.getName());
            
        }

        // Reset form
        model.addAttribute("order", new OrdersDTO());
        return "dashboard"; // reload page
    }

    // Additional order types (Views, Likes, etc.) can follow the same pattern
}
