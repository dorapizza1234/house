 package com.example.house.dto;

import java.time.LocalDate;

public record SignupResponse(
      Long memberId,
      String email,
      String nickname
  ) {}