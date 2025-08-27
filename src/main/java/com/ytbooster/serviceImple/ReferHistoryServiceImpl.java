package com.ytbooster.serviceImple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.scheduling.annotation.Scheduled; // 🔧 DISABLED
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ytbooster.model.ReferHistory;
import com.ytbooster.model.dto.ReferHistoryDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.model.mapper.ReferHistoryMapper;
import com.ytbooster.repository.RechargeHistoryRepository;
import com.ytbooster.repository.ReferHistoryRepository;
import com.ytbooster.service.ReferHistoryService;

import jakarta.transaction.Transactional;

@Service
@EnableScheduling
public class ReferHistoryServiceImpl implements ReferHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ReferHistoryServiceImpl.class);

    @Autowired
    private ReferHistoryRepository referHistoryRepository;

    @Autowired
    private RechargeHistoryRepository rechargeHistoryRepository;

    @Autowired
    private UserWallet userWallet;

    @Autowired
    private UserServiceImpl userService;

    @Override
    public ReferHistory save(ReferHistoryDTO referhistory) {
        try {
            ReferHistory refer = ReferHistoryMapper.toEntity(referhistory);
            ReferHistory savedRefer = referHistoryRepository.save(refer);
            log.info("Successfully saved referral: referrer {} -> referred {}", 
                    savedRefer.getUserId(), savedRefer.getReferredUserId());
            return savedRefer;
        } catch (Exception e) {
            log.error("Error saving referral history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save referral history", e);
        }
    }

    @Override
    public ReferHistory findByReferredUserId(Long userId) {
        try {
            return referHistoryRepository.findByReferredUserId(userId);
        } catch (Exception e) {
            log.error("Error finding referral by referred user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to find referral history", e);
        }
    }

    /**
     * 🔧 DISABLED: Commented out to avoid conflict with immediate processing
     * Enable this only if you want batch processing instead of immediate processing
     */
    /*
    @Transactional
    @Scheduled(fixedRate = 300000) // Changed to 5 minutes to avoid conflicts
    public void updateReferralRewards() {
        log.info("🎯 Starting scheduled referral rewards processing");
        // ... your existing batch processing code
    }
    */

    @Override
    public List<ReferHistoryDTO> findByUserId(Long userId) {
        try {
            return referHistoryRepository.findByUserId(userId)
                    .stream()
                    .map(ReferHistoryMapper::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error finding referral histories by user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to find referral histories", e);
        }
    }

    @Override
    public List<ReferHistory> findByStatus(String userReferStatus) {
        try {
            return referHistoryRepository.findByStatus(userReferStatus);
        } catch (Exception e) {
            log.error("Error finding referral histories by status {}: {}", userReferStatus, e.getMessage(), e);
            throw new RuntimeException("Failed to find referral histories by status", e);
        }
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            return referHistoryRepository.findByUserId(userId).size();
        } catch (Exception e) {
            log.error("Error counting referral histories for user ID {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void resetReferralRewards(Long userId) {
        try {
            List<ReferHistory> histories = referHistoryRepository.findByUserId(userId);
            
            if (histories.isEmpty()) {
                log.info("No referral histories found for user {}", userId);
                return;
            }
            
            for (ReferHistory history : histories) {
                history.setRewarded(false);
                history.setAmount(BigDecimal.ZERO);
                history.setStatus("PENDING");
                history.setLastUpdate(LocalDate.now());
            }
            
            referHistoryRepository.saveAll(histories);
            
            log.info("Successfully reset {} referral rewards for user {}", histories.size(), userId);
            
        } catch (Exception e) {
            log.error("Error resetting referral rewards for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to reset referral rewards", e);
        }
    }

    /**
     * 📊 Get referral reward statistics
     */
    public Map<String, Object> getReferralRewardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            long totalReferrals = referHistoryRepository.count();
            long rewardedReferrals = referHistoryRepository.countByRewardedTrue();
            long pendingReferrals = referHistoryRepository.countByRewardedFalse();
            
            BigDecimal totalRewardsDistributed = referHistoryRepository.sumTotalRewardsDistributed();
            
            stats.put("totalReferrals", totalReferrals);
            stats.put("rewardedReferrals", rewardedReferrals);
            stats.put("pendingReferrals", pendingReferrals);
            stats.put("totalRewardsDistributed", totalRewardsDistributed);
            stats.put("rewardPercentage", totalReferrals > 0 ? 
                     Math.round(rewardedReferrals * 100.0 / totalReferrals * 100.0) / 100.0 : 0);
            
            return stats;
            
        } catch (Exception e) {
            log.error("Error getting referral reward stats: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
}
