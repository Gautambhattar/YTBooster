package com.ytbooster.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ytbooster.model.dto.OrdersDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.serviceImple.OrdersServiceImpl;
import com.ytbooster.serviceImple.UserServiceImpl;

@Controller
public class OrderHistoryController {

    @Autowired
    private OrdersServiceImpl orderService;

    @Autowired
    private UserServiceImpl userService;

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

    /**
     * Show order history page
     * - Only logged-in user sees their own orders
     */
    @GetMapping("/orders")
    public String orderHistory(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login"; // force login if not authenticated
        }

        // Get logged-in user
        UserDTO userdto = userService.findByEmail(principal.getName());
        if (userdto == null) {
            return "redirect:/login"; // safety check
        }

        // Fetch only this user's orders
        List<OrdersDTO> orders = orderService.getByUserId(userdto.getId());

        model.addAttribute("orders", orders);
        model.addAttribute("username", userdto.getName());
        model.addAttribute("wallet", userdto.getWallet());

        return "dashboard"; // fixed typo (was "dasboard")
    }
}
