  package com.example.house.dto;

  public record MemberContributionDto(
      Long memberId,
      String nickname,
      long wateringCount
  ) {}