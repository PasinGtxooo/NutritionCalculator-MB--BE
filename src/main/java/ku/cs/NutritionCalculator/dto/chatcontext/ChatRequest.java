package ku.cs.NutritionCalculator.dto.chatcontext;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String imageBase64;
}
