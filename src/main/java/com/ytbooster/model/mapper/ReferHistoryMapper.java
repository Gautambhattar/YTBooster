package com.ytbooster.model.mapper;

import com.ytbooster.model.ReferHistory;
import com.ytbooster.model.dto.ReferHistoryDTO;

public class ReferHistoryMapper {

    public static ReferHistoryDTO toDTO(ReferHistory entity) {
        if (entity == null) return null;
        return ReferHistoryDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .referredUserId(entity.getReferredUserId())
                .totalRecharge(entity.getTotalRecharge())
                .rewarded(entity.isRewarded())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .lastUpdate(entity.getLastUpdate())
                .build();
    }

    public static ReferHistory toEntity(ReferHistoryDTO dto) {
        if (dto == null) return null;
        return ReferHistory.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .referredUserId(dto.getReferredUserId())
                .totalRecharge(dto.getTotalRecharge())
                .rewarded(dto.isRewarded())
                .amount(dto.getAmount())
                .status(dto.getStatus())
                .lastUpdate(dto.getLastUpdate())
                .build();
    }
}
