package com.example.house.service;

import com.example.house.security.JwtUtil;
import com.example.house.security.JwtUtil.RefreshClaim;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    // jwt.refresh-expiration(604800000ms = 7일)과 동일하게
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    /** 로그인 시 호출 — refresh 발급 + Redis에 현재 jti 저장 */
    public String issue(String email) {
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(email, jti);
        redis.opsForValue().set(key(email), jti, REFRESH_TTL);   // 덮어쓰기 = 항상 최신 1개
        return refreshToken;
    }

    /** /refresh 시 호출 — 회전 + 재사용 감지 */
    public TokenPair rotate(String oldRefreshToken) {
        // 1) 서명·만료·type=refresh 검증 + email/jti 추출
        RefreshClaim claim = jwtUtil.parseRefreshToken(oldRefreshToken);
        String email = claim.email();

        // 2) Redis에 저장된 '현재 유효한 jti' 조회
        String storedJti = redis.opsForValue().get(key(email));

        // 3-a) 없음 → 만료됐거나 로그아웃됨
        if (storedJti == null) {
            throw new JwtException("세션이 만료되었습니다. 다시 로그인해 주세요.");
        }

        // 3-b) 불일치 → 이미 회전된 옛 토큰 재사용 = 탈취 의심 → 전체 폐기
        if (!storedJti.equals(claim.jti())) {
            redis.delete(key(email));
            throw new JwtException("비정상적인 접근이 감지되었습니다. 다시 로그인해 주세요.");
        }

        // 3-c) 일치 → 회전: 새 access + 새 refresh(새 jti) + Redis 덮어쓰기
        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = issue(email);   // 새 jti 발급 + Redis 덮어씀
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /** 로그아웃 — 현재 refresh 무효화 */
    public void logout(String email) {
        redis.delete(key(email));
    }

    private String key(String email) {
        return "refresh:" + email;
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}