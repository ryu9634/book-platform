package com.ryu.ebook.user.chat.client;

import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.anthropic.models.messages.OutputConfig;
import com.ryu.ebook.user.chat.config.ChatProperties;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

// Spring AI로 Anthropic Claude를 부릅니다. 모델 쪽 사정을 아는 코드는 이 클래스 하나로 모읍니다.
//
// 대화를 이어 붙이는 방식: Spring AI의 ChatMemory를 쓰지 않고 지난 메시지를 직접 실어 보냅니다.
// 이미 대화를 CHAT_MESSAGE 테이블에 남기고 있어 저장소가 두 벌 필요 없고,
// 무엇을 다시 보내는지(도구 호출 기록과 추론 블록은 보내지 않는다는 것)를 코드에서 그대로 읽을 수 있기 때문입니다.
@Slf4j
@Component
public class AnthropicChatCompletionPort implements ChatCompletionPort {

    private final ChatClient chatClient;
    private final EbookSearchTools ebookSearchTools;
    private final ChatProperties chatProperties;

    public AnthropicChatCompletionPort(ChatClient.Builder chatClientBuilder, EbookSearchTools ebookSearchTools,
            ChatProperties chatProperties) {
        this.chatClient = chatClientBuilder.build();
        this.ebookSearchTools = ebookSearchTools;
        this.chatProperties = chatProperties;
    }

    @Override
    public ChatCompletion answer(String systemPrompt, List<ChatTurn> history, String question, Long memberId) {
        ChatToolSession toolSession = new ChatToolSession(memberId, chatProperties.maxToolCalls());
        try {
            ChatResponse response = chatClient.prompt()
                    .options(options())
                    .system(systemPrompt)
                    .messages(toMessages(history))
                    .user(question)
                    .tools(ebookSearchTools)
                    .toolContext(Map.of(ChatToolSession.KEY, toolSession))
                    .call()
                    .chatResponse();
            return toCompletion(response, toolSession);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            // 예외 메시지에는 요청 본문이나 URL이 섞일 수 있어 종류만 남깁니다.
            log.warn("chat model call failed. memberId={}, cause={}", memberId, e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.CHAT_ANSWER_FAILED);
        }
    }

    // ChatClient.options()는 만들어진 옵션이 아니라 빌더를 받습니다.
    // temperature, top-p, top-k는 넣지 않습니다. Claude 5 세대는 기본값이 아닌 값을 받으면 400으로 거절합니다.
    // 추론(thinking)은 기본값인 적응형으로 두고 깊이만 effort로 낮춥니다. 도구를 쓰는 대화에서 추론을 끄면
    // 모델이 도구 호출을 본문에 글로 써 버리는 일이 있어서, 끄는 대신 LOW로 얕게 씁니다.
    private AnthropicChatOptions.Builder options() {
        return AnthropicChatOptions.builder()
                .model(chatProperties.model())
                .maxTokens(chatProperties.maxOutputTokens())
                .effort(OutputConfig.Effort.of(chatProperties.effort()));
    }

    private List<Message> toMessages(List<ChatTurn> history) {
        return history.stream()
                .map(turn -> turn.fromUser()
                        ? (Message) new UserMessage(turn.content())
                        : new AssistantMessage(turn.content()))
                .toList();
    }

    private ChatCompletion toCompletion(ChatResponse response, ChatToolSession toolSession) {
        if (response == null || response.getResult() == null) {
            throw new BusinessException(ErrorCode.CHAT_ANSWER_FAILED);
        }
        String finishReason = response.getResult().getMetadata() == null ? null
                : response.getResult().getMetadata().getFinishReason();
        boolean rejected = "refusal".equalsIgnoreCase(finishReason);
        boolean truncated = "max_tokens".equalsIgnoreCase(finishReason);
        TokenUsage usage = toUsage(response);

        String text = response.getResult().getOutput() == null ? null : response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            // 거절은 HTTP 200에 빈 본문으로 오므로, 본문이 비었다고 바로 실패로 보지 않습니다.
            if (rejected) {
                return new ChatCompletion("", usage, true, false, List.of());
            }
            throw new BusinessException(ErrorCode.CHAT_ANSWER_FAILED);
        }
        return new ChatCompletion(text.trim(), usage, rejected, truncated, toolSession.shownBooks());
    }

    // 캐시 토큰은 프롬프트 캐싱을 켠 뒤에 채웁니다. 지금은 입력·출력만 셉니다.
    private TokenUsage toUsage(ChatResponse response) {
        if (response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return TokenUsage.NONE;
        }
        var usage = response.getMetadata().getUsage();
        Integer promptTokens = usage.getPromptTokens();
        Integer completionTokens = usage.getCompletionTokens();
        return TokenUsage.of(promptTokens == null ? 0 : promptTokens, completionTokens == null ? 0 : completionTokens);
    }
}
