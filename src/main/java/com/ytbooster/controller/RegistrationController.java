package com.ytbooster.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.ytbooster.model.dto.ReferHistoryDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.serviceImple.ReferHistoryServiceImpl;
import com.ytbooster.serviceImple.RegistrationService;
import com.ytbooster.serviceImple.UserServiceImpl;

@Controller
public class RegistrationController {

    @Autowired
    private RegistrationService register;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private ReferHistoryServiceImpl refer;

    // ✅ Signup form
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("userDto", new UserDTO()); 
        return "signup";
    }

    // ✅ Handle signup + referral
    @PostMapping("/signup")
    public String signupSubmit(
            @ModelAttribute("userDto") UserDTO userDto,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String referralCode,
            Model model) {
        try {
            // 1. Register new user
            userDto = register.register(userDto, key);

            // 2. If referral code entered, map referrer + new user
            if (referralCode != null && !referralCode.isBlank()) {
                UserDTO referrer = userService.findByReferralCode(referralCode);
                if (referrer != null) {
                    ReferHistoryDTO referHistory = new ReferHistoryDTO();
                    referHistory.setUserId(referrer.getId());           // Referrer ID
                    referHistory.setReferredUserId(userDto.getId());    // New user ID
                    referHistory.setStatus("PENDING");
                    referHistory.setAmount(BigDecimal.valueOf(20));     // reward as BigDecimal
                    refer.save(referHistory);
                }
            }

            // Redirect to login page after success
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("error", "Signup failed: " + e.getMessage());
            return "signup"; // stay on signup page with error
        }
    }

    // Optional: login page
//    @GetMapping("/login")
//    public String loginPage() {
//        return "login";
//    }
}
