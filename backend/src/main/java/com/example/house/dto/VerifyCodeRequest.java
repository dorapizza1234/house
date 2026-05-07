package com.example.house.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequest(
    @NotBlank
    @Pattern(regexp = "^01[0-9]{8,9}$")
    String phone,

    @NotBlank(message = "인증번호를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
    String code,

    @NotBlank
    @Pattern(regexp = "^(SIGNUP|FIND_ID)$")
    String purpose
) {}
