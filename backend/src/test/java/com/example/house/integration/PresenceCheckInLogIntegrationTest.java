package com.example.house.integration;

import com.example.house.dto.CreateFamilyRequest;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.SendCodeRequest;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.VerifyCodeRequest;
import com.example.house.service.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PresenceCheckInLogIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @MockitoBean
    SmsService smsService;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("재실 토글 → check_in_log 이벤트 소싱 패턴 검증")
    void 토글_체크인로그_플로우() throws Exception {
        // alice 가입 + 가족 생성 (가족 있어야 family_id가 로그에 박힘)
        String aliceToken = signupAndLogin("alice@example.com", "Qwer1234", "alice",
                "01011112222", LocalDate.of(1990, 3, 15));
        Long familyId = createFamily(aliceToken, "도라네");

        // ─── 토글 1회: OUTSIDE → HOME ──────────────────
        String toggle1Json = mockMvc.perform(post("/api/me/presence/toggle")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(toggle1Json).get("status").asText()).isEqualTo("HOME");

        // ─── 본인 로그 조회 → 1건 (HOME) ────────────────
        String log1Json = mockMvc.perform(get("/api/me/check-in-logs?days=7")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode log1Node = objectMapper.readTree(log1Json);
        assertThat(log1Node.isArray()).isTrue();
        assertThat(log1Node.size()).isEqualTo(1);
        assertThat(log1Node.get(0).get("status").asText()).isEqualTo("HOME");
        assertThat(log1Node.get(0).get("memberNickname").asText()).isEqualTo("alice");

        // ─── 토글 2회: HOME → OUTSIDE ──────────────────
        String toggle2Json = mockMvc.perform(post("/api/me/presence/toggle")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(toggle2Json).get("status").asText()).isEqualTo("OUTSIDE");

        // ─── 본인 로그 조회 → 2건 (DESC 정렬: 최신 OUTSIDE 먼저) ──
        String log2Json = mockMvc.perform(get("/api/me/check-in-logs?days=7")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode log2Node = objectMapper.readTree(log2Json);
        assertThat(log2Node.size()).isEqualTo(2);
        assertThat(log2Node.get(0).get("status").asText()).isEqualTo("OUTSIDE");   // 최신
        assertThat(log2Node.get(1).get("status").asText()).isEqualTo("HOME");      // 이전

        // ─── 가족 로그 조회 → 2건 + 닉네임 매핑 검증 ──────
        String familyLogJson = mockMvc.perform(get("/api/families/" + familyId + "/check-in-logs?days=7")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode familyLogNode = objectMapper.readTree(familyLogJson);
        assertThat(familyLogNode.size()).isEqualTo(2);
        assertThat(familyLogNode.get(0).get("memberNickname").asText()).isEqualTo("alice");

        // ─── 잘못된 days 파라미터 → 400 ─────────────────
        mockMvc.perform(get("/api/me/check-in-logs?days=0")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest());
    }

    // ─── 공통 헬퍼 ────────────────────────────────

    private String signupAndLogin(String email, String password, String nickname,
                                  String phone, LocalDate birthDate) throws Exception {
        SendCodeRequest sendCodeReq = new SendCodeRequest(phone, "SIGNUP");
        mockMvc.perform(post("/api/auth/phone/send-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sendCodeReq)))
                .andExpect(status().isOk());

        String code = redisTemplate.opsForValue().get("phone:verify:SIGNUP:" + phone);

        VerifyCodeRequest verifyReq = new VerifyCodeRequest(phone, code, "SIGNUP");
        String verifyJson = mockMvc.perform(post("/api/auth/phone/verify-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String phoneVerificationToken = objectMapper.readTree(verifyJson).get("verificationToken").asText();

        SignupRequest signupReq = new SignupRequest(
                email, password, nickname, birthDate, phone, phoneVerificationToken);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(signupReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest(email, password);
        String loginJson = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(loginJson).get("accessToken").asText();
    }

    private Long createFamily(String token, String name) throws Exception {
        CreateFamilyRequest req = new CreateFamilyRequest(name);
        String json = mockMvc.perform(post("/api/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("familyId").asLong();
    }
}
