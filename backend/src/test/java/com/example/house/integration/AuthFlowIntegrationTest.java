package com.example.house.integration;

import com.example.house.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.house.dto.SendCodeRequest;
import com.example.house.dto.VerifyCodeRequest;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MvcResult;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @MockitoBean
    SmsService smsService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("회원가입 → 로그인 → 보호 API 호출 한 사이클 통과")
    void 회원가입_로그인_보호API() throws Exception {
        String phone ="01012345678";
        String email="test@example.com";
        String password="Qwer1234";
        
     // ─── 1. 인증코드 발송 요청 ───────────────────────
        SendCodeRequest sendCodeReq = new SendCodeRequest(phone, "SIGNUP");

        mockMvc.perform(post("/api/auth/phone/send-code")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(sendCodeReq)))
                .andExpect(status().isOk());

        // ─── 2. Redis에서 인증코드 직접 꺼내기 ────────────
        // (Mock SmsService라 실제 발송 안 됨. 코드는 Redis에 저장돼 있음)
        String redisKey = "phone:verify:SIGNUP:" + phone;
        String code = redisTemplate.opsForValue().get(redisKey);
        assertThat(code).isNotNull().hasSize(6);

        // ─── 3. 인증코드 검증 → 임시 토큰 받기 ───────────
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(phone, code, "SIGNUP");

        String verifyResponseJson = mockMvc.perform(post("/api/auth/phone/verify-code")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode verifyJson = objectMapper.readTree(verifyResponseJson);
        String phoneVerificationToken = verifyJson.get("verificationToken").asText();
        assertThat(phoneVerificationToken).isNotBlank();

        // ─── 4. 회원가입 ──────────────────────────────
        SignupRequest signupReq = new SignupRequest(
                email,
                password,
                "테스트사용자",
                LocalDate.of(1995, 5, 12),
                phone,
                phoneVerificationToken
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(signupReq)))
                .andExpect(status().isCreated());

        // ─── 5. 로그인 → Access Token 받기 ───────────
        LoginRequest loginReq = new LoginRequest(email, password);

        String loginResponseJson = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponseJson);
        String accessToken = loginJson.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        // ─── 6. 보호 API 호출 → 200 + 본인 이메일 응답 확인 ─
        mockMvc.perform(get("/api/test/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string("인증된 사용자: " + email));


    }

    @Test
    @DisplayName("refresh 회전 + 재사용 감지 - 회전되고, 옛 토큰 재사용 시 패밀리 전체 폐기")
    void refresh_회전_및_재사용감지() throws Exception {
        registerUser("rtr@example.com", "Qwer1234", "01099990001", "회전유저");
        MvcResult loginResult = login("rtr@example.com", "Qwer1234");

        String setCookie1 = refreshSetCookie(loginResult);
        assertThat(setCookie1).contains("HttpOnly");   // 쿠키가 HttpOnly인지
        String v1 = cookieValue(setCookie1);

        // 회전: R1로 refresh → 200 + 새 쿠키(R2), R2 != R1
        MvcResult rotate = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", v1)))
                .andExpect(status().isOk())
                .andReturn();
        String v2 = cookieValue(refreshSetCookie(rotate));
        assertThat(v2).isNotEqualTo(v1);

        // 재사용 감지: 이미 쓴 R1 다시 사용 → 거부
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", v1)))
                .andExpect(status().is4xxClientError());

        // 재사용 감지로 패밀리 폐기됨 → 회전으로 받은 R2도 무효
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", v2)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그아웃 - Redis의 refresh jti가 삭제되어 이후 회전 불가")
    void 로그아웃_후_회전불가() throws Exception {
        registerUser("logout@example.com", "Qwer1234", "01099990002", "로그아웃유저");
        MvcResult loginResult = login("logout@example.com", "Qwer1234");
        String v = cookieValue(refreshSetCookie(loginResult));

        // 로그인 직후 Redis에 refresh jti 저장돼 있음
        assertThat(redisTemplate.opsForValue().get("refresh:logout@example.com")).isNotNull();

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", v)))
                .andExpect(status().isOk());

        // Redis에서 삭제됨
        assertThat(redisTemplate.opsForValue().get("refresh:logout@example.com")).isNull();

        // 이후 그 refresh로 회전 시도 → 거부
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", v)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("토큰 타입 분리 - refresh 토큰을 access처럼 쓰면 401, 토큰 없어도 401")
    void 보호API_refresh토큰_및_무토큰_거부() throws Exception {
        registerUser("typecheck@example.com", "Qwer1234", "01099990003", "타입유저");
        MvcResult loginResult = login("typecheck@example.com", "Qwer1234");
        String refreshJwt = cookieValue(refreshSetCookie(loginResult));  // 쿠키값 = refresh JWT

        // refresh 토큰을 Bearer로 → type!=access → 401 (Step7 + entry point 수정)
        mockMvc.perform(get("/api/test/me")
                        .header("Authorization", "Bearer " + refreshJwt))
                .andExpect(status().isUnauthorized());

        // 토큰 없음 → 401 (403 아님)
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 헬퍼 ───────────────────────────────────────
    private void registerUser(String email, String password, String phone, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/phone/send-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SendCodeRequest(phone, "SIGNUP"))))
                .andExpect(status().isOk());
        String code = redisTemplate.opsForValue().get("phone:verify:SIGNUP:" + phone);
        String verifyJson = mockMvc.perform(post("/api/auth/phone/verify-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(phone, code, "SIGNUP"))))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(verifyJson).get("verificationToken").asText();
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                email, password, nickname, LocalDate.of(1995, 5, 12), phone, token))))
                .andExpect(status().isCreated());
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    /** 응답의 Set-Cookie 헤더 중 refreshToken 항목 전체 문자열 */
    private String refreshSetCookie(MvcResult result) {
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith("refreshToken="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("refreshToken Set-Cookie 없음"));
    }

    /** "refreshToken=VALUE; Path=..." → VALUE */
    private String cookieValue(String setCookieHeader) {
        String first = setCookieHeader.split(";", 2)[0];      // refreshToken=VALUE
        return first.substring("refreshToken=".length());
    }
}
