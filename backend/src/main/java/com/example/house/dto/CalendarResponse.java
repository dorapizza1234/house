package com.example.house.dto;

  import java.util.List;

  public record CalendarResponse(
          int year,
          int month,
          List<CalendarItemDto> items
  ) {}