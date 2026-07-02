package com.example.house.service;

import com.example.house.security.JwtUtil;
import com.example.house.security.JwtUtil.RefreshClaim; // 1. 내부 클래스 구조에 맞게 import
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;   // redis.opsForValue() 반환 가짜 객체
    @Mock JwtUtil jwtUtil;

    @InjectMocks RefreshTokenService refreshTokenService;

    @Test
    void issue_성공() {
        // given
        String email = "alice@test.com";
        given(redis.opsForValue()).willReturn(valueOps);
        given(jwtUtil.generateRefreshToken(eq(email), anyString())).willReturn("refresh-token");

        // when
        String result = refreshTokenService.issue(email);

        // then
        assertThat(result).isEqualTo("refresh-token");
        verify(valueOps).set(eq("refresh:" + email), anyString(), any());  // Redis에 잘 저장되었는지 확인
    }

    @Test
    void rotate_성공_jti일치하면_새토큰발급() {
        // given
        String email = "alice@test.com";
        given(jwtUtil.parseRefreshToken("old-refresh")).willReturn(new RefreshClaim(email, "jti-A"));
        given(redis.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:" + email)).willReturn("jti-A");        // 저장된 것과 일치함
        
        // rotate 내부의 issue() 호출 대응
        given(jwtUtil.generateAccessToken(email)).willReturn("new-access");
        given(jwtUtil.generateRefreshToken(eq(email), anyString())).willReturn("new-refresh");

        // when
        RefreshTokenService.TokenPair result = refreshTokenService.rotate("old-refresh");

        // then
        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void rotate_재사용감지_jti불일치하면_전체폐기() {
        // given
        String email = "alice@test.com";
        given(jwtUtil.parseRefreshToken("old-refresh")).willReturn(new RefreshClaim(email, "jti-OLD"));
        given(redis.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:" + email)).willReturn("jti-NEW");      // 이미 회전되어 변해있음

        // when & then (예외가 발생하는지 확인)
        assertThatThrownBy(() -> refreshTokenService.rotate("old-refresh"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("비정상적인 접근이 감지되었습니다.");
                
        verify(redis).delete("refresh:" + email);  // 핵심: 탈취 대응으로 Redis에서 삭제(무효화)되었는지 검증
    }

    @Test
    void rotate_만료_redis에없으면_예외() {
        // given
        String email = "alice@test.com";
        given(jwtUtil.parseRefreshToken("old-refresh")).willReturn(new RefreshClaim(email, "jti-A"));
        given(redis.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:" + email)).willReturn(null);           // 만료되거나 로그아웃됨

        // when & then
        assertThatThrownBy(() -> refreshTokenService.rotate("old-refresh"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("세션이 만료되었습니다.");
                
        verify(redis, never()).delete(anyString());  // 지울 것도 없으므로 delete가 호출되면 안 됨
    }
}