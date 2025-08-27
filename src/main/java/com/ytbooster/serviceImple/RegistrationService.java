package com.ytbooster.serviceImple;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ytbooster.model.User;
import com.ytbooster.model.dto.UserDTO;
import com.ytbooster.model.mapper.UserMapper;
import com.ytbooster.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Admin key for special registrations
    private static final String ADMIN_SECRET_KEY = "myAdminSecret123";

    /**
     * Register a new user.
     * - Encrypts password
     * - Assigns role based on adminKey
     * - Defaults wallet and referCode handled by entity
     */
    @Transactional
    public UserDTO register(UserDTO userDto, String adminKey) {
        if (userDto == null) {
            throw new IllegalArgumentException("UserDTO cannot be null");
        }

        // Encrypt password
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

        // Default status
        userDto.setStatus("ACTIVE");

        // Assign role
        if (ADMIN_SECRET_KEY.equals(adminKey)) {
            userDto.setRole("ADMIN");
        } else {
            userDto.setRole("USER");
        }

        // Map DTO → Entity
        User user = UserMapper.toEntity(userDto);

        // Save (concurrency-safe thanks to DB constraints & entity defaults)
        user = userRepository.save(user);

        // Return safe DTO (password excluded)
        return UserMapper.toSafeDTO(user);
    }
}
