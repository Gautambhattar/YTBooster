package com.ytbooster.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // Renders the login page (templates/login.html)
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
