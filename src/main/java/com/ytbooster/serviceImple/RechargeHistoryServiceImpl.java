package com.ytbooster.serviceImple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.ytbooster.model.RechargeHistory;
import com.ytbooster.model.ReferHistory;
import com.ytbooster.model.dto.RechargeDTO;
import com.ytbooster.model.dto.RechargeHistoryDTO;
import com.ytbooster.model.dto.UpiPaymentInfoDTO;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.model.mapper.RechargeHistoryMapper;
import com.ytbooster.repository.RechargeHistoryRepository;
import com.ytbooster.repository.ReferHistoryRepository;
import com.ytbooster.service.RechargeHistoryService;

import jakarta.transaction.Transactional;

@Service
public class RechargeHistoryServiceImpl implements RechargeHistoryService {

    private static final Logger log = LoggerFactory.getLogger(RechargeHistoryServiceImpl.class);

    @Autowired
    private RechargeHistoryRepository rechargeHistoryRepository;

    @Autowired 
    private ReferHistoryRepository referHistoryRepository;

    @Autowired
    private UserWallet userWallet;

    @Autowired
    private UserServiceImpl userService;

    // 🔧 CRITICAL FIX: Add missing dependency injection
    @Autowired
    private UpiPaymentInfoServiceImpl upiPaymentInfoService;

    @Override
    @Transactional
    public void save(RechargeHistoryDTO rechargeHistory) {
        // Set initial status
        rechargeHistory.setStatus("PENDING");

        // Save recharge history
        RechargeHistory saved = rechargeHistoryRepository
                .save(RechargeHistoryMapper.toEntity(rechargeHistory));

        // Credit wallet if payment is successful
        if ("SUCCESS".equalsIgnoreCase(saved.getPaymentStatus())) {
            userWallet.walletRecharge(saved.getAmount(), saved.getEmail());
            // Update status to COMPLETE when payment is successful
            saved.setStatus("COMPLETE");
            rechargeHistoryRepository.save(saved);
        }

        // Only process referral rewards when recharge is COMPLETE
        if ("COMPLETE".equalsIgnoreCase(saved.getStatus())) {
            processReferralReward(saved);
        }
    }

