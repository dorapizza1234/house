package com.example.house.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    private static final String SECRET = "test-secret-key-for-jwt-filter-should-be-long-enough-32";
    private static final long ACCESS_EXP  = 1800000L;    // 30분
    private static final long REFRESH_EXP = 604800000L;  // 7일
    private static final long INVITE_EXP  = 86400000L;   // 1일

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXP, REFRESH_EXP, INVITE_EXP);
        filter = new JwtAuthenticationFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("access 토큰 - 인증 컨텍스트에 email이 세팅된다")
    void access토큰_인증성공() throws Exception {
        // given
        String token = jwtUtil.generateAccessToken("test@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("refresh 토큰 - type이 access가 아니라 인증되지 않는다")
    void refresh토큰_인증거부() throws Exception {
        // given
        String token = jwtUtil.generateRefreshToken("test@example.com", UUID.randomUUID().toString());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("invite 토큰 - type이 access가 아니라 인증되지 않는다")
    void invite토큰_인증거부() throws Exception {
        // given
        String token = jwtUtil.generateInviteToken(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization 헤더 없음 - 인증되지 않는다")
    void 헤더없음_인증거부() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}