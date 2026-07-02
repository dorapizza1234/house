package com.example.house.care;

public enum SignalType {
    LATE_RETURN,       // 평소보다 크게 늦은 귀가
    NO_OUTING_3DAYS,   // 며칠 연속 외출 없음
    HOSPITAL_TODAY,    // 오늘 병원 일정
    BIRTHDAY           // 오늘 생일
}