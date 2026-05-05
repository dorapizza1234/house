package com.example.house.dto;

  import java.time.LocalDate;
  import java.time.LocalDateTime;

  public record EventResponse(
          Long id,
          String type,
          String title,
          LocalDate eventDate,
          boolean isYearly,
          String memo,
          Long creatorId,
          LocalDateTime createdAt
  ) {}