package com.example.house.service;                                                                                    
  import com.example.house.domain.CheckInLog;                                                                             import com.example.house.dto.HourPredictionResponse;
  import com.example.house.dto.TodayComparisonResponse;
  import com.example.house.repository.CheckInLogRepository;
  import com.example.house.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.util.List;
  import java.util.OptionalDouble;
  import java.util.OptionalInt;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class PredictionService {

      private static final int MAX_DAYS = 30;

      private final CheckInLogRepository checkInLogRepository;
      private final MemberRepository memberRepository;

      public HourPredictionResponse getAverageLeaveTime(Long memberId, int days) {
          validateDays(days);

          if (!memberRepository.existsById(memberId)) {
              throw new IllegalArgumentException("회원을 찾을 수 없습니다");
          }

          LocalDateTime after = LocalDateTime.now().minusDays(days);
          List<CheckInLog> logs = checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(memberId, after);

          List<Integer> outHours = logs.stream()
                  .filter(log -> "OUTSIDE".equals(log.getStatus()))
                  .map(log -> log.getCheckedAt().getHour())
                  .toList();

          double average = outHours.stream()
                  .mapToInt(Integer::intValue)
                  .average()
                  .orElse(0.0);

          return new HourPredictionResponse(average, outHours.size(), days);
      }

      public HourPredictionResponse getAverageReturnTime(Long memberId, int days) {
          validateDays(days);

          if (!memberRepository.existsById(memberId)) {
              throw new IllegalArgumentException("회원을 찾을 수 없습니다");
          }

          LocalDateTime after = LocalDateTime.now().minusDays(days);
          List<CheckInLog> logs = checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(memberId, after);

          List<Integer> homeHours = logs.stream()
                  .filter(log -> "HOME".equals(log.getStatus()))
                  .map(log -> log.getCheckedAt().getHour())
                  .toList();

          double average = homeHours.stream()
                  .mapToInt(Integer::intValue)
                  .average()
                  .orElse(0.0);

          return new HourPredictionResponse(average, homeHours.size(), days);
      }

      public TodayComparisonResponse getTodayReturnComparison(Long memberId, int days) {
          validateDays(days);

          if (!memberRepository.existsById(memberId)) {
              throw new IllegalArgumentException("회원을 찾을 수 없습니다");
          }

          LocalDateTime after = LocalDateTime.now().minusDays(days);
          List<CheckInLog> logs = checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(memberId, after);

          LocalDate today = LocalDate.now();

          OptionalInt todayMinutesOpt = logs.stream()
                  .filter(log -> "HOME".equals(log.getStatus()))
                  .filter(log -> log.getCheckedAt().toLocalDate().equals(today))
                  .mapToInt(this::toMinutes)
                  .min();

          OptionalDouble pastAverageOpt = logs.stream()
                  .filter(log -> "HOME".equals(log.getStatus()))
                  .filter(log -> log.getCheckedAt().toLocalDate().isBefore(today))
                  .mapToInt(this::toMinutes)
                  .average();

          boolean hasComparison = todayMinutesOpt.isPresent() && pastAverageOpt.isPresent();

          Double todayReturnHour = todayMinutesOpt.isPresent()
                  ? todayMinutesOpt.getAsInt() / 60.0
                  : null;

          Double averageReturnHour = pastAverageOpt.isPresent()
                  ? pastAverageOpt.getAsDouble() / 60.0
                  : 0.0;

          Integer diffMinutes = null;
          boolean isLate = false;

          if (hasComparison) {
              diffMinutes = todayMinutesOpt.getAsInt() - (int) Math.round(pastAverageOpt.getAsDouble());
              isLate = diffMinutes >= 30;
          }

          return new TodayComparisonResponse(
                  todayReturnHour, averageReturnHour, diffMinutes, isLate, hasComparison, days);
      }

      private int toMinutes(CheckInLog log) {
          return log.getCheckedAt().getHour() * 60 + log.getCheckedAt().getMinute();
      }

      private void validateDays(int days) {
          if (days < 1 || days > MAX_DAYS) {
              throw new IllegalArgumentException("days는 1~" + MAX_DAYS + " 사이여야 합니다");
          }
      }
  }
