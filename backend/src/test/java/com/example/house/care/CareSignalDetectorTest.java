package com.example.house.care;

import com.example.house.domain.CheckInLog;
import com.example.house.domain.FamilyEvent;
import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.dto.TodayComparisonResponse;
import com.example.house.repository.CheckInLogRepository;
import com.example.house.repository.FamilyEventRepository;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.service.PredictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CareSignalDetectorTest {

    @Mock FamilyMemberRepository familyMemberRepository;
    @Mock MemberRepository memberRepository;
    @Mock PredictionService predictionService;
    @Mock CheckInLogRepository checkInLogRepository;
    @Mock FamilyEventRepository familyEventRepository;
    @InjectMocks CareSignalDetector detector;

    private FamilyMember familyMember(Long familyId, Long memberId) {
        return FamilyMember.builder().familyId(familyId).memberId(memberId).role("MEMBER").build();
    }

    @Test
    @DisplayName("LATE_RETURN - 평소보다 30분+ 늦으면 신호 1개 생성")
    void lateReturn_감지() {
        Long familyId = 1L;
        Member member = mock(Member.class);
        given(member.getId()).willReturn(10L);
        given(member.getNickname()).willReturn("엄마");

        given(familyMemberRepository.findByFamilyId(familyId)).willReturn(List.of(familyMember(familyId, 10L)));
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(predictionService.getTodayReturnComparison(10L, 14))
                .willReturn(new TodayComparisonResponse(19.63, 18.5, 68, true, true, 14));

        List<CareSignal> signals = detector.detect(familyId);

        assertThat(signals).hasSize(1);
        CareSignal s = signals.get(0);
        assertThat(s.type()).isEqualTo(SignalType.LATE_RETURN);
        assertThat(s.subject()).isEqualTo("엄마");
        assertThat(s.subjectMemberId()).isEqualTo(10L);
        assertThat(s.detail()).contains("68");
    }

    @Test
    @DisplayName("LATE_RETURN - 늦지 않으면 신호 없음")
    void 정시귀가_신호없음() {
        Long familyId = 1L;
        Member member = mock(Member.class);
        given(member.getId()).willReturn(10L);

        given(familyMemberRepository.findByFamilyId(familyId)).willReturn(List.of(familyMember(familyId, 10L)));
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(predictionService.getTodayReturnComparison(10L, 14))
                .willReturn(new TodayComparisonResponse(18.4, 18.5, -6, false, true, 14));

        assertThat(detector.detect(familyId)).isEmpty();
    }

    @Test
    @DisplayName("NO_OUTING_3DAYS - 최근 3일 로그는 있으나 OUTSIDE 없으면 신호")
    void 외출없음_감지() {
        Long familyId = 1L;
        Member member = mock(Member.class);
        given(member.getId()).willReturn(10L);
        given(member.getNickname()).willReturn("아빠");

        given(familyMemberRepository.findByFamilyId(familyId)).willReturn(List.of(familyMember(familyId, 10L)));
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(predictionService.getTodayReturnComparison(10L, 14))
                .willReturn(new TodayComparisonResponse(18.0, 18.0, 0, false, true, 14)); // not late
        CheckInLog homeLog = CheckInLog.builder()
                .memberId(10L).familyId(familyId).status("HOME").checkedAt(LocalDateTime.now()).build();
        given(checkInLogRepository.findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(10L), any()))
                .willReturn(List.of(homeLog));

        List<CareSignal> signals = detector.detect(familyId);

        assertThat(signals).extracting(CareSignal::type).containsExactly(SignalType.NO_OUTING_3DAYS);
    }

    @Test
    @DisplayName("BIRTHDAY - 오늘이 생일이면 신호")
    void 생일_감지() {
        Long familyId = 1L;
        LocalDate today = LocalDate.now();
        Member member = mock(Member.class);
        given(member.getId()).willReturn(10L);
        given(member.getNickname()).willReturn("딸");
        given(member.getBirthDate()).willReturn(LocalDate.of(1990, today.getMonthValue(), today.getDayOfMonth()));

        given(familyMemberRepository.findByFamilyId(familyId)).willReturn(List.of(familyMember(familyId, 10L)));
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(predictionService.getTodayReturnComparison(10L, 14))
                .willReturn(new TodayComparisonResponse(18.0, 18.0, 0, false, true, 14)); // not late

        List<CareSignal> signals = detector.detect(familyId);

        assertThat(signals).extracting(CareSignal::type).contains(SignalType.BIRTHDAY);
    }

    @Test
    @DisplayName("HOSPITAL_TODAY - 오늘 제목에 '병원' 들어간 일정이 있으면 신호(가족 단위)")
    void 병원일정_감지() {
        Long familyId = 1L;
        given(familyMemberRepository.findByFamilyId(familyId)).willReturn(List.of()); // 멤버 루프는 비움

        FamilyEvent event = FamilyEvent.builder()
                .familyId(familyId).type("ETC").title("정형외과 병원 진료")
                .eventDate(LocalDate.now()).isYearly(false).creatorId(1L).memo(null).build();
        given(familyEventRepository.findByFamilyId(familyId)).willReturn(List.of(event));

        List<CareSignal> signals = detector.detect(familyId);

        assertThat(signals).hasSize(1);
        CareSignal s = signals.get(0);
        assertThat(s.type()).isEqualTo(SignalType.HOSPITAL_TODAY);
        assertThat(s.subjectMemberId()).isNull();
        assertThat(s.subject()).isEqualTo("가족");
        assertThat(s.detail()).contains("정형외과 병원 진료");
    }
}
