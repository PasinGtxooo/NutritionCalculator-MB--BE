package ku.cs.NutritionCalculator.dto.chatcontext;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String message;
    private FoodAnalysisResponse foodAnalysis; // ถ้าวิเคราะห์อาหาร
}
