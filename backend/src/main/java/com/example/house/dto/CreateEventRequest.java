 package com.example.house.dto;

  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.NotNull;
                                                                                                                          import java.time.LocalDate;
                                                                                                                          public record CreateEventRequest(
          @NotBlank(message = "이벤트 타입은 필수입니다")
          String type,

          @NotBlank(message = "제목은 필수입니다")
          String title,

          @NotNull(message = "이벤트 날짜는 필수입니다")
          LocalDate eventDate,

          boolean isYearly,

          String memo
  ) {}
