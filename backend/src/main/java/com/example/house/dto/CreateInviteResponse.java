package com.example.house.dto;

  import java.time.LocalDateTime;

  public record CreateInviteResponse(
      String token,
      LocalDateTime expiresAt
  ) {}
