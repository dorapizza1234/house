 package com.example.house.dto;

  public record SendMessageResponse(
      Long messageId,
      Integer finalPoints,
      Integer multiplier,
      String eventReasons,        // "어버이날,엄마생신" — 없으면 null
      int todayPresetCount        // 오늘 프리셋 몇 개 보냈는지 (3회 한도 확인용)
  ) {}