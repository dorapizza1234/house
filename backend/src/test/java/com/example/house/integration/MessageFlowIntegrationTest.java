package com.example.house.integration;

import com.example.house.dto.AcceptInviteRequest;
import com.example.house.dto.CreateFamilyRequest;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.SendCodeRequest;
import com.example.house.dto.SendMessageRequest;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.VerifyCodeRequest;
import com.example.house.repository.MemberRepository;
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
class MessageFlowIntegrationTest {

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
    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("자유입력 메시지 전송 → 0점 적립 + 가족 외부 발신 차단")
    void 메시지_전송_플로우() throws Exception {
        // 가족 셋업: alice (OWNER) + bob (MEMBER)
        String aliceToken = signupAndLogin("alice@example.com", "Qwer1234", "alice",
                "01011112222", LocalDate.of(1990, 3, 15));
        String bobToken = signupAndLogin("bob@example.com", "Qwer1234", "bob",
                "01033334444", LocalDate.of(1992, 7, 20));

        Long familyId = createFamily(aliceToken, "도라네");
        String inviteToken = createInvite(aliceToken, familyId);
        acceptInvite(bobToken, inviteToken);

        // alice/bob의 memberId 조회 (메시지 receiver 지정용)
        Long bobId = memberRepository.findByEmail("bob@example.com").orElseThrow().getId();

        // ─── 자유입력 메시지 전송 (messageTypeId=null) ────
        SendMessageRequest msgReq = new SendMessageRequest(bobId, null, "오늘 잘 지냈어?");

        String sendJson = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sendNode = objectMapper.readTree(sendJson);
        assertThat(sendNode.get("messageId").asLong()).isPositive();
        assertThat(sendNode.get("finalPoints").asInt()).isEqualTo(0);   // 자유입력 = 0점
        assertThat(sendNode.get("multiplier").asInt()).isEqualTo(1);    // 곱셈 없음

        // ─── 가족 메시지 목록 조회 → 1건 ─────────────────
        String listJson = mockMvc.perform(get("/api/families/" + familyId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listNode = objectMapper.readTree(listJson);
        assertThat(listNode.isArray()).isTrue();
        assertThat(listNode.size()).isEqualTo(1);
        assertThat(listNode.get(0).get("content").asText()).isEqualTo("오늘 잘 지냈어?");

        // ─── 가족 외부 발신자 차단 검증 ───────────────────
        // charlie는 가족 멤버 아님 → 메시지 전송 시도 → 400
        String charlieToken = signupAndLogin("charlie@example.com", "Qwer1234", "charlie",
                "01055556666", LocalDate.of(1995, 1, 1));

        SendMessageRequest invalidReq = new SendMessageRequest(bobId, null, "외부 침입");
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + charlieToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidReq)))
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

    private String createInvite(String token, Long familyId) throws Exception {
        String json = mockMvc.perform(post("/api/families/" + familyId + "/invites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private void acceptInvite(String token, String inviteToken) throws Exception {
        AcceptInviteRequest req = new AcceptInviteRequest(inviteToken);
        mockMvc.perform(post("/api/families/invites/accept")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
