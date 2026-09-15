package com.saasai.service;

import com.saasai.entity.User;
import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.feature.ai.AiCompletionService;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.feature.ai.AiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private AiCompletionService aiCompletionService;

    @InjectMocks
    private AiService aiService;

    @Test
    void processCompletion_shouldDelegateToAiCompletionService() {
        User user = new User();
        user.setUserId("user-uuid-1");

        AiCompletionRequestDTO request = AiCompletionRequestDTO.builder()
                .sessionUuid("session-uuid-1")
                .promptCommand("Rewrite")
                .userText("Prompt text")
                .model("gpt-4")
                .pinEditorContext(false)
                .build();

        AiProviderResultDTO expected = AiProviderResultDTO.builder()
                .content("Generated content")
                .model("gpt-4")
                .finishReason("stop")
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .build();

        when(aiCompletionService.complete(user, request))
                .thenReturn(expected);

        AiProviderResultDTO result =
                aiService.processCompletion(user, request);

        assertThat(result).isSameAs(expected);
        verify(aiCompletionService).complete(user, request);
    }
}