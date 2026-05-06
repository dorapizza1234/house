package com.example.house.service;                                                                                    
  import com.example.house.domain.CheckInLog;
  import com.example.house.dto.HourPredictionResponse;
import com.example.house.dto.TodayComparisonResponse;
import com.example.house.repository.CheckInLogRepository;
  import com.example.house.repository.MemberRepository;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
  import java.util.List;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.assertj.core.api.Assertions.within;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.BDDMockito.given;

  @ExtendWith(MockitoExtension.class)
  class PredictionServiceTest {

      @Mock CheckInLogRepository checkInLogRepository;
      @Mock MemberRepository memberRepository;

      @InjectMocks PredictionService predictionService;

      @Test
      void getAverageLeaveTime_성공() {
          // given: OUTSIDE 3건(8시,9시,10시) + HOME 2건 → 평균 9.0
          Long memberId = 1L;
          int days = 7;

          given(memberRepository.existsById(memberId)).willReturn(true);

          CheckInLog out1 = makeLog(memberId, "OUTSIDE", LocalDateTime.now().withHour(8));
          CheckInLog out2 = makeLog(memberId, "OUTSIDE", LocalDateTime.now().withHour(9));
          CheckInLog out3 = makeLog(memberId, "OUTSIDE", LocalDateTime.now().withHour(10));
          CheckInLog home1 = makeLog(memberId, "HOME", LocalDateTime.now().withHour(20));
          CheckInLog home2 = makeLog(memberId, "HOME", LocalDateTime.now().withHour(22));

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(out1, out2, out3, home1, home2));

          // when
          HourPredictionResponse result = predictionService.getAverageLeaveTime(memberId, days);

          // then
          assertThat(result.averageHour()).isCloseTo(9.0, within(0.01));
          assertThat(result.sampleCount()).isEqualTo(3);  // OUTSIDE만 카운트
          assertThat(result.days()).isEqualTo(7);
      }

      @Test
      void getAverageLeaveTime_회원없음() {
          // given
          given(memberRepository.existsById(99L)).willReturn(false);

          // when & then
          assertThatThrownBy(() -> predictionService.getAverageLeaveTime(99L, 7))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("회원을 찾을 수 없습니다");
      }

      @Test
      void getAverageLeaveTime_OUTSIDE로그없음() {
          // given: HOME 로그만 있음
          Long memberId = 1L;
          given(memberRepository.existsById(memberId)).willReturn(true);

          CheckInLog home = makeLog(memberId, "HOME", LocalDateTime.now().withHour(20));
          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(home));

          // when
          HourPredictionResponse result = predictionService.getAverageLeaveTime(memberId, 7);

          // then
          assertThat(result.averageHour()).isEqualTo(0.0);
          assertThat(result.sampleCount()).isZero();
          assertThat(result.days()).isEqualTo(7);
      }

      @Test
      void getAverageReturnTime_성공() {
          // given: HOME 2건(20시, 22시) + OUTSIDE 1건 → 평균 21.0
          Long memberId = 1L;
          given(memberRepository.existsById(memberId)).willReturn(true);

          CheckInLog home1 = makeLog(memberId, "HOME", LocalDateTime.now().withHour(20));
          CheckInLog home2 = makeLog(memberId, "HOME", LocalDateTime.now().withHour(22));
          CheckInLog out = makeLog(memberId, "OUTSIDE", LocalDateTime.now().withHour(8));

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(home1, home2, out));

          // when
          HourPredictionResponse result = predictionService.getAverageReturnTime(memberId, 7);

          // then
          assertThat(result.averageHour()).isCloseTo(21.0, within(0.01));
          assertThat(result.sampleCount()).isEqualTo(2);
          assertThat(result.days()).isEqualTo(7);
      }

      private CheckInLog makeLog(Long memberId, String status, LocalDateTime checkedAt) {
          CheckInLog log = CheckInLog.builder()
                  .memberId(memberId)
                  .familyId(99L)
                  .status(status)
                  .checkedAt(checkedAt)
                  .build();
          ReflectionTestUtils.setField(log, "id", System.nanoTime());
          return log;
      }
      
      
      @Test
      void getTodayReturnComparison_늦음() {
          // given: 어제 19:00, 그저께 19:00 → 평균 19:00 (1140분)
          //        오늘 20:00 (1200분) → 차이 60분, 늦음
          Long memberId = 1L;
          given(memberRepository.existsById(memberId)).willReturn(true);

          LocalDate todayDate = LocalDate.now();
          LocalDateTime today2000 = todayDate.atTime(20, 0);
          LocalDateTime yesterday1900 = todayDate.minusDays(1).atTime(19, 0);
          LocalDateTime twoDaysAgo1900 = todayDate.minusDays(2).atTime(19, 0);

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(
                          makeLog(memberId, "HOME", today2000),
                          makeLog(memberId, "HOME", yesterday1900),
                          makeLog(memberId, "HOME", twoDaysAgo1900)));

          // when
          TodayComparisonResponse result = predictionService.getTodayReturnComparison(memberId, 7);

          // then
          assertThat(result.hasComparison()).isTrue();
          assertThat(result.todayReturnHour()).isCloseTo(20.0, within(0.01));
          assertThat(result.averageReturnHour()).isCloseTo(19.0, within(0.01));
          assertThat(result.diffMinutes()).isEqualTo(60);
          assertThat(result.isLate()).isTrue();
      }

      @Test
      void getTodayReturnComparison_늦지않음() {
          // given: 어제 19:00, 오늘 19:10 → 차이 10분 (30분 미만)
          Long memberId = 1L;
          given(memberRepository.existsById(memberId)).willReturn(true);

          LocalDate todayDate = LocalDate.now();
          LocalDateTime today1910 = todayDate.atTime(19, 10);
          LocalDateTime yesterday1900 = todayDate.minusDays(1).atTime(19, 0);

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(
                          makeLog(memberId, "HOME", today1910),
                          makeLog(memberId, "HOME", yesterday1900)));

          // when
          TodayComparisonResponse result = predictionService.getTodayReturnComparison(memberId, 7);

          // then
          assertThat(result.hasComparison()).isTrue();
          assertThat(result.diffMinutes()).isEqualTo(10);
          assertThat(result.isLate()).isFalse();
      }

      @Test
      void getTodayReturnComparison_오늘로그없음() {
          // given: 어제 HOME만 있음. 오늘 데이터 X
          Long memberId = 1L;
          given(memberRepository.existsById(memberId)).willReturn(true);

          LocalDateTime yesterday1900 = LocalDate.now().minusDays(1).atTime(19, 0);

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(makeLog(memberId, "HOME", yesterday1900)));

          // when
          TodayComparisonResponse result = predictionService.getTodayReturnComparison(memberId, 7);

          // then
          assertThat(result.hasComparison()).isFalse();
          assertThat(result.todayReturnHour()).isNull();
          assertThat(result.diffMinutes()).isNull();
          assertThat(result.isLate()).isFalse();
          assertThat(result.averageReturnHour()).isCloseTo(19.0, within(0.01));
      }

      @Test
      void getTodayReturnComparison_과거데이터없음() {
          // given: 오늘 HOME만 있음. 과거 데이터 X
          Long memberId = 1L;
          given(memberRepository.existsById(memberId)).willReturn(true);

          LocalDateTime today1900 = LocalDate.now().atTime(19, 0);

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(makeLog(memberId, "HOME", today1900)));

          // when
          TodayComparisonResponse result = predictionService.getTodayReturnComparison(memberId, 7);

          // then
          assertThat(result.hasComparison()).isFalse();
          assertThat(result.todayReturnHour()).isCloseTo(19.0, within(0.01));
          assertThat(result.averageReturnHour()).isEqualTo(0.0);
          assertThat(result.diffMinutes()).isNull();
          assertThat(result.isLate()).isFalse();
      }
  }