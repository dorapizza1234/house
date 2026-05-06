 package com.example.house.dto;

  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.NotNull;
  import jakarta.validation.constraints.Size;

  public record SendMessageRequest(
      @NotNull(message = "받는 사람은 필수입니다")
      Long receiverId,

      Long messageTypeId,                 // null = 자유입력

      @NotBlank(message = "메시지 내용은 필수입니다")
      @Size(max = 500, message = "메시지는 500자 이하여야 합니다")
      String content
  ) {}

