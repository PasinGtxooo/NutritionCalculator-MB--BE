package ku.cs.NutritionCalculator.dto.chatcontext;

import java.util.List;

import lombok.Data;

@Data
public class FoodAnalysisResponse {
    private String name;
    private String nameEn;
    private List<String> ingredients;
    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fat;
    private Integer fiber;
    private Integer sodium;
    private String description;
}
