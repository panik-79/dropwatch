package com.dropwatch.api.dto;

import java.io.Serializable;
import java.time.Instant;

public class AuthDto {

    public record LoginRequest(
            String email,
            String password
    ) implements Serializable {}

    public record RegisterRequest(
            String email,
            String password,
            String fullName
    ) implements Serializable {}

    public record AuthResponse(
            String token,
            UserDto user
    ) implements Serializable {}

    public record UserDto(
            String id,
            String email,
            String fullName,
            String role,
            Instant createdAt
    ) implements Serializable {}
}
