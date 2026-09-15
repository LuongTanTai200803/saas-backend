package com.saasai.feature.ai.openrouter;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Một message trong cuộc hội thoại gửi tới OpenRouter.
 *
 * Ví dụ:
 *
 * {
 *   "role": "system",
 *   "content": "Bạn là chuyên gia soạn thảo văn bản..."
 * }
 *
 * {
 *   "role": "user",
 *   "content": "Hãy soạn nghị quyết..."
 * }
 */

public record OpenRouterMessageDTO(

    /**
     * Vai trò của message.
     *
     * Các giá trị thường dùng:
     * - system
     * - user
     * - assistant
     */
    String role,

    /**
     * Nội dung của message.
     */
    String content
){
}
