package com.example.house.integration;

import com.example.house.dto.AcceptInviteRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class FamilyFlowIntegrationTest {

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
    @DisplayName("alice 가족 생성 → bob 초대 수락 + 1회용 토큰 재사용 시 실패")
    void 가족_생성_초대_수락_플로우() throws Exception {

        // ─── 1. alice 회원가입 + 로그인 ────────────────
        String aliceToken = signupAndLogin(
                "alice@example.com", "Qwer1234", "alice", "01011112222", LocalDate.of(1990, 3, 15));
        assertThat(aliceToken).isNotBlank();

        // ─── 2. bob 회원가입 + 로그인 ───────────────────
        String bobToken = signupAndLogin(
                "bob@example.com", "Qwer1234", "bob", "01033334444", LocalDate.of(1992, 7, 20));
        assertThat(bobToken).isNotBlank();

        // ─── 3. alice 가족 생성 "도라네" ─────────────────
        CreateFamilyRequest createFamilyReq = new CreateFamilyRequest("도라네");

        String createFamilyJson = mockMvc.perform(post("/api/families")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createFamilyReq)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long familyId = objectMapper.readTree(createFamilyJson).get("familyId").asLong();
        String familyName = objectMapper.readTree(createFamilyJson).get("name").asText();
        assertThat(familyId).isNotNull();
        assertThat(familyName).isEqualTo("도라네");

        // ─── 4. alice 초대 토큰 생성 ─────────────────────
        String createInviteJson = mockMvc.perform(post("/api/families/" + familyId + "/invites")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String inviteToken = objectMapper.readTree(createInviteJson).get("token").asText();
        assertThat(inviteToken).isNotBlank();

        // ─── 5. bob 초대 수락 ────────────────────────
        AcceptInviteRequest acceptReq = new AcceptInviteRequest(inviteToken);

        String acceptJson = mockMvc.perform(post("/api/families/invites/accept")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode acceptNode = objectMapper.readTree(acceptJson);
        assertThat(acceptNode.get("familyId").asLong()).isEqualTo(familyId);
        assertThat(acceptNode.get("familyName").asText()).isEqualTo("도라네");

        // ─── 6. 1회용 토큰 재사용 시 실패 검증 ─────────────
        // bob이 이미 사용한 토큰으로 다시 시도 → 400 Bad Request
        mockMvc.perform(post("/api/families/invites/accept")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 회원가입 → 로그인 한 사이클 → accessToken 반환
     * (휴대폰 인증 토큰 발급 포함)
     */
    private String signupAndLogin(String email, String password, String nickname,
                                  String phone, LocalDate birthDate) throws Exception {

        // 인증코드 발송
        SendCodeRequest sendCodeReq = new SendCodeRequest(phone, "SIGNUP");
        mockMvc.perform(post("/api/auth/phone/send-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sendCodeReq)))
                .andExpect(status().isOk());

        // Redis에서 코드 직접 꺼냄 (Mock SMS라 실제 발송 안 됨)
        String code = redisTemplate.opsForValue().get("phone:verify:SIGNUP:" + phone);

        // 인증코드 검증 → phoneVerificationToken 발급
        VerifyCodeRequest verifyReq = new VerifyCodeRequest(phone, code, "SIGNUP");
        String verifyJson = mockMvc.perform(post("/api/auth/phone/verify-code")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String phoneVerificationToken = objectMapper.readTree(verifyJson).get("verificationToken").asText();

        // 회원가입
        SignupRequest signupReq = new SignupRequest(
                email, password, nickname, birthDate, phone, phoneVerificationToken);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(signupReq)))
                .andExpect(status().isCreated());

        // 로그인 → accessToken
        LoginRequest loginReq = new LoginRequest(email, password);
        String loginJson = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(loginJson).get("accessToken").asText();
    }
}
