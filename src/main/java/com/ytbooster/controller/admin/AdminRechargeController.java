package com.ytbooster.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ytbooster.model.dto.RechargeHistoryDTO;
import com.ytbooster.service.RechargeHistoryService;
import com.ytbooster.serviceImple.UserWallet;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/recharge")
public class AdminRechargeController {

    @Autowired private RechargeHistoryService rechargeService;
    @Autowired private UserWallet walletService;   // credits wallet

    /* ───────── LIST / FILTER ───────── */
    @GetMapping
    public String list(@RequestParam(defaultValue="ALL") String status,
                       @RequestParam(required=false) Long utr,
                       @RequestParam(required=false) Long userId,
                       @RequestParam(name="searchBy",defaultValue="utr") String searchBy,
                       Model model) {

        status = status.toUpperCase();
        List<RechargeHistoryDTO> rows;

        if ("utr".equalsIgnoreCase(searchBy) && utr != null) {
            RechargeHistoryDTO one = rechargeService.findByUtr(utr);
            rows = (one != null) ? List.of(one) : List.of();
        }
        else if ("user".equalsIgnoreCase(searchBy) && userId != null) {
            rows = "ALL".equals(status)
                   ? rechargeService.findByUserId(userId)
                   : rechargeService.findByUserIdAndStatus(userId, status);
        }
        else if (!"ALL".equals(status)) {
            rows = rechargeService.findByStatus(status);
        }
        else {
            rows = rechargeService.findAll();
        }

        Map<String,Object> param = new HashMap<>();
        param.put("searchBy", searchBy);
        if (utr != null)    param.put("utr", utr);
        if (userId != null) param.put("userId", userId);

        model.addAttribute("recharges", rows);
        model.addAttribute("statusFilter", status);
        model.addAttribute("param", param);
        return "admintabs/recharges";
    }

    /* ───────── UPDATE + WALLET CREDIT ───────── */
    @PostMapping("/update")
    @Transactional
    public String update(@RequestParam Long utr,
                         @RequestParam String status,
                         RedirectAttributes ra) {

        try {
            RechargeHistoryDTO rec = rechargeService.findByUtr(utr);
            if (rec == null) throw new IllegalArgumentException("UTR not found");

            String previous = rec.getStatus();
            rec.setStatus(status.toUpperCase());
            rechargeService.updateStatus(utr, status);

            if ("COMPLETE".equalsIgnoreCase(status) && !"COMPLETE".equalsIgnoreCase(previous)) {

                String paymentStatus = rec.getPaymentStatus();
                if (!"CREDITED".equalsIgnoreCase(paymentStatus)) {
                    rechargeService.updatePaymentStatus(utr, "CREDITED");

                    // ✅ Use BigDecimal for amount
                    BigDecimal amount = rec.getAmount() != null ? rec.getAmount() : BigDecimal.ZERO;
                    walletService.walletRecharge(amount, rec.getEmail());
                }
            }
            ra.addFlashAttribute("message", "✅ Update successful");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "❌ " + ex.getMessage());
        }
        return "redirect:/admin/recharge";
    }
}
