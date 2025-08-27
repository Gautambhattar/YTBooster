package com.ytbooster.serviceImple;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ytbooster.model.User;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.model.mapper.UserMapper;
import com.ytbooster.repository.UserRepository;
import com.ytbooster.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /** Save a new user */
    @Override
    @Transactional
    public User save(UserDTO userDTO) {
        User user = UserMapper.toEntity(userDTO);
        // default wallet if null
        if (user.getWallet() == null) user.setWallet(BigDecimal.ZERO);
        return userRepository.save(user);
    }

    /** Find user by email (safe DTO) */
    @Override
    public UserDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return UserMapper.toSafeDTO(user);
    }

    /** Find user by ID (safe DTO) */
    @Override
    public UserDTO findByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return UserMapper.toSafeDTO(user);
    }

    /** Find by referral code (safe DTO) */
    @Override
    public UserDTO findByReferralCode(String referralCode) {
        User user = userRepository.findByReferCode(referralCode);
        if (user == null) throw new IllegalArgumentException("User not found with referral code: " + referralCode);
        return UserMapper.toSafeDTO(user);
    }

    /** Return all users (internal use; returns entities) */
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** Delete by ID */
    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    /** Add funds safely using pessimistic lock */
    @Override
    @Transactional
    public void addFunds(Long userId, BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        userRepository.incrementWallet(userId, amount);
    }

    /** Deduct funds safely using pessimistic lock */
    @Override
    @Transactional
    public void deductFunds(Long userId, BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        userRepository.decrementWallet(userId, amount);
    }
    
    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    public UserDTO update(UserDTO userDTO) {
        try {
            // Find existing user
            User user = userRepository.findById(userDTO.getId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userDTO.getId()));

            // Update fields
            user.setName(userDTO.getName());
            user.setEmail(userDTO.getEmail());
            user.setWallet(userDTO.getWallet());
            // Add other fields as needed

            // Save updated user
            user = userRepository.save(user);
            
            // Convert back to DTO and return
            return UserMapper.toDTO(user);
            
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure updating user: {} - will retry", userDTO.getId());
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage());
        }
    }

    @Recover
    public UserDTO recoverUpdate(ObjectOptimisticLockingFailureException e, UserDTO userDTO) {
        log.error("Failed to update user {} after all retry attempts", userDTO.getId());
        throw new RuntimeException("Unable to update user due to concurrent modifications");
    }

    // ======== USER LIST IMPLEMENTATIONS ========

    @Override
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserMapper::toSafeDTO);
    }

    @Override
    public Page<UserDTO> searchUsers(String search, Pageable pageable) {
        return userRepository.searchByNameOrEmail(search, pageable)
                .map(UserMapper::toSafeDTO);
    }

    @Override
    public Page<UserDTO> getUsersByRole(String role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(UserMapper::toSafeDTO);
    }

    @Override
    public Page<UserDTO> getUsersByStatus(String status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable)
                .map(UserMapper::toSafeDTO);
    }

    @Override
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toSafeDTO);
    }

    // Statistics implementations
    @Override
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Override
    public long getOnlineUserCount() {
        return userRepository.countOnlineUsers();
    }

    @Override
    public long getAuthenticatedUserCount() {
        return userRepository.countAuthenticatedUsers();
    }

    @Override
    public long getGuestUserCount() {
        return getTotalUserCount() - getAuthenticatedUserCount();
    }

    // ======== USER MANAGEMENT WITH RETRY LOGIC ========

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 100)
    )
    public boolean updateUserStatus(Long userId, String status) {
        try {
            return userRepository.updateUserStatus(userId, status) > 0;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure updating status for user: {} - will retry", userId);
            throw e;
        }
    }

    @Recover
    public boolean recoverUpdateUserStatus(ObjectOptimisticLockingFailureException e, Long userId, String status) {
        log.error("Failed to update status for user {} after all retry attempts", userId);
        return false;
    }

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 100)
    )
    public boolean updateAuthenticationStatus(Long userId, boolean authenticated) {
        try {
            return userRepository.updateAuthenticationStatus(userId, authenticated) > 0;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure updating auth status for user: {} - will retry", userId);
            throw e;
        }
    }

    @Recover
    public boolean recoverUpdateAuthenticationStatus(ObjectOptimisticLockingFailureException e, Long userId, boolean authenticated) {
        log.error("Failed to update auth status for user {} after all retry attempts", userId);
        return false;
    }

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 100)
    )
    public boolean updateOnlineStatus(Long userId, boolean online) {
        try {
            return userRepository.updateOnlineStatus(userId, online, LocalDateTime.now()) > 0;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure updating online status for user: {} - will retry", userId);
            throw e;
        }
    }

    @Recover
    public boolean recoverUpdateOnlineStatus(ObjectOptimisticLockingFailureException e, Long userId, boolean online) {
        log.error("Failed to update online status for user {} after all retry attempts", userId);
        return false;
    }

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 150)
    )
    public boolean addToWallet(Long userId, BigDecimal amount) {
        try {
            userRepository.incrementWallet(userId, amount);
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure adding to wallet for user: {} - will retry", userId);
            throw e;
        } catch (Exception e) {
            log.error("Error adding to wallet for user: {}", userId, e);
            return false;
        }
    }

    @Recover
    public boolean recoverAddToWallet(ObjectOptimisticLockingFailureException e, Long userId, BigDecimal amount) {
        log.error("Failed to add to wallet for user {} after all retry attempts", userId);
        return false;
    }

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 150)
    )
    public boolean deductFromWallet(Long userId, BigDecimal amount) {
        try {
            userRepository.decrementWallet(userId, amount);
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure deducting from wallet for user: {} - will retry", userId);
            throw e;
        } catch (Exception e) {
            log.error("Error deducting from wallet for user: {}", userId, e);
            return false;
        }
    }

    @Recover
    public boolean recoverDeductFromWallet(ObjectOptimisticLockingFailureException e, Long userId, BigDecimal amount) {
        log.error("Failed to deduct from wallet for user {} after all retry attempts", userId);
        return false;
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }
    
    // ======== HEARTBEAT & ONLINE STATUS WITH RETRY ========

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    public boolean setUserOnline(String email, boolean online) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setOnline(online);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);
                log.debug("Successfully set user {} online status: {}", email, online);
                return true;
            }
            log.warn("User not found for email: {}", email);
            return false;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure setting online status for user: {} - will retry", email);
            throw e;
        } catch (Exception e) {
            log.error("Error setting user online status: {}", e.getMessage());
            return false;
        }
    }

    @Recover
    public boolean recoverSetUserOnline(ObjectOptimisticLockingFailureException e, String email, boolean online) {
        log.warn("Failed to set online status for user {} after all retry attempts", email);
        return false; // For heartbeat, it's OK to fail occasionally
    }

    @Override
    @Transactional
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    public boolean updateLastSeen(String email) {
        try {
            // Re-fetch user to get latest version
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
            
            user.setLastSeen(LocalDateTime.now());
            user.setOnline(true);
            userRepository.save(user);
            
            log.debug("Successfully updated lastSeen for user: {}", email);
            return true;
            
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure updating lastSeen for user: {} - will retry", email);
            throw e; // Re-throw to trigger retry
        }
    }
    
    @Recover
    public boolean recoverUpdateLastSeen(ObjectOptimisticLockingFailureException e, String email) {
        log.warn("Failed to update lastSeen for user {} after all retry attempts", email);
        return false; // For heartbeat, it's OK to fail occasionally
    }
}