package com.ytbooster.service;

import java.math.BigDecimal;

/**
 * Concurrency-safe wallet operations interface
 * - BigDecimal for money amounts
 * - Deduct and recharge wallet safely
 */
public interface UserWalletServices {

    /**
     * Deduct amount from user's wallet
     * @param amount BigDecimal amount to deduct
     * @param email user's email
     * @return true if deduction successful, false if insufficient balance or user not found
     */
    boolean walletDeduct(BigDecimal amount, String email);

    /**
     * Recharge amount to user's wallet
     * @param amount BigDecimal amount to add
     * @param email user's email
     * @return true if recharge successful, false if user not found
     */
    boolean walletRecharge(BigDecimal amount, String email);
}