    /**
     * 🔧 FIXED: Separate method to handle referral reward processing
     */
    private void processReferralReward(RechargeHistory recharge) {
        try {
            ReferHistory history = referHistoryRepository.findByReferredUserId(recharge.getUserId());
            if (history != null && !history.isRewarded()) {
                
                // Update total recharge for referred user
                BigDecimal totalRecharge = rechargeHistoryRepository
                        .sumRechargeByUserId(history.getReferredUserId());
                if (totalRecharge == null) totalRecharge = BigDecimal.ZERO;
                
                history.setTotalRecharge(totalRecharge);
                history.setLastUpdate(LocalDate.now());

                log.info("📊 Referral update - Referrer: {}, Referred: {}, Total: ₹{}", 
                        history.getUserId(), history.getReferredUserId(), totalRecharge);

                // Check if referred user meets minimum requirement
                if (totalRecharge.compareTo(new BigDecimal("50")) >= 0) {
                    
                    // Check if REFERRER has minimum recharge
                    BigDecimal referrerMaxRecharge = rechargeHistoryRepository
                            .maxCompletedRechargeByUserId(history.getUserId());
                    
                    if (referrerMaxRecharge != null && 
                        referrerMaxRecharge.compareTo(new BigDecimal("20")) >= 0) {
                        
                        BigDecimal rewardAmount = new BigDecimal("20");
                        history.setAmount(rewardAmount);
                        history.setRewarded(true);
                        history.setStatus("CREDITED");

                        // Credit reward to REFERRER (User A), NOT referred user (User B)
                        UserDTO referrer = userService.findByUserId(history.getUserId()); // User A
                        if (referrer != null && referrer.getEmail() != null) {
                            userWallet.walletRecharge(rewardAmount, referrer.getEmail());
                            log.info("✅ Successfully credited ₹{} to REFERRER {} ({})", 
                                    rewardAmount, history.getUserId(), referrer.getEmail());
                        } else {
                            log.error("❌ Referrer user {} not found or invalid email", history.getUserId());
                            history.setStatus("REFERRER_NOT_FOUND");
                            history.setRewarded(false);
                        }
                    } else {
                        log.debug("🚫 Referrer {} insufficient recharge: {} < 20", 
                                 history.getUserId(), referrerMaxRecharge);
                        history.setStatus("REFERRER_INSUFFICIENT_RECHARGE");
                    }
                } else {
                    log.debug("🚫 Referred user {} insufficient recharge: {} < 50", 
                             history.getReferredUserId(), totalRecharge);
                    history.setStatus("REFERRED_INSUFFICIENT_RECHARGE");
                }

                referHistoryRepository.save(history);
            }
        } catch (Exception e) {
            log.error("❌ Failed to process referral reward for recharge {}: {}", 
                     recharge.getUserId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void saveFromDTO(RechargeDTO dto) {
        RechargeHistory entity = RechargeHistoryMapper.toEntityRecharge(dto);
        rechargeHistoryRepository.save(entity);

        if ("SUCCESS".equalsIgnoreCase(dto.getPaymentStatus())) {
            userWallet.walletRecharge(dto.getAmount(), dto.getEmail());
            // Update status to COMPLETE
            entity.setStatus("COMPLETE");
            rechargeHistoryRepository.save(entity);
            
            // Process referral reward
            processReferralReward(entity);
        }
    }

    @Override
    public List<RechargeHistoryDTO> findByUserId(Long userId) {
        return rechargeHistoryRepository.findByUserId(userId)
                .stream()
                .map(RechargeHistoryMapper::toDTO)
                .toList();
    }

    @Override
    public List<RechargeHistoryDTO> findByStatus(String status) {
        return rechargeHistoryRepository.findByStatus(status)
                .stream()
                .map(RechargeHistoryMapper::toDTO)
                .toList();
    }

    @Override
    public List<RechargeHistoryDTO> findByUserIdAndStatus(Long userId, String status) {
        return rechargeHistoryRepository.findByUserIdAndStatus(userId, status)
                .stream()
                .map(RechargeHistoryMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public RechargeHistoryDTO updateStatus(Long utr, String status) {
        RechargeHistory entity = rechargeHistoryRepository.findByUtr(utr);
        if (entity == null) throw new IllegalArgumentException("UTR not found: " + utr);

        String oldStatus = entity.getStatus();
        entity.setStatus(status.toUpperCase());
        RechargeHistory saved = rechargeHistoryRepository.save(entity);
        
        // Process referral reward when status changes to COMPLETE
        if ("COMPLETE".equalsIgnoreCase(status) && !"COMPLETE".equalsIgnoreCase(oldStatus)) {
            processReferralReward(saved);
        }
        
        return RechargeHistoryMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Long utr, String paymentStatus) {
        RechargeHistory entity = rechargeHistoryRepository.findByUtr(utr);
        if (entity == null) throw new IllegalArgumentException("UTR not found: " + utr);

        entity.setPaymentStatus(paymentStatus.toUpperCase());
        
        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            userWallet.walletRecharge(entity.getAmount(), entity.getEmail());
            entity.setStatus("COMPLETE");
            rechargeHistoryRepository.save(entity);
            
            // Process referral reward
            processReferralReward(entity);
        } else {
            rechargeHistoryRepository.save(entity);
        }
    }

    @Override
    public RechargeHistoryDTO findByUtr(Long utr) {
        return RechargeHistoryMapper.toDTO(rechargeHistoryRepository.findByUtr(utr));
    }

    /**
     * 🔧 FIXED: Dynamic UPI integration with proper error handling
     */
    @Override
    public void loadUpiInfo(Model model) {
        try {
            log.debug("Loading UPI info for recharge form");
            
            // Get the latest active UPI setting from admin configuration
            Optional<UpiPaymentInfoDTO> latestUpi = upiPaymentInfoService.findActiveForPublic();
            
            if (latestUpi.isPresent()) {
                UpiPaymentInfoDTO upi = latestUpi.get();
                
                // Add UPI details to model
                model.addAttribute("upiId", upi.getUpiId());
                model.addAttribute("accountHolderName", upi.getAccountHolderName());
                model.addAttribute("paymentNote", upi.getNote());
                
                // Add QR code if available
                if (upi.getQrBase64() != null && !upi.getQrBase64().trim().isEmpty()) {
                    model.addAttribute("qrCodeBase64", upi.getQrBase64());
                    log.debug("QR code loaded for UPI: {}", upi.getUpiId());
                } else {
                    log.debug("No QR code available for UPI: {}", upi.getUpiId());
                }
                
                log.info("✅ Successfully loaded UPI info: {} ({})", 
                        upi.getUpiId(), upi.getAccountHolderName());
                
            } else {
                log.warn("⚠️ No active UPI payment settings found, using fallback values");
                
                // Fallback to default values
                model.addAttribute("upiId", "admin@setup");
                model.addAttribute("accountHolderName", "Payment Setup Required");
                model.addAttribute("paymentNote", "Please contact admin to configure UPI payments");
                model.addAttribute("upiConfigRequired", true);
            }
            
        } catch (Exception e) {
            log.error("❌ Error loading UPI info: {}", e.getMessage(), e);
            
            // Set error fallback values
            model.addAttribute("upiId", "error@loading");
            model.addAttribute("accountHolderName", "System Error");
            model.addAttribute("paymentNote", "Unable to load payment information. Please try again later.");
            model.addAttribute("upiError", true);
        }
    }

    @Override
    public List<RechargeHistoryDTO> findAll() {
        return rechargeHistoryRepository.findAll()
                .stream()
                .map(RechargeHistoryMapper::toDTO)
                .toList();
    }

    /**
     * 🔧 NEW: Get recharge statistics for admin dashboard
     */
    public RechargeStatsDTO getRechargeStats() {
        try {
            long totalRecharges = rechargeHistoryRepository.count();
            long completedRecharges = rechargeHistoryRepository.findByStatus("COMPLETE").size();
            long pendingRecharges = rechargeHistoryRepository.findByStatus("PENDING").size();
            long failedRecharges = rechargeHistoryRepository.findByStatus("FAILED").size();
            
            // Calculate total amount for completed recharges
            BigDecimal totalAmount = rechargeHistoryRepository.findByStatus("COMPLETE")
                    .stream()
                    .map(RechargeHistory::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return RechargeStatsDTO.builder()
                    .totalRecharges(totalRecharges)
                    .completedRecharges(completedRecharges)
                    .pendingRecharges(pendingRecharges)
                    .failedRecharges(failedRecharges)
                    .totalAmount(totalAmount)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting recharge stats: {}", e.getMessage(), e);
            return RechargeStatsDTO.builder()
                    .totalRecharges(0)
                    .completedRecharges(0)
                    .pendingRecharges(0)
                    .failedRecharges(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }
    }

    /**
     * 🔧 NEW: DTO for recharge statistics
     */
    public static class RechargeStatsDTO {
        private long totalRecharges;
        private long completedRecharges;
        private long pendingRecharges;
        private long failedRecharges;
        private BigDecimal totalAmount;

        public static RechargeStatsDTOBuilder builder() {
            return new RechargeStatsDTOBuilder();
        }

        public static class RechargeStatsDTOBuilder {
            private long totalRecharges;
            private long completedRecharges;
            private long pendingRecharges;
            private long failedRecharges;
            private BigDecimal totalAmount;

            public RechargeStatsDTOBuilder totalRecharges(long totalRecharges) {
                this.totalRecharges = totalRecharges;
                return this;
            }

            public RechargeStatsDTOBuilder completedRecharges(long completedRecharges) {
                this.completedRecharges = completedRecharges;
                return this;
            }

            public RechargeStatsDTOBuilder pendingRecharges(long pendingRecharges) {
                this.pendingRecharges = pendingRecharges;
                return this;
            }

            public RechargeStatsDTOBuilder failedRecharges(long failedRecharges) {
                this.failedRecharges = failedRecharges;
                return this;
            }

            public RechargeStatsDTOBuilder totalAmount(BigDecimal totalAmount) {
                this.totalAmount = totalAmount;
                return this;
            }

            public RechargeStatsDTO build() {
                RechargeStatsDTO dto = new RechargeStatsDTO();
                dto.totalRecharges = this.totalRecharges;
                dto.completedRecharges = this.completedRecharges;
                dto.pendingRecharges = this.pendingRecharges;
                dto.failedRecharges = this.failedRecharges;
                dto.totalAmount = this.totalAmount;
                return dto;
            }
        }

        // Getters
        public long getTotalRecharges() { return totalRecharges; }
        public long getCompletedRecharges() { return completedRecharges; }
        public long getPendingRecharges() { return pendingRecharges; }
        public long getFailedRecharges() { return failedRecharges; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}
