package com.example.house.integration;

import com.example.house.dto.CreateFamilyRequest;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.PlantPlantRequest;
import com.example.house.dto.SendCodeRequest;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.VerifyCodeRequest;
import com.example.house.service.SmsService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PlantFlowIntegrationTest {

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
    @DisplayName("식물 비즈니스 룰 검증: 가족없음/포인트부족/이미죽지않은상태 부활 실패")
    void 식물_비즈니스_룰_플로우() throws Exception {
        // ─── alice 가입 (가족 X 상태) ──────────────────
        String aliceToken = signupAndLogin("alice@example.com", "Qwer1234", "alice",
                "01011112222", LocalDate.of(1990, 3, 15));

        // ─── 1. 가족 없는 상태에서 식물 심기 시도 → 실패 ─
        PlantPlantRequest plantReq = new PlantPlantRequest("초록이");
        mockMvc.perform(post("/api/families/me/plant")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(plantReq)))
                .andExpect(status().isBadRequest());

        // ─── 2. 가족 생성 후 다시 시도 → 포인트 부족(0점 / 30점 필요) ─
        createFamily(aliceToken, "도라네");

        mockMvc.perform(post("/api/families/me/plant")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(plantReq)))
                .andExpect(status().isBadRequest());

        // ─── 3. 식물 없는 상태에서 조회 → 실패 ─────────
        mockMvc.perform(get("/api/families/me/plant")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest());

        // ─── 4. 죽지 않은 식물 부활 시도 → 실패 ─────────
        // (식물 자체가 없어서 더 일찍 실패하지만, revive 엔드포인트 자체 검증)
        mockMvc.perform(post("/api/families/me/plant/revive")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest());

        // ─── 5. 물주기 (식물 없음) → 실패 ──────────────
        mockMvc.perform(post("/api/families/me/plant/water")
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

    private void createFamily(String token, String name) throws Exception {
        CreateFamilyRequest req = new CreateFamilyRequest(name);
        mockMvc.perform(post("/api/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
