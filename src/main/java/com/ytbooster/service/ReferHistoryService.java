package com.ytbooster.service;

import java.util.List;

import com.ytbooster.model.ReferHistory;
import com.ytbooster.model.dto.ReferHistoryDTO;

public interface ReferHistoryService {

    /** Save a referral history record */
    ReferHistory save(ReferHistoryDTO referhistory);

    /** Find referral history DTOs by referrer user ID */
    List<ReferHistoryDTO> findByUserId(Long userId);

    /** Find referral histories by status */
    List<ReferHistory> findByStatus(String userReferStatus);

    /** Count referral histories for a specific user */
    long countByUserId(Long userId);

    /** Find referral history by referred user ID */
    ReferHistory findByReferredUserId(Long userId);

    /** Reset referral rewards for a given user (optional admin operation) */
    void resetReferralRewards(Long userId);
}
