package com.ytbooster.controller.admin;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserListController {

    private final UserService userService;

    @GetMapping("/users")
    public String userList(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String role,
                          @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<UserDTO> userPage;

        // Apply search and filters
        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search.trim(), pageable);
        } else if (role != null && !role.isEmpty()) {
            userPage = userService.getUsersByRole(role, pageable);
        } else if (status != null && !status.isEmpty()) {
            userPage = userService.getUsersByStatus(status, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }

        // Add data to model
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("totalUsers", userService.getTotalUserCount());
        model.addAttribute("onlineUsers", userService.getOnlineUserCount());
        model.addAttribute("authenticatedUsers", userService.getAuthenticatedUserCount());
        model.addAttribute("guestUsers", userService.getGuestUserCount());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        return "admintabs/UserList";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        UserDTO user = userService.getUserById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/users?error=User not found";
        }
        model.addAttribute("user", user);
        return "admintabs/UserDetail";
    }

    @PostMapping("/users/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateUserStatus(@PathVariable Long id,
                                                               @RequestBody Map<String, String> request) {
        String status = request.get("status");
        boolean updated = userService.updateUserStatus(id, status);

        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update status"));
        }
    }

    @PostMapping("/users/{id}/authentication")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateAuthentication(@PathVariable Long id,
                                                                   @RequestBody Map<String, Boolean> request) {
        Boolean authenticated = request.get("authenticated");
        boolean updated = userService.updateAuthenticationStatus(id, authenticated);

        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Authentication status updated"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update authentication"));
        }
    }

    @PostMapping("/users/{id}/online")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateOnlineStatus(@PathVariable Long id,
                                                                 @RequestBody Map<String, Boolean> request) {
        Boolean online = request.get("online");
        boolean updated = userService.updateOnlineStatus(id, online);

        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Online status updated"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update online status"));
        }
    }

    @PostMapping("/users/{id}/wallet/add")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addToWallet(@PathVariable Long id,
                                                          @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount"));
        }

        boolean updated = userService.addToWallet(id, amount);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Amount added to wallet"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add amount"));
        }
    }

    @PostMapping("/users/{id}/wallet/deduct")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deductFromWallet(@PathVariable Long id,
                                                               @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount"));
        }

        boolean updated = userService.deductFromWallet(id, amount);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Amount deducted from wallet"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to deduct amount or insufficient balance"));
        }
    }

    @DeleteMapping("/users/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete user"));
        }
    }
}
