package com.ytbooster.serviceImple;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ytbooster.exception.DuplicateResourceException;
import com.ytbooster.exception.ResourceNotFoundException;
import com.ytbooster.model.UpiPaymentInfo;
import com.ytbooster.model.dto.UpiPaymentInfoDTO;
import com.ytbooster.model.mapper.UpiPaymentInfoMapper;
import com.ytbooster.repository.UpiPaymentInfoRepository;
import com.ytbooster.service.UpiPaymentInfoService; // 🔧 FIXED: Implement interface

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpiPaymentInfoServiceImpl implements UpiPaymentInfoService { // 🔧 FIXED: Implement interface

    private final UpiPaymentInfoRepository repository;
    private final UpiPaymentInfoMapper mapper;

    @Transactional(readOnly = true)
    public List<UpiPaymentInfoDTO> findByAdminId(Long adminId) {
        log.debug("Finding UPI payment info for admin: {}", adminId);
        List<UpiPaymentInfo> entities = repository.findByAdminIdAndIsActiveTrue(adminId);
        return mapper.toDTOList(entities);
    }

    @Transactional(readOnly = true)
    public Page<UpiPaymentInfoDTO> findByAdminId(Long adminId, Pageable pageable) {
        log.debug("Finding paginated UPI payment info for admin: {}", adminId);
        Page<UpiPaymentInfo> entities = repository.findByAdminIdAndIsActiveTrue(adminId, pageable);
        return entities.map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public UpiPaymentInfoDTO findById(Long id) {
        log.debug("Finding UPI payment info by ID: {}", id);
        UpiPaymentInfo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UPI Payment Info not found with ID: " + id));
        return mapper.toDTO(entity);
    }

    @Transactional(readOnly = true)
    public Optional<UpiPaymentInfoDTO> findByUpiIdAndAdminId(String upiId, Long adminId) {
        log.debug("Finding UPI payment info by UPI ID: {} and admin: {}", upiId, adminId);
        return repository.findByUpiIdAndAdminIdAndIsActiveTrue(upiId, adminId)
                .map(mapper::toDTO);
    }

    public UpiPaymentInfoDTO create(UpiPaymentInfoDTO dto) {
        log.info("Creating new UPI payment info for admin: {}", dto.getAdminId());
        
        // 🔧 FIXED: Validate required fields
        validateUpiPaymentInfo(dto);
        
        // Check for duplicate
        if (repository.existsByUpiIdAndAdminIdAndIsActiveTrue(dto.getUpiId(), dto.getAdminId())) {
            throw new DuplicateResourceException("UPI ID already exists for this admin: " + dto.getUpiId());
        }

        UpiPaymentInfo entity = mapper.toEntity(dto);
        UpiPaymentInfo saved = repository.save(entity);
        log.info("Created UPI payment info with ID: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    public UpiPaymentInfoDTO update(Long id, UpiPaymentInfoDTO dto) {
        log.info("Updating UPI payment info with ID: {}", id);
        
        // 🔧 FIXED: Validate required fields
        validateUpiPaymentInfo(dto);
        
        UpiPaymentInfo existingEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UPI Payment Info not found with ID: " + id));

        // 🔧 FIXED: Security check - ensure admin can only update their own records
        if (!existingEntity.getAdminId().equals(dto.getAdminId())) {
            throw new SecurityException("Access denied: Admin can only update their own UPI settings");
        }

        // Check for duplicate UPI ID (excluding current record)
        Optional<UpiPaymentInfo> duplicate = repository.findByUpiIdAndAdminIdAndIsActiveTrue(dto.getUpiId(), dto.getAdminId());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new DuplicateResourceException("UPI ID already exists for this admin: " + dto.getUpiId());
        }

        mapper.updateEntityFromDTO(dto, existingEntity);
        UpiPaymentInfo updated = repository.save(existingEntity);
        log.info("Updated UPI payment info with ID: {}", updated.getId());
        return mapper.toDTO(updated);
    }

    public UpiPaymentInfoDTO uploadQrCode(Long id, MultipartFile file) throws IOException {
        log.info("Uploading QR code for UPI payment info ID: {}", id);
        
        UpiPaymentInfo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UPI Payment Info not found with ID: " + id));

        // 🔧 FIXED: Enhanced file validation
        validateFile(file);

        entity.setQrCode(file.getBytes());
        UpiPaymentInfo updated = repository.save(entity);
        log.info("QR code uploaded for UPI payment info ID: {}", updated.getId());
        return mapper.toDTO(updated);
    }

    public void delete(Long id) {
        log.info("Soft deleting UPI payment info with ID: {}", id);
        
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("UPI Payment Info not found with ID: " + id);
        }

        repository.softDeleteById(id);
        log.info("Soft deleted UPI payment info with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public long countActiveByAdminId(Long adminId) {
        return repository.countActiveByAdminId(adminId);
    }

    // 🔧 FIXED: Better method for finding latest active UPI
    @Transactional(readOnly = true)
    public Optional<UpiPaymentInfoDTO> findLatest() {
        return repository.findTopByIsActiveTrueOrderByCreatedAtDesc()
                .map(mapper::toDTO);
    }

    // ==================== INTERFACE IMPLEMENTATION METHODS ====================

    @Override
    @Transactional(readOnly = true)
    public Optional<UpiPaymentInfoDTO> getById(Long id) {
        log.debug("Getting UPI payment info by ID: {}", id);
        return repository.findById(id).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpiPaymentInfoDTO> getByAdminId(Long adminId) {
        return findByAdminId(adminId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UpiPaymentInfoDTO> getLatest() {
        return findLatest();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpiPaymentInfoDTO> getAll() {
        log.debug("Getting all active UPI payment info");
        List<UpiPaymentInfo> entities = repository.findByIsActiveTrue();
        return mapper.toDTOList(entities);
    }

    @Override
    public UpiPaymentInfoDTO saveUpiInfo(UpiPaymentInfoDTO dto) {
        if (dto.getId() != null) {
            return update(dto.getId(), dto);
        } else {
            return create(dto);
        }
    }

    @Override
    public void updateUpiInfo(Long id, UpiPaymentInfoDTO dto) {
        update(id, dto);
    }

    // ==================== ADDITIONAL HELPER METHODS ====================

    /**
     * 🔧 FIXED: Enhanced file validation
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image (PNG, JPG, JPEG, GIF)");
        }

        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size cannot exceed 10MB. Current size: " + 
                    String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)));
        }

        // Validate image dimensions (optional - basic check)
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 100) { // Very basic check for valid image
                throw new IllegalArgumentException("Invalid image file");
            }
        } catch (IOException e) {
            log.error("Error reading file bytes", e);
            throw new IOException("Error processing image file", e);
        }

        log.debug("File validation passed: {} ({})", file.getOriginalFilename(), contentType);
    }

    /**
     * 🔧 FIXED: Validate UPI payment info
     */
    private void validateUpiPaymentInfo(UpiPaymentInfoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("UPI Payment Info cannot be null");
        }

        if (dto.getUpiId() == null || dto.getUpiId().trim().isEmpty()) {
            throw new IllegalArgumentException("UPI ID is required");
        }

        if (dto.getAccountHolderName() == null || dto.getAccountHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name is required");
        }

        if (dto.getAdminId() == null) {
            throw new IllegalArgumentException("Admin ID is required");
        }

        // Validate UPI ID format (basic validation)
        String upiId = dto.getUpiId().trim();
        if (!upiId.contains("@") || upiId.split("@").length != 2) {
            throw new IllegalArgumentException("Invalid UPI ID format. Expected format: username@bankcode");
        }

        log.debug("UPI Payment Info validation passed for UPI ID: {}", dto.getUpiId());
    }

    /**
     * 🔧 NEW: Get UPI settings for specific admin with security check
     */
    public List<UpiPaymentInfoDTO> findByAdminIdSecure(Long adminId, Long currentAdminId) {
        // Security check: ensure admin can only access their own records
        if (!adminId.equals(currentAdminId)) {
            throw new SecurityException("Access denied: Admin can only access their own UPI settings");
        }
        return findByAdminId(adminId);
    }

    /**
     * 🔧 NEW: Get active UPI for public use (for recharge page)
     */
    @Transactional(readOnly = true)
    public Optional<UpiPaymentInfoDTO> findActiveForPublic() {
        log.debug("Finding active UPI for public use");
        return repository.findTopByIsActiveTrueOrderByCreatedAtDesc()
                .map(mapper::toDTO);
    }

    /**
     * 🔧 NEW: Get UPI statistics for admin dashboard
     */
    @Transactional(readOnly = true)
    public UpiStatsDTO getUpiStats(Long adminId) {
        long totalUpiSettings = repository.countActiveByAdminId(adminId);
        long totalWithQrCode = repository.countActiveByAdminIdWithQrCode(adminId);
        
        return UpiStatsDTO.builder()
                .totalUpiSettings(totalUpiSettings)
                .settingsWithQrCode(totalWithQrCode)
                .settingsWithoutQrCode(totalUpiSettings - totalWithQrCode)
                .build();
    }

    // ==================== INNER CLASSES ====================

    /**
     * 🔧 NEW: DTO for UPI statistics
     */
    public static class UpiStatsDTO {
        private long totalUpiSettings;
        private long settingsWithQrCode;
        private long settingsWithoutQrCode;

        // Builder pattern
        public static UpiStatsDTOBuilder builder() {
            return new UpiStatsDTOBuilder();
        }

        public static class UpiStatsDTOBuilder {
            private long totalUpiSettings;
            private long settingsWithQrCode;
            private long settingsWithoutQrCode;

            public UpiStatsDTOBuilder totalUpiSettings(long totalUpiSettings) {
                this.totalUpiSettings = totalUpiSettings;
                return this;
            }

            public UpiStatsDTOBuilder settingsWithQrCode(long settingsWithQrCode) {
                this.settingsWithQrCode = settingsWithQrCode;
                return this;
            }

            public UpiStatsDTOBuilder settingsWithoutQrCode(long settingsWithoutQrCode) {
                this.settingsWithoutQrCode = settingsWithoutQrCode;
                return this;
            }

            public UpiStatsDTO build() {
                UpiStatsDTO dto = new UpiStatsDTO();
                dto.totalUpiSettings = this.totalUpiSettings;
                dto.settingsWithQrCode = this.settingsWithQrCode;
                dto.settingsWithoutQrCode = this.settingsWithoutQrCode;
                return dto;
            }
        }

        // Getters
        public long getTotalUpiSettings() { return totalUpiSettings; }
        public long getSettingsWithQrCode() { return settingsWithQrCode; }
        public long getSettingsWithoutQrCode() { return settingsWithoutQrCode; }
    }
}
