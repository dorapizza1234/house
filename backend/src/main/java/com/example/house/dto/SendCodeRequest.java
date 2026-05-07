package com.example.house.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendCodeRequest(
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    String phone,

    @NotBlank(message = "용도는 필수입니다")
    @Pattern(regexp = "^(SIGNUP|FIND_ID)$", message = "유효하지 않은 용도입니다")
    String purpose
) {}
