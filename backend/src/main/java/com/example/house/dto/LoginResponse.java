  package com.example.house.dto;

  public record LoginResponse(
      String accessToken,
      String refreshToken,
      Long familyId
  ) {}
