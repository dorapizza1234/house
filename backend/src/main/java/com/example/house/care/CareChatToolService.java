package com.example.house.care;

import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.dto.HourPredictionResponse;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 챗봇(Claude tool-use)이 호출하는 조회 도구들의 백엔드.
 * 모든 메서드는 familyId 스코프를 강제한다 — AI가 넘긴 memberId가 해당 가족 소속인지 검증해
 * 다른 가족의 데이터 접근을 원천 차단한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareChatToolService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 30;

    private final FamilyMemberRepository familyMemberRepository;
    private final MemberRepository memberRepository;
    private final PredictionService predictionService;

    /** 가족 구성원 명단 — AI가 "엄마" 같은 호칭을 memberId로 매핑하는 데 사용 */
    public List<FamilyMemberBrief> listFamilyMembers(Long familyId) {
        List<Long> memberIds = familyMemberRepository.findByFamilyId(familyId).stream()
                .map(FamilyMember::getMemberId)
                .toList();
        return memberRepository.findAllById(memberIds).stream()
                .map(m -> new FamilyMemberBrief(m.getId(), m.getNickname()))
                .toList();
    }

    /** 특정 구성원의 평균 귀가 시각 (기존 PredictionService 재사용, 가족 스코프 강제) */
    public ReturnPatternResult getReturnPattern(Long familyId, Long memberId, int days) {
        requireFamilyMember(familyId, memberId);
        int window = clampDays(days);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));
        HourPredictionResponse r = predictionService.getAverageReturnTime(memberId, window);

        return new ReturnPatternResult(
                memberId, member.getNickname(), r.averageHour(), r.sampleCount(), window);
    }

    /** 요청한 memberId가 이 가족 소속인지 검증 — 아니면 예외(다른 가족 데이터 차단) */
    private void requireFamilyMember(Long familyId, Long memberId) {
        if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId)) {
            throw new IllegalArgumentException("해당 가족의 구성원이 아닙니다");
        }
    }

    private int clampDays(int days) {
        if (days < MIN_DAYS) return MIN_DAYS;
        return Math.min(days, MAX_DAYS);
    }

    public record FamilyMemberBrief(Long memberId, String nickname) {}

    public record ReturnPatternResult(
            Long memberId, String nickname, Double averageReturnHour, int sampleCount, int days) {}
}
