package com.example.house.service;

import com.example.house.domain.Member;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.LoginResponse;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.SignupResponse;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.security.JwtUtil;
import com.example.house.security.JwtUtil.PhoneVerificationClaim;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private AuthService authService;

    private SignupRequest signupRequest() {
        return new SignupRequest(
            "test@example.com",
            "Password123",
            "dora",
            LocalDate.of(2000, 1, 1),
            "01012345678",
            "valid.verification.token"
        );
    }

    @Test
    @DisplayName("회원가입 성공 - 인증/이메일/번호 OK면 회원 저장하고 응답 반환")
    void signup_성공() {
        // given
        SignupRequest request = signupRequest();

        given(jwtUtil.parsePhoneVerificationToken(request.phoneVerificationToken()))
                .willReturn(new PhoneVerificationClaim(request.phone(), "SIGNUP"));
        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(memberRepository.existsByPhone(request.phone())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("hashedPassword");

        Member savedMember = Member.builder()
                .email(request.email())
                .passwordHash("hashedPassword")
                .nickname(request.nickname())
                .birthDate(request.birthDate())
                .phone(request.phone())
                .build();
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("dora");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복이면 IllegalArgumentException")
    void signup_이메일중복_실패() {
        // given
        SignupRequest request = signupRequest();

        given(jwtUtil.parsePhoneVerificationToken(request.phoneVerificationToken()))
                .willReturn(new PhoneVerificationClaim(request.phone(), "SIGNUP"));
        given(memberRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 가입된 이메일입니다");

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 인증 토큰 purpose 불일치")
    void signup_purpose불일치_실패() {
        // given
        SignupRequest request = signupRequest();

        given(jwtUtil.parsePhoneVerificationToken(request.phoneVerificationToken()))
                .willReturn(new PhoneVerificationClaim(request.phone(), "FIND_ID"));

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 인증 토큰입니다");

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("로그인 성공 - 이메일/비번 일치하면 accessToken/refreshToken/familyId 반환")
    void login_성공() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "Password123");

        Member member = Member.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("dora")
                .birthDate(LocalDate.of(2000, 1, 1))
                .phone("01012345678")
                .build();

        given(memberRepository.findByEmail(request.email())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(request.password(), member.getPasswordHash())).willReturn(true);
        given(jwtUtil.generateAccessToken(member.getEmail())).willReturn("access.token.value");
        given(refreshTokenService.issue(member.getEmail())).willReturn("refresh.token.value");
        given(familyMemberRepository.findByMemberId(member.getId())).willReturn(Collections.emptyList());

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("access.token.value");
        assertThat(response.refreshToken()).isEqualTo("refresh.token.value");
        assertThat(response.familyId()).isNull();
        verify(jwtUtil).generateAccessToken("test@example.com");
    }

    @Test
    @DisplayName("로그인 실패 - 비번 틀리면 같은 메시지 (계정 열거 방지)")
    void login_비번틀림_실패() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "WrongPass123");

        Member member = Member.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("dora")
                .birthDate(LocalDate.of(2000, 1, 1))
                .phone("01012345678")
                .build();

        given(memberRepository.findByEmail(request.email())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(request.password(), member.getPasswordHash())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");

        verify(jwtUtil, never()).generateAccessToken(any());
    }
}
