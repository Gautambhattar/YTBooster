package com.ytbooster.controller;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ytbooster.model.dto.RechargeHistoryDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.serviceImple.RechargeHistoryServiceImpl;
import com.ytbooster.serviceImple.UserServiceImpl;

@Controller
public class RechargeHistoryController {

    @Autowired
    private RechargeHistoryServiceImpl rechargeHistoryService;
    
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

    // ✅ Show recharge history for a specific user
    @GetMapping("/rechargeHistory")
    public String viewHistory(@RequestParam Long userId, Model model)
            throws ExecutionException, InterruptedException {

        List<RechargeHistoryDTO> history = rechargeHistoryService.findByUserId(userId);
        model.addAttribute("rechargeHistory", history);

        return "rechargeHistory"; // thymeleaf template
    }

    // ✅ Search by UTR (transaction id) → still useful for users
    @GetMapping("/search-utr")
    public String searchByUtr(@RequestParam Long utr, 
                              @RequestParam Long userId, 
                              Model model)
            throws ExecutionException, InterruptedException {

        RechargeHistoryDTO history = rechargeHistoryService.findByUtr(utr);

        // filter results to only show current user's records

        model.addAttribute("rechargeHistory", history);
        return "rechargeHistory";
    }
}
