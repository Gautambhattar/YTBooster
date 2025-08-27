package com.ytbooster.service;

import com.ytbooster.model.User;
import com.ytbooster.model.dto.UserDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for User operations.
 * - Returns safe DTOs for API responses
 * - Supports concurrent wallet operations with BigDecimal
 */
public interface UserService {

    /** Save a new user */
    User save(UserDTO userDTO);

    /** Find user by email (safe DTO) */
    UserDTO findByEmail(String email);

    /** Find user by referral code (safe DTO) */
    UserDTO findByReferralCode(String referralCode);

    /** Find user by ID (safe DTO) */
    UserDTO findByUserId(Long userId);

    /** Get all users (internal use, returns entities) */
    List<User> findAll();

    /** Delete user by ID */
    void deleteById(Long id);

    /** Add funds safely (concurrency-safe, BigDecimal) */
    void addFunds(Long userId, BigDecimal amount);

    /** Deduct funds safely (concurrency-safe, BigDecimal) */
    void deductFunds(Long userId, BigDecimal amount);
    
    UserDTO update(UserDTO userDTO);

    // ======== NEW METHODS FOR USER LIST ========
    
    /** Get all users with pagination */
    Page<UserDTO> getAllUsers(Pageable pageable);
    
    /** Search users by name or email */
    Page<UserDTO> searchUsers(String search, Pageable pageable);
    
    /** Get users by role with pagination */
    Page<UserDTO> getUsersByRole(String role, Pageable pageable);
    
    /** Get users by status with pagination */
    Page<UserDTO> getUsersByStatus(String status, Pageable pageable);
    
    /** Get user by ID for viewing */
    Optional<UserDTO> getUserById(Long id);
    
    // Statistics methods
    long getTotalUserCount();
    long getOnlineUserCount();
    long getAuthenticatedUserCount();
    long getGuestUserCount();
    
    // User management methods
    boolean updateUserStatus(Long userId, String status);
    boolean updateAuthenticationStatus(Long userId, boolean authenticated);
    boolean updateOnlineStatus(Long userId, boolean online);
    boolean addToWallet(Long userId, BigDecimal amount);
    boolean deductFromWallet(Long userId, BigDecimal amount);
    boolean deleteUser(Long userId);
    
    boolean setUserOnline(String email, boolean online);
    boolean updateLastSeen(String email);
}
