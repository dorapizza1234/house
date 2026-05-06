package com.example.house.dto;

  import java.time.LocalDateTime;

  public record CheckInLogResponse(
          Long id,
          Long memberId,
          String memberNickname,
          String status,
          LocalDateTime checkedAt                                                                                         
          ) {}