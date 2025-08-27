package com.ytbooster.model.mapper;

import com.ytbooster.model.User;
import com.ytbooster.model.dto.UserDTO;
import lombok.experimental.UtilityClass;

/**
 * Mapper for converting between User entity and UserDTO.
 * - Converts Entity ↔ DTO
 * - Provides a safe mapper (without password) for API responses
 */
@UtilityClass
public class UserMapper {

    /** Convert Entity → DTO (includes password, use internally only) */
    public static UserDTO toDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword()) // ⚠️ avoid sending this in responses
                .referCode(user.getReferCode())
                .wallet(user.getWallet())
                .role(user.getRole())
                .status(user.getStatus())
                .authenticated(user.isAuthenticated())
                .online(user.isOnline())
                .lastSeen(user.getLastSeen())
                .build();
    }

    /** Convert Entity → DTO (safe for API response, excludes password) */
    public static UserDTO toSafeDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .referCode(user.getReferCode())
                .wallet(user.getWallet())
                .role(user.getRole())
                .status(user.getStatus())
                .authenticated(user.isAuthenticated())
                .online(user.isOnline())
                .lastSeen(user.getLastSeen())
                .build();
    }

    /** Convert DTO → Entity */
    public static User toEntity(UserDTO dto) {
        if (dto == null) return null;

        return User.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .referCode(dto.getReferCode())
                .wallet(dto.getWallet())
                .role(dto.getRole())
                .status(dto.getStatus())
                .authenticated(dto.isAuthenticated())
                .online(dto.isOnline())
                .lastSeen(dto.getLastSeen())
                .build();
    }
}
