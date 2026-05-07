package com.example.house.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "비밀번호는 영문 대문자, 소문자, 숫자를 각각 1개 이상 포함해야 합니다"
    )
    String password,

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
    String nickname,

    @NotNull(message = "생년월일은 필수입니다")
    LocalDate birthDate,

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    String phone,

    @NotBlank(message = "휴대폰 인증이 필요합니다")
    String phoneVerificationToken
) {}
