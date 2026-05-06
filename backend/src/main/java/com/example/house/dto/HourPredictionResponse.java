  package com.example.house.dto;

  public record HourPredictionResponse(
      Double averageHour,
      int sampleCount,
      int days
  ) {}
