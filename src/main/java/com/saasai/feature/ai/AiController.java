package com.saasai.feature.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.feature.ai.ApiResponseDTO;

import com.saasai.dto.ExportRequestDTO;
import com.saasai.entity.User;
import com.saasai.service.UserService;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private UserService userService;

    private AiCompletionService aiCompletionService;

    public AiController(AiCompletionService aiCompletionService) {
        this.aiCompletionService = aiCompletionService;
    }

    @PostMapping("/completions")
    public ResponseEntity<ApiResponseDTO<AiProviderResultDTO>> complete(
            @RequestBody AiCompletionRequestDTO request
    ) {

        String userId = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userService.getUserById(userId);

        AiProviderResultDTO result =
                aiCompletionService.complete(user, request);

        return ResponseEntity.ok(
                ApiResponseDTO.success(result)
        );
    }
    
    @PostMapping("/workspace/refine")
        public ResponseEntity<ApiResponseDTO<AiProviderResultDTO>> refineWorkspace(
                @RequestBody AiCompletionRequestDTO request
        ) {
        String userId = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userService.getUserById(userId);

        AiProviderResultDTO result =
                aiCompletionService.refineWorkspace(user, request);

        return ResponseEntity.ok(ApiResponseDTO.success(result));
        }

}