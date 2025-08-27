package com.ytbooster.repository;

import com.ytbooster.model.UpiPaymentInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpiPaymentInfoRepository extends JpaRepository<UpiPaymentInfo, Long> {
    
    List<UpiPaymentInfo> findByAdminIdAndIsActiveTrue(Long adminId);
    
    Page<UpiPaymentInfo> findByAdminIdAndIsActiveTrue(Long adminId, Pageable pageable);
    
    Optional<UpiPaymentInfo> findByUpiIdAndAdminIdAndIsActiveTrue(String upiId, Long adminId);
    
    Optional<UpiPaymentInfo> findTopByOrderByCreatedAtDesc();
    
    boolean existsByUpiIdAndAdminIdAndIsActiveTrue(String upiId, Long adminId);
    
    @Query("SELECT u FROM UpiPaymentInfo u WHERE u.adminId = :adminId AND u.isActive = true ORDER BY u.createdAt DESC")
    List<UpiPaymentInfo> findActiveUpiPaymentsByAdmin(@Param("adminId") Long adminId);
    
    @Modifying
    @Query("UPDATE UpiPaymentInfo u SET u.isActive = false WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);
    
    @Query("SELECT COUNT(u) FROM UpiPaymentInfo u WHERE u.adminId = :adminId AND u.isActive = true")
    long countActiveByAdminId(@Param("adminId") Long adminId);
 // Add these methods to UpiPaymentInfoRepository.java

    @Query("SELECT u FROM UpiPaymentInfo u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    List<UpiPaymentInfo> findByIsActiveTrue();

    Optional<UpiPaymentInfo> findTopByIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT COUNT(u) FROM UpiPaymentInfo u WHERE u.adminId = :adminId AND u.isActive = true AND u.qrCode IS NOT NULL")
    long countActiveByAdminIdWithQrCode(@Param("adminId") Long adminId);

}
