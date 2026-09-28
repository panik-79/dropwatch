package com.dropwatch.api.service;

import com.dropwatch.api.dto.AuthDto;
import com.dropwatch.api.security.JwtProvider;
import com.dropwatch.core.domain.User;
import com.dropwatch.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("User with email " + req.email() + " already exists");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.fullName() != null ? req.fullName() : req.email())
                .roles(Set.of("ROLE_USER"))
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);

        String role = saved.getRoles() != null && !saved.getRoles().isEmpty() ? saved.getRoles().iterator().next() : "USER";
        String token = jwtProvider.generateToken(saved.getId(), saved.getEmail(), role);
        AuthDto.UserDto userDto = new AuthDto.UserDto(saved.getId(), saved.getEmail(), saved.getDisplayName(), role, saved.getCreatedAt());

        return new AuthDto.AuthResponse(token, userDto);
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String role = user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().iterator().next() : "USER";
        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), role);
        AuthDto.UserDto userDto = new AuthDto.UserDto(user.getId(), user.getEmail(), user.getDisplayName(), role, user.getCreatedAt());

        return new AuthDto.AuthResponse(token, userDto);
    }

    public AuthDto.UserDto getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String role = user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().iterator().next() : "USER";
        return new AuthDto.UserDto(user.getId(), user.getEmail(), user.getDisplayName(), role, user.getCreatedAt());
    }
}
