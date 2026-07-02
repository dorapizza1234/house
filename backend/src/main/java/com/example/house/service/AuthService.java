package com.example.house.service;

import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.dto.FindIdResponse;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.LoginResponse;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.SignupResponse;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    // 인증 토큰 검증 + 이메일/번호 중복 + 해싱 + 저장
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        JwtUtil.PhoneVerificationClaim claim = jwtUtil.parsePhoneVerificationToken(
                request.phoneVerificationToken()
        );
        if (!"SIGNUP".equals(claim.purpose())) {
            throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다");
        }
        if (!claim.phone().equals(request.phone())) {
            throw new IllegalArgumentException("인증된 번호와 일치하지 않습니다");
        }

        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다");
        }
        if (memberRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("이미 가입된 번호입니다");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        Member member = Member.builder()
                .email(request.email())
                .passwordHash(hashedPassword)
                .nickname(request.nickname())
                .birthDate(request.birthDate())
                .phone(request.phone())
                .build();

        Member saved = memberRepository.save(member);

        return new SignupResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getNickname()
        );
    }

    // 회원 조회 + 비번 매칭 + 토큰 발급
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String accessToken = jwtUtil.generateAccessToken(member.getEmail());
        String refreshToken = refreshTokenService.issue(member.getEmail());

        Long familyId = familyMemberRepository.findByMemberId(member.getId())
                .stream().findFirst()
                .map(FamilyMember::getFamilyId)
                .orElse(null);

        return new LoginResponse(accessToken, refreshToken, familyId);
    }

    // ID 찾기 — 인증 토큰으로 phone 추출 → 가입 회원의 이메일 마스킹
    @Transactional(readOnly = true)
    public FindIdResponse findId(String verificationToken) {
        JwtUtil.PhoneVerificationClaim claim = jwtUtil.parsePhoneVerificationToken(verificationToken);
        if (!"FIND_ID".equals(claim.purpose())) {
            throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다");
        }

        Member member = memberRepository.findByPhone(claim.phone())
                .orElseThrow(() -> new IllegalArgumentException("가입된 회원이 아닙니다"));

        return new FindIdResponse(maskEmail(member.getEmail()));
    }

    // 이메일 중복확인
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(email);
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        int len = local.length();

        if (len <= 4) {
            // 짧은 경우: 첫 1자만 보이고 나머지는 별
            return local.charAt(0) + "*".repeat(Math.max(1, len - 1)) + domain;
        }
        // 일반: 앞 3자 + 별(len - 4)개 + 마지막 1자
        return local.substring(0, 3) + "*".repeat(len - 4) + local.charAt(len - 1) + domain;
    }
}
