 package com.example.house.dto;

  public record TodayComparisonResponse(
      Double todayReturnHour,    // 오늘 첫 HOME 시각 (예: 11.25 = 11:15). 없으면 null
      Double averageReturnHour,  // 과거 N일 평균. 데이터 없으면 0.0
      Integer diffMinutes,       // 오늘 - 평균. 양수=늦음. 비교 불가면 null
      boolean isLate,            // 30분 이상 늦었는지
      boolean hasComparison,     // 비교 가능한 데이터가 있는지
      int days                   // 평균 계산 윈도우
  ) {}
