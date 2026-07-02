package com.example.house.care;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 가족 안부 챗봇 — Claude tool-use 루프.
 * Claude가 질문을 이해하고 어떤 도구를 부를지 판단하면, 여기서 CareChatToolService를
 * (서버가 familyId를 주입해) 실행하고 결과를 되돌려준다. AI는 판단·표현, 데이터는 코드.
 */
@Service
public class ChatService {

    private static final int MAX_ITERATIONS = 5;   // 도구 호출 반복 상한
    private static final long MAX_TOKENS = 1024L;

    private static final String SYSTEM_PROMPT = """
            너는 '집으로' 가족 안부 도우미야. 가족의 귀가/외출/일정 같은 안부 관련 질문에만 답해.
            - 반드시 제공된 도구를 호출해 얻은 데이터에만 근거해서 답해. 데이터에 없거나 도구가 실패하면 모른다고 솔직히 말하고, 숫자를 절대 지어내지 마.
            - 가족 안부와 무관한 질문(날씨, 코딩, 일반상식 등)은 정중히 거절해.
            - 답변은 한국어로 따뜻하고 간결하게, 2~3문장으로.
            - averageReturnHour 같은 24시간제 소수(예: 19.5)는 '저녁 7시 반'처럼 사람이 읽기 쉽게 바꿔서 말해줘.
            """;

    private final CareChatToolService tools;
    private final AnthropicClient client;
    private final String model;
    private final List<Tool> toolDefs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatService(CareChatToolService tools,
                       @Value("${anthropic.api-key}") String apiKey,
                       @Value("${anthropic.model}") String model) {
        this.tools = tools;
        this.model = model;
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.toolDefs = buildTools();
    }

    /** 사용자 질문 1건에 답한다. familyId는 서버(컨트롤러)가 인증정보에서 주입 — AI는 못 넘긴다. */
    public String chat(Long familyId, String userMessage) {
        try {
            return runLoop(familyId, userMessage);
        } catch (Exception e) {
            return "죄송해요, 지금은 답변을 드릴 수 없어요. 잠시 후 다시 시도해 주세요.";
        }
    }

    private String runLoop(Long familyId, String userMessage) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessage(userMessage);
        toolDefs.forEach(builder::addTool);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Message message = client.messages().create(builder.build());

            StopReason stop = message.stopReason().orElse(null);
            if (!StopReason.TOOL_USE.equals(stop)) {
                return extractText(message);   // 최종 답변
            }

            // 도구 호출 요청 → 응답을 대화에 넣고, 각 도구를 실행해 결과를 되돌린다
            builder.addMessage(message);
            List<ContentBlockParam> results = new ArrayList<>();
            for (ContentBlock block : message.content()) {
                if (block.isToolUse()) {
                    ToolUseBlock tu = block.asToolUse();
                    String resultText = executeTool(familyId, tu);
                    results.add(ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                    .toolUseId(tu.id())
                                    .content(resultText)
                                    .build()));
                }
            }
            builder.addUserMessageOfBlockParams(results);
        }
        return "질문이 조금 복잡해요. 좀 더 구체적으로 물어봐 주실래요?";
    }

    /** 도구 실행 — familyId는 서버가 주입, memberId는 스코프 검증됨(CareChatToolService) */
    private String executeTool(Long familyId, ToolUseBlock tu) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> input = tu._input().convert(Map.class);
            if (input == null) input = Map.of();

            switch (tu.name()) {
                case "list_family_members":
                    return toJson(tools.listFamilyMembers(familyId));
                case "get_return_pattern": {
                    Long memberId = asLong(input.get("memberId"));
                    int days = input.get("days") == null ? 14 : asInt(input.get("days"));
                    return toJson(tools.getReturnPattern(familyId, memberId, days));
                }
                default:
                    return "알 수 없는 도구입니다: " + tu.name();
            }
        } catch (IllegalArgumentException e) {
            // 스코프 위반/잘못된 입력 등 — AI에게 사실대로 전달 (지어내지 않게)
            return "요청을 처리할 수 없습니다: " + e.getMessage();
        } catch (Exception e) {
            return "도구 실행 중 오류가 발생했습니다.";
        }
    }

    private String extractText(Message message) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                sb.append(block.asText().text());
            }
        }
        return sb.toString().isBlank() ? "죄송해요, 답변을 만들지 못했어요." : sb.toString();
    }

    private List<Tool> buildTools() {
        Tool listMembers = Tool.builder()
                .name("list_family_members")
                .description("현재 가족 구성원 목록(memberId, nickname)을 반환한다. "
                        + "사용자가 '엄마' 같은 호칭으로 물으면 먼저 이 도구로 memberId를 찾아라.")
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder().build())
                        .build())
                .build();

        Tool returnPattern = Tool.builder()
                .name("get_return_pattern")
                .description("특정 구성원의 최근 평균 귀가 시각을 조회한다. "
                        + "averageReturnHour는 24시간제 소수(예: 19.5=저녁 7시 반). sampleCount가 0이면 데이터 없음.")
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder()
                                .putAdditionalProperty("memberId", JsonValue.from(Map.of(
                                        "type", "integer",
                                        "description", "list_family_members로 얻은 구성원 id")))
                                .putAdditionalProperty("days", JsonValue.from(Map.of(
                                        "type", "integer",
                                        "description", "최근 며칠 기준 (기본 14, 최대 30)")))
                                .build())
                        .required(List.of("memberId"))
                        .build())
                .build();

        return List.of(listMembers, returnPattern);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }

    private int asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
