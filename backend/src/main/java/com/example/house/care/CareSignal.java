package com.example.house.care;

public record CareSignal(
        Long familyId,          // 어느 가족의 신호인지 (발송 대상 범위)
        Long subjectMemberId,   // 신호의 대상자 (예: 엄마) — 수신자 타겟팅에 사용
        String subject,         // 관계호칭/닉네임 (예: "어머니") — LLM 프롬프트에 들어감
        SignalType type,        // 신호 종류
        String detail           // 규칙엔진이 만든 '사실 요약' 문자열
) {}
