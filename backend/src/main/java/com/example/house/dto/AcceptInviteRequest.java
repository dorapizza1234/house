package com.example.house.dto;

  import jakarta.validation.constraints.NotBlank;

  public record AcceptInviteRequest(
      @NotBlank(message = "초대 토큰은 필수입니다")
      String token
  ) {}