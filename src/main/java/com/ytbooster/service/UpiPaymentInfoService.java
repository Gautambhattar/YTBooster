package com.ytbooster.service;

import com.ytbooster.model.dto.UpiPaymentInfoDTO;
import com.ytbooster.serviceImple.UpiPaymentInfoServiceImpl.UpiStatsDTO;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UpiPaymentInfoService {
    
    // Basic CRUD operations
    UpiPaymentInfoDTO saveUpiInfo(UpiPaymentInfoDTO dto);
    void updateUpiInfo(Long id, UpiPaymentInfoDTO dto);
    Optional<UpiPaymentInfoDTO> getById(Long id);
    List<UpiPaymentInfoDTO> getByAdminId(Long adminId);
    List<UpiPaymentInfoDTO> getAll();
    
    // Special queries
    Optional<UpiPaymentInfoDTO> getLatest();
    Page<UpiPaymentInfoDTO> findByAdminId(Long adminId, Pageable pageable);
    
    // Security methods
    List<UpiPaymentInfoDTO> findByAdminIdSecure(Long adminId, Long currentAdminId);
    Optional<UpiPaymentInfoDTO> findActiveForPublic();
    
    // Statistics
    UpiStatsDTO getUpiStats(Long adminId);
}
