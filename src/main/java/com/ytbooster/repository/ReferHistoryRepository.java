package com.ytbooster.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ytbooster.model.ReferHistory;

public interface ReferHistoryRepository extends JpaRepository<ReferHistory, Long> {
    
    // Your existing methods
    List<ReferHistory> findByUserId(Long userId);
    List<ReferHistory> findByStatus(String status);
    ReferHistory findByReferredUserId(Long userId);
    
    // NEW: Batch processing methods
    List<ReferHistory> findByRewardedFalse();
    
    List<ReferHistory> findByRewardedTrue();
    
    long countByRewardedTrue();
    
    long countByRewardedFalse();
    
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM ReferHistory r WHERE r.rewarded = true")
    BigDecimal sumTotalRewardsDistributed();
    
    @Query("SELECT r FROM ReferHistory r WHERE r.rewarded = false AND r.totalRecharge >= :minAmount")
    List<ReferHistory> findEligibleForReward(@Param("minAmount") BigDecimal minAmount);
}
