package com.ytbooster.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ytbooster.model.dto.OrdersDTO;
import com.ytbooster.service.OrdersService;

@Controller
@RequestMapping("/admin/orders")  // ✅ Changed from "/orders" to avoid conflict
public class AdminOrdersController {

    @Autowired
    private OrdersService ordersService;

    /**
     * Display orders with filtering capability
     * URL: GET /admin/orders?status=ALL|PENDING|COMPLETE
     */
    @GetMapping
    public String listOrders(Model model,
                             @RequestParam(name = "status", defaultValue = "ALL") String status) {

        List<OrdersDTO> orders;

        try {
            if ("ALL".equalsIgnoreCase(status)) {
                orders = ordersService.getAllOrders();
            } else {
                orders = ordersService.getOrdersByStatus(status.toUpperCase());
            }
        } catch (Exception e) {
            // Handle service errors gracefully
            orders = List.of(); // Empty list
            model.addAttribute("error", "❌ Failed to load orders: " + e.getMessage());
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status.toUpperCase()); // For active tab highlight

        return "admintabs/orders"; // Make sure this template exists
    }

    /**
     * Update order status
     * URL: POST /admin/orders/update
     */
    @PostMapping("/update")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") String status,
                                    @RequestParam(value = "currentStatus", defaultValue = "ALL") String currentStatus,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Validate inputs
            if (orderId == null || orderId <= 0) {
                redirectAttributes.addFlashAttribute("error", "❌ Invalid order ID!");
                return "redirect:/admin/orders?status=" + currentStatus;
            }

            if (status == null || status.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "❌ Status cannot be empty!");
                return "redirect:/admin/orders?status=" + currentStatus;
            }

            // Get and update order
            OrdersDTO order = ordersService.getById(orderId);
            if (order != null) {
                String oldStatus = order.getStatus();
                order.setStatus(status.toUpperCase());
                ordersService.update(order);
                
                redirectAttributes.addFlashAttribute("message", 
                    String.format("✅ Order #%d status updated from %s to %s", 
                                orderId, oldStatus, status.toUpperCase()));
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "❌ Order #" + orderId + " not found!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "❌ Failed to update order: " + e.getMessage());
        }
        
        // Redirect back to orders with the same filter status
        return "redirect:/admin/orders?status=" + currentStatus;
    }

    /**
     * Bulk update orders (optional - for future enhancement)
     */
    @PostMapping("/bulk-update")
    public String bulkUpdateOrders(@RequestParam("orderIds") List<Long> orderIds,
                                   @RequestParam("status") String status,
                                   @RequestParam(value = "currentStatus", defaultValue = "ALL") String currentStatus,
                                   RedirectAttributes redirectAttributes) {
        try {
            int updatedCount = 0;
            for (Long orderId : orderIds) {
                OrdersDTO order = ordersService.getById(orderId);
                if (order != null) {
                    order.setStatus(status.toUpperCase());
                    ordersService.update(order);
                    updatedCount++;
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                String.format("✅ Updated %d orders to %s status", updatedCount, status.toUpperCase()));
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "❌ Failed to bulk update orders: " + e.getMessage());
        }
        
        return "redirect:/admin/orders?status=" + currentStatus;
    }
}
