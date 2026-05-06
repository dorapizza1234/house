 package com.example.house.dto;

  import java.time.LocalDateTime;

  public record MessageResponse(
      Long id,
      Long senderId,
      String senderNickname,
      Long receiverId,
      String receiverNickname,
      Long messageTypeId,
      String messageTypeName,     // null = 자유입력
      String content,
      Integer finalPoints,
      Integer multiplier,
      String eventReasons,
      LocalDateTime createdAt
  ) {}