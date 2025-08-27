package com.ytbooster.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ytbooster.model.User;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    User findByReferCode(String referralCode);

    /** 
     * Pessimistic lock for safe concurrent wallet updates
     * Example usage: transfer funds between two users
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    User findByIdForUpdate(@Param("userId") Long userId);

    /**
     * Optional JPQL update if you want direct wallet set (less safe under concurrency)
     * - Use BigDecimal (matches entity)
     * - Must be inside @Transactional
     */
    @Transactional
    @Query("UPDATE User u SET u.wallet = :wallet WHERE u.id = :userId")
    void updateWallet(@Param("userId") Long userId, @Param("wallet") BigDecimal wallet);

    /** 
     * Convenience method to increment wallet safely using repository + service transaction
     */
    @Transactional
    default void incrementWallet(Long userId, BigDecimal amount) {
        User user = findByIdForUpdate(userId);
        user.setWallet(user.getWallet().add(amount));
        save(user); // JPA flush will commit
    }

    @Transactional
    default void decrementWallet(Long userId, BigDecimal amount) {
        User user = findByIdForUpdate(userId);
        if (user.getWallet().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance");
        }
        user.setWallet(user.getWallet().subtract(amount));
        save(user);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    User findByEmailForUpdate(@Param("email") String email);

    // ========== USER LIST FUNCTIONALITY ==========
    
    // Pagination support
    Page<User> findAll(Pageable pageable);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByStatus(String status, Pageable pageable);

    // Search functionality
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchByNameOrEmail(@Param("search") String search, Pageable pageable);

    // Basic filtering
    List<User> findByRole(String role);
    List<User> findByStatus(String status);
    List<User> findByAuthenticated(boolean authenticated);
    List<User> findByOnline(boolean online);

    // Statistics for dashboard
    @Query("SELECT COUNT(u) FROM User u WHERE u.authenticated = true")
    long countAuthenticatedUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.online = true")
    long countOnlineUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    // Update operations for user management
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.online = :online, u.lastSeen = :lastSeen WHERE u.id = :userId")
    int updateOnlineStatus(@Param("userId") Long userId, @Param("online") boolean online, 
                          @Param("lastSeen") LocalDateTime lastSeen);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.authenticated = :authenticated WHERE u.id = :userId")
    int updateAuthenticationStatus(@Param("userId") Long userId, @Param("authenticated") boolean authenticated);
    
    
}
