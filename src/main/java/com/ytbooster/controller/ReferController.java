package com.ytbooster.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ytbooster.model.dto.ReferHistoryDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.service.ReferHistoryService;
import com.ytbooster.serviceImple.UserServiceImpl;

@Controller
public class ReferController {

    @Autowired
    private ReferHistoryService referHistoryService;
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

    // ✅ Show referral page (only code + history)
    @GetMapping("/referrals")
    public String showReferralPage(Model model, Principal principal) {
    	UserDTO userdto= userService.findByEmail(principal.getName());
        // Generate / fetch referral code for logged-in user
        String referralCode = userdto.getReferCode();
        System.out.println(referralCode);

        // Fetch referral history of this user
        List<ReferHistoryDTO> history = referHistoryService.findByUserId(userdto.getId());

        model.addAttribute("referralCode", referralCode);
        model.addAttribute("referralHistory", history);

        return "refer"; // Thymeleaf page
    }
}
