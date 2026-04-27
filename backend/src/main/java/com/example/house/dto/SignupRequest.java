 package com.example.house.dto;

  import jakarta.validation.constraints.Email;
  import jakarta.validation.constraints.NotBlank;
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
      String nickname
  ) {}
