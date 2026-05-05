 package com.example.house.dto;

  import java.time.LocalDateTime;

  public record PresenceResponse(
          String status,
          LocalDateTime updatedAt
  ) {}