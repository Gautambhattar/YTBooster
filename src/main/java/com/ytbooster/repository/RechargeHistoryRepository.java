package com.ytbooster.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ytbooster.model.RechargeHistory;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

public interface RechargeHistoryRepository extends JpaRepository<RechargeHistory, Long> {

    List<RechargeHistory> findByUserId(Long userId);

    List<RechargeHistory> findByStatus(String status);
    
    boolean existsByUtr(Long utr);

    List<RechargeHistory> findByUserIdAndStatus(Long userId, String status);

    RechargeHistory findByUtr(Long utr);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE RechargeHistory r SET r.status = :status WHERE r.utr = :utr")
    int updateStatus(@Param("utr") Long utr, @Param("status") String status);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE RechargeHistory r SET r.paymentStatus = :paymentStatus WHERE r.utr = :utr")
    int updatePaymentStatus(@Param("utr") Long utr, @Param("paymentStatus") String paymentStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RechargeHistory r WHERE r.utr = :utr")
    RechargeHistory findByUtrForUpdate(@Param("utr") Long utr);
    
    // 🔧 FIXED: Consistent use of 'COMPLETE' status
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RechargeHistory r WHERE r.userId = :userId AND r.status = 'COMPLETE'")
    BigDecimal sumRechargeByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(r.amount) FROM RechargeHistory r WHERE r.userId = :userId AND r.status = 'COMPLETE'")
    BigDecimal maxCompletedRechargeByUserId(@Param("userId") Long userId);

    // 🔧 FIXED: Changed from 'CREDITED' to 'COMPLETE'
    @Query("SELECT r.userId as userId, COALESCE(SUM(r.amount), 0) as total " +
            "FROM RechargeHistory r " +
            "WHERE r.userId IN :userIds AND r.status = 'COMPLETE' " +
            "GROUP BY r.userId")
     List<Object[]> getTotalRechargesByUserIdsRaw(@Param("userIds") Set<Long> userIds);
     
     @Query("SELECT r.userId as userId, MAX(r.amount) as maxAmount " +
            "FROM RechargeHistory r " +
            "WHERE r.userId IN :userIds AND r.status = 'COMPLETE' " +
            "GROUP BY r.userId")
     List<Object[]> getMaxCompletedRechargesByUserIdsRaw(@Param("userIds") Set<Long> userIds);
     
     // Helper methods remain the same
     default Map<Long, BigDecimal> getTotalRechargesByUserIds(Set<Long> userIds) {
         return getTotalRechargesByUserIdsRaw(userIds).stream()
                 .collect(Collectors.toMap(
                     result -> (Long) result[0],
                     result -> (BigDecimal) result[1]
                 ));
     }
     
     default Map<Long, BigDecimal> getMaxCompletedRechargesByUserIds(Set<Long> userIds) {
         return getMaxCompletedRechargesByUserIdsRaw(userIds).stream()
                 .collect(Collectors.toMap(
                     result -> (Long) result[0],
                     result -> (BigDecimal) result[1]
                 ));
     }
 }
