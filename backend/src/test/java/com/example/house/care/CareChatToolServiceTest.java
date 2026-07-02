package com.example.house.care;

import com.example.house.care.CareChatToolService.FamilyMemberBrief;
import com.example.house.care.CareChatToolService.ReturnPatternResult;
import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.dto.HourPredictionResponse;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.service.PredictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CareChatToolServiceTest {

    @Mock FamilyMemberRepository familyMemberRepository;
    @Mock MemberRepository memberRepository;
    @Mock PredictionService predictionService;
    @InjectMocks CareChatToolService toolService;

    @Test
    @DisplayName("listFamilyMembers - 가족 구성원 명단(memberId+nickname) 반환")
    void 가족명단() {
        Long familyId = 1L;
        FamilyMember fm = FamilyMember.builder().familyId(familyId).memberId(10L).role("MEMBER").build();
        Member member = org.mockito.Mockito.mock(Member.class);
        given(member.getId()).willReturn(10L);
        given(member.getNickname()).willReturn("엄마");

        given(familyMemberRepository.findByFamilyId(familyId)).willReturn(List.of(fm));
        given(memberRepository.findAllById(any())).willReturn(List.of(member));

        List<FamilyMemberBrief> result = toolService.listFamilyMembers(familyId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberId()).isEqualTo(10L);
        assertThat(result.get(0).nickname()).isEqualTo("엄마");
    }

    @Test
    @DisplayName("getReturnPattern - 가족 구성원이면 평균 귀가시각 반환")
    void 귀가패턴_성공() {
        Long familyId = 1L;
        Member member = org.mockito.Mockito.mock(Member.class);
        given(member.getNickname()).willReturn("엄마");

        given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, 10L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(predictionService.getAverageReturnTime(10L, 14))
                .willReturn(new HourPredictionResponse(19.05, 12, 14));

        ReturnPatternResult r = toolService.getReturnPattern(familyId, 10L, 14);

        assertThat(r.nickname()).isEqualTo("엄마");
        assertThat(r.averageReturnHour()).isEqualTo(19.05);
        assertThat(r.sampleCount()).isEqualTo(12);
        assertThat(r.days()).isEqualTo(14);
    }

    @Test
    @DisplayName("getReturnPattern - 다른 가족 memberId면 예외 + 조회 안 함(스코프 차단)")
    void 귀가패턴_스코프차단() {
        Long familyId = 1L;
        given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, 99L)).willReturn(false);

        assertThatThrownBy(() -> toolService.getReturnPattern(familyId, 99L, 14))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 가족의 구성원이 아닙니다");

        verify(predictionService, never()).getAverageReturnTime(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("getReturnPattern - days가 30 초과면 30으로 clamp")
    void 귀가패턴_days_clamp() {
        Long familyId = 1L;
        Member member = org.mockito.Mockito.mock(Member.class);
        given(member.getNickname()).willReturn("아빠");

        given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, 10L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));
        given(predictionService.getAverageReturnTime(10L, 30))
                .willReturn(new HourPredictionResponse(20.0, 20, 30));

        ReturnPatternResult r = toolService.getReturnPattern(familyId, 10L, 90); // 90 → 30

        assertThat(r.days()).isEqualTo(30);
        verify(predictionService).getAverageReturnTime(10L, 30);
    }
}
