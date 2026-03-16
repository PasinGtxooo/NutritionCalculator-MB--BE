package ku.cs.NutritionCalculator.dto.food;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class FoodsResponse {
    private UUID id;
    private LocalDateTime datetimeFood;
    private int dish;
    private String imagePath;
    private String text;
    private String ai;
    private String weekly;
    private UUID userId;
}
