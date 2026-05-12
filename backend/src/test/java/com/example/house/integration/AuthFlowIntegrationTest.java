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
}
