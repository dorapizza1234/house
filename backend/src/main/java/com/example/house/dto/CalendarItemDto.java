 package com.example.house.dto;

  import java.time.LocalDate;

  public record CalendarItemDto(
          Long id,
          String type,
          String title,
          LocalDate date,
          boolean isYearly,
          String memo,
          Long creatorId
  ) {}