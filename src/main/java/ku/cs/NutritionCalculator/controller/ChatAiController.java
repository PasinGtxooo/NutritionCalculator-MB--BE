package ku.cs.NutritionCalculator.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ku.cs.NutritionCalculator.dto.ApiResponse;
import ku.cs.NutritionCalculator.dto.chatcontext.ChatRequest;
import ku.cs.NutritionCalculator.dto.chatcontext.ChatResponse;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.UserRepository;
import ku.cs.NutritionCalculator.service.ChatAiService;
import ku.cs.NutritionCalculator.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/ai")
public class ChatAiController {

    @Autowired
    private ChatAiService chatAiService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String aiMessage = chatAiService.chat(user, request.getMessage());
            ChatResponse response = ChatResponse.builder()
                    .message(aiMessage)
                    .build();
            return ResponseEntity.ok(new ApiResponse<>(true, "success", response));
        } catch (Exception e) {
            System.err.println("Chat API ERROR: " + e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("PROFANITY:")) {
                String userMsg = msg.substring("PROFANITY:".length());
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, userMsg,
                                ChatResponse.builder().message(userMsg).build()));
            }
            e.printStackTrace();
            ChatResponse errorResponse = ChatResponse.builder()
                    .message("Error: " + msg)
                    .build();
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, msg, errorResponse));
        }
    }

    @GetMapping("/chat/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Map<String, Object>> history = chatAiService.getHistory(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "success", history));
        } catch (Exception e) {
            System.err.println("Chat History ERROR: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/chat/history")
    public ResponseEntity<ApiResponse<String>> clearHistory() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            chatAiService.clearHistory(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "ล้างประวัติสำเร็จ", null));
        } catch (Exception e) {
            System.err.println("Clear History ERROR: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
