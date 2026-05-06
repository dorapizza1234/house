package com.example.house.dto;

  import java.time.LocalDateTime;
  import java.util.List;

  public record PlantResponse(
      Long id,
      Long familyId,
      String name,
      String state,                       // ALIVE / WILTED / DEAD
      Long plantedByMemberId,
      String plantedByNickname,
      LocalDateTime plantedAt,
      LocalDateTime lastWateredAt,
      LocalDateTime stateChangedAt,
      Integer happiness,                  // Phase 2 (값 0)
      List<MemberContributionDto> contributions    // 멤버별 물주기 카운트
  ) {}