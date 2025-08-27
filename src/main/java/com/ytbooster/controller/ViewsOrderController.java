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
import com.ytbooster.serviceImple.UserWallet;

import lombok.RequiredArgsConstructor;

/**
 * Handles "Views" orders (watch time purchases)
 * - Deducts wallet safely under concurrency
 * - Uses constructor injection
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class ViewsOrderController {

    private final UserService userService;
    private final UserWallet walletService;
    private final OrdersService orderService;

    @ModelAttribute("order")
    public OrdersDTO orderDTO() {
        return new OrdersDTO();
    }

    // 🔧 NEW: Add user profile attributes using getName() only
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

    /**
     * Handle Views order submission
     * - Deducts wallet safely
     * - Creates order only if wallet deduction succeeds
     */
    @PostMapping("/views")
    @Transactional
    public String placeViewsOrder(@ModelAttribute("order") OrdersDTO order,
                                  Model model,
                                  Principal principal) {

        String username = principal.getName();
        UserDTO userDTO = userService.findByEmail(username);

        // Attach user info to order
        order.setUserId(userDTO.getId());
        order.setOrderDescription("Views & Watch Time");

        // Deduct wallet safely
        boolean isWalletUpdated = walletService.walletDeduct(order.getAmount(), username);

        if (!isWalletUpdated) {
            model.addAttribute("error", "⚠️ Insufficient wallet balance!");
            model.addAttribute("tab", "views");
            model.addAttribute("username", principal.getName());
            model.addAttribute("wallet", userDTO.getWallet());
        } else {
            orderService.createOrder(order);

            // Reload wallet safely after update
            BigDecimal updatedWallet = userService.findByEmail(username).getWallet();
            model.addAttribute("success", "✅ Views order placed successfully!");
            model.addAttribute("wallet", updatedWallet);
            model.addAttribute("tab", "views");
        }

        // Reset form
        model.addAttribute("order", new OrdersDTO());
        return "dashboard";
    }
}
