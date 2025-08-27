package com.ytbooster.model.mapper;

import com.ytbooster.model.RechargeHistory;
import com.ytbooster.model.dto.RechargeDTO;
import com.ytbooster.model.dto.RechargeHistoryDTO;

/**
 * Mapper for RechargeHistory <-> DTOs
 * - BigDecimal-safe
 * - Null-safe
 */
public class RechargeHistoryMapper {

    /** Entity → RechargeHistoryDTO */
    public static RechargeHistoryDTO toDTO(RechargeHistory entity) {
        if (entity == null) return null;

        return RechargeHistoryDTO.builder()
                .utr(entity.getUtr())
                .userId(entity.getUserId())
                .amount(entity.getAmount()) // BigDecimal
                .email(entity.getEmail())
                .mobileNumber(entity.getMobileNumber())
                .paymentStatus(entity.getPaymentStatus())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /** RechargeHistoryDTO → Entity */
    public static RechargeHistory toEntity(RechargeHistoryDTO dto) {
        if (dto == null) return null;

        RechargeHistory entity = new RechargeHistory();
        entity.setUtr(dto.getUtr());
        entity.setUserId(dto.getUserId());
        entity.setAmount(dto.getAmount());
        entity.setEmail(dto.getEmail());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setPaymentStatus(dto.getPaymentStatus());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    /** RechargeDTO → RechargeHistory Entity */
    public static RechargeHistory toEntityRecharge(RechargeDTO dto) {
        if (dto == null) return null;

        RechargeHistory entity = new RechargeHistory();
        entity.setUtr(dto.getUtr());
        entity.setUserId(dto.getUserId());
        entity.setAmount(dto.getAmount()); // BigDecimal
        entity.setEmail(dto.getEmail());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setPaymentStatus(dto.getPaymentStatus());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setStatus(dto.getStatus());
        return entity;
    }
}
