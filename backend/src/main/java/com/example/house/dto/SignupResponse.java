 package com.example.house.dto;

  public record SignupResponse(
      Long memberId,
      String email,
      String nickname
  ) {}