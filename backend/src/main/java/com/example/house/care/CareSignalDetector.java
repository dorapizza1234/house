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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareSignalDetector {

    private static final int WINDOW_DAYS = 14;   // 평균 계산 윈도우

    private final FamilyMemberRepository familyMemberRepository;
    private final MemberRepository memberRepository;
    private final PredictionService predictionService;
    private final CheckInLogRepository checkInLogRepository;
    private final FamilyEventRepository familyEventRepository;

    /** 한 가족의 모든 케어 신호를 판정 */
    public List<CareSignal> detect(Long familyId) {
        List<CareSignal> signals = new ArrayList<>();
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(familyId);

        for (FamilyMember fm : members) {
            Member member = memberRepository.findById(fm.getMemberId()).orElse(null);
            if (member == null) continue;

            detectLateReturn(familyId, member).ifPresent(signals::add);
            detectNoOuting(familyId, member).ifPresent(signals::add);
            detectBirthday(familyId, member).ifPresent(signals::add);
        }
        signals.addAll(detectHospitalToday(familyId));   // 병원은 가족 단위라 루프 밖
        return signals;
    }

    /** 평소보다 30분+ 늦게 귀가했으면 LATE_RETURN 신호 (기존 PredictionService 재사용) */
    private Optional<CareSignal> detectLateReturn(Long familyId, Member member) {
        TodayComparisonResponse c =
                predictionService.getTodayReturnComparison(member.getId(), WINDOW_DAYS);

        if (!c.isLate()) {
            return Optional.empty();
        }

        String detail = "평소보다 약 " + c.diffMinutes() + "분 늦게 귀가했습니다.";
        return Optional.of(new CareSignal(
                familyId, member.getId(), member.getNickname(),
                SignalType.LATE_RETURN, detail));
    }

    /** 최근 3일간 로그는 있는데 OUTSIDE가 하나도 없으면 = 외출 안 함 */
    private Optional<CareSignal> detectNoOuting(Long familyId, Member member) {
        LocalDateTime since = LocalDateTime.now().minusDays(3);
        List<CheckInLog> logs = checkInLogRepository
                .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(member.getId(), since);

        if (logs.isEmpty()) return Optional.empty();   // 데이터 없으면 판단 보류(오탐 방지)
        boolean anyOutside = logs.stream().anyMatch(l -> "OUTSIDE".equals(l.getStatus()));
        if (anyOutside) return Optional.empty();

        return Optional.of(new CareSignal(familyId, member.getId(), member.getNickname(),
                SignalType.NO_OUTING_3DAYS, "최근 3일간 외출 기록이 없습니다."));
    }

    /** 생일이 오늘(월/일 일치)이면 BIRTHDAY */
    private Optional<CareSignal> detectBirthday(Long familyId, Member member) {
        LocalDate bd = member.getBirthDate();
        if (bd == null) return Optional.empty();
        LocalDate today = LocalDate.now();
        if (bd.getMonthValue() == today.getMonthValue()
                && bd.getDayOfMonth() == today.getDayOfMonth()) {
            return Optional.of(new CareSignal(familyId, member.getId(), member.getNickname(),
                    SignalType.BIRTHDAY, "오늘 생일입니다."));
        }
        return Optional.empty();
    }

    /** 오늘 날짜의 가족 일정 중 제목에 "병원"이 들어간 게 있으면 HOSPITAL_TODAY (여러 개 가능) */
    private List<CareSignal> detectHospitalToday(Long familyId) {
        LocalDate today = LocalDate.now();
        List<CareSignal> result = new ArrayList<>();
        for (FamilyEvent e : familyEventRepository.findByFamilyId(familyId)) {
            boolean isToday = e.isYearly()
                    ? (e.getEventDate().getMonthValue() == today.getMonthValue()
                       && e.getEventDate().getDayOfMonth() == today.getDayOfMonth())
                    : e.getEventDate().isEqual(today);
            boolean isHospital = e.getTitle() != null && e.getTitle().contains("병원");
            if (isToday && isHospital) {
                result.add(new CareSignal(familyId, null, "가족",
                        SignalType.HOSPITAL_TODAY, "오늘 '" + e.getTitle() + "' 일정이 있습니다."));
            }
        }
        return result;
    }
}