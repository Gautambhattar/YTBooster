package com.ytbooster.service;

import java.util.List;

import org.springframework.ui.Model;

import com.ytbooster.model.dto.RechargeDTO;
import com.ytbooster.model.dto.RechargeHistoryDTO;

/**
 * Service for managing Recharge History
 * - Supports wallet-safe operations with BigDecimal
 * - Methods designed for concurrency-safe updates
 */
public interface RechargeHistoryService {

    /** Save a new recharge record */
    void save(RechargeHistoryDTO recharge);

    /** Save a new recharge from RechargeDTO (optional convenience) */
    void saveFromDTO(RechargeDTO recharge);

    /** Find recharge history by user */
    List<RechargeHistoryDTO> findByUserId(Long userId);

    /** Find recharge history by status */
    List<RechargeHistoryDTO> findByStatus(String status);

    /** Find recharge history by user and status */
    List<RechargeHistoryDTO> findByUserIdAndStatus(Long userId, String status);

    /** Find recharge record by UTR (transaction reference) */
    RechargeHistoryDTO findByUtr(Long utr);

    /** Update status by UTR */
    RechargeHistoryDTO updateStatus(Long utr, String status);

    /** Update payment status by UTR */
    void updatePaymentStatus(Long utr, String paymentStatus);

    /** Load UPI information for the front-end model */
    void loadUpiInfo(Model model);

    /** Get all recharge records */
    List<RechargeHistoryDTO> findAll();
}
