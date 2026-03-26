package ku.cs.NutritionCalculator.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import ku.cs.NutritionCalculator.dto.food.FoodCreateRequest;
import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.FoodRepository;

@Service
public class FoodService {
    private final FoodRepository foodRepository;
    private final FoodAiService foodAiService;

    public FoodService(FoodRepository foodRepository, FoodAiService foodAiService) {
        this.foodRepository = foodRepository;
        this.foodAiService = foodAiService;
    }

    public void runAndSaveWeeklyAnalysis(Food_Logging savedFood, User user) {
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(7);
            List<Food_Logging> weeklyFoods = foodRepository
                    .findByUserAndDatetimeFoodBetweenOrderByDatetimeFoodAsc(user, start, end);
            if (!weeklyFoods.isEmpty()) {
                String analysis = foodAiService.analyzeWeeklyFood(user, weeklyFoods);
                savedFood.setWeekly(analysis);
                foodRepository.save(savedFood);
            }
        } catch (Exception e) {
            System.err.println("Weekly analysis failed: " + e.getMessage());
        }
    }

    public Food_Logging createFood(Food_Logging foodLogging) {
        return foodRepository.save(foodLogging);
    }

    public Food_Logging createFoodLogging(
            FoodCreateRequest req,
            String imagePath,
            User user) {
        Food_Logging food = new Food_Logging();
        food.setDatetimeFood(req.getDatetimeFood());
        food.setDish(req.getDish());
        food.setText(req.getText());
        food.setAi(req.getAi());
        food.setImagePath(imagePath);
        food.setUser(user);

        return foodRepository.save(food);
    }

    public List<Food_Logging> getAllFoods() {
        return foodRepository.findAll();
    }

    public List<Food_Logging> getFoodsByUser(User user) {
        return foodRepository.findByUserOrderByDatetimeFoodDesc(user);
    }
}
