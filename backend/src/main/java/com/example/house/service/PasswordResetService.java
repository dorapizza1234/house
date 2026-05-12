package com.example.house.service;

import com.example.house.domain.Member;
import com.example.house.domain.PasswordResetToken;
import com.example.house.repository.MemberRepository;
import com.example.house.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /** 재설정 메일 발송 */
    @Transactional
    public void requestReset(String email) {
        // 회원 없어도 조용히 통과 (이메일 존재 여부 노출 방지)
        memberRepository.findByEmail(email).ifPresent(member -> {
            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .memberId(member.getId())
                    .token(token)
                    .build();
            tokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(email, token);
        });
    }

    /** 토큰으로 비번 재설정 */
    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 토큰입니다");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("만료된 토큰입니다");
        }

        Member member = memberRepository.findById(resetToken.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));

        if (passwordEncoder.matches(newPassword, member.getPasswordHash())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다");
        }

        member.updatePassword(passwordEncoder.encode(newPassword));
        resetToken.markAsUsed();
        // dirty checking으로 둘 다 자동 UPDATE
    }
}
