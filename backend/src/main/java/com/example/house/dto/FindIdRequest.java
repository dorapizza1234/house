package com.example.house.dto;

import jakarta.validation.constraints.NotBlank;

public record FindIdRequest(
    @NotBlank(message = "인증 토큰이 필요합니다")
    String verificationToken
) {}
