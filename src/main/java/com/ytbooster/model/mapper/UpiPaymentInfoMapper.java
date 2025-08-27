package com.ytbooster.model.mapper;

import com.ytbooster.model.UpiPaymentInfo;
import com.ytbooster.model.dto.UpiPaymentInfoDTO;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UpiPaymentInfoMapper {

    public UpiPaymentInfoDTO toDTO(UpiPaymentInfo entity) {
        if (entity == null) {
            return null;
        }

        UpiPaymentInfoDTO.UpiPaymentInfoDTOBuilder builder = UpiPaymentInfoDTO.builder()
                .id(entity.getId())
                .upiId(entity.getUpiId())
                .accountHolderName(entity.getAccountHolderName())
                .note(entity.getNote())
                .adminId(entity.getAdminId())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // Handle QR code conversion
        if (entity.getQrCode() != null && entity.getQrCode().length > 0) {
            builder.withQrCode(entity.getQrCode());
        }

        return builder.build();
    }

    public UpiPaymentInfo toEntity(UpiPaymentInfoDTO dto) {
        if (dto == null) {
            return null;
        }

        UpiPaymentInfo.UpiPaymentInfoBuilder builder = UpiPaymentInfo.builder()
                .id(dto.getId())
                .upiId(dto.getUpiId())
                .accountHolderName(dto.getAccountHolderName())
                .note(dto.getNote())
                .adminId(dto.getAdminId())
                .isActive(dto.getIsActive())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt());

        // Handle QR code conversion from base64
        if (dto.getQrBase64() != null && !dto.getQrBase64().isEmpty()) {
            try {
                byte[] qrCode = Base64.getDecoder().decode(dto.getQrBase64());
                builder.qrCode(qrCode);
            } catch (IllegalArgumentException e) {
                // Handle invalid base64
            }
        } else if (dto.getQrCode() != null) {
            builder.qrCode(dto.getQrCode());
        }

        return builder.build();
    }

    public List<UpiPaymentInfoDTO> toDTOList(List<UpiPaymentInfo> entities) {
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<UpiPaymentInfo> toEntityList(List<UpiPaymentInfoDTO> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDTO(UpiPaymentInfoDTO dto, UpiPaymentInfo entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setUpiId(dto.getUpiId());
        entity.setAccountHolderName(dto.getAccountHolderName());
        entity.setNote(dto.getNote());
        entity.setIsActive(dto.getIsActive());

        // Handle QR code update
        if (dto.getQrBase64() != null && !dto.getQrBase64().isEmpty()) {
            try {
                byte[] qrCode = Base64.getDecoder().decode(dto.getQrBase64());
                entity.setQrCode(qrCode);
            } catch (IllegalArgumentException e) {
                // Handle invalid base64
            }
        } else if (dto.getQrCode() != null) {
            entity.setQrCode(dto.getQrCode());
        }
    }
}
