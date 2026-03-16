package ku.cs.NutritionCalculator.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ku.cs.NutritionCalculator.dto.food.FoodCreateRequest;
import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.FoodRepository;

@Service
public class FoodService {
    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
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
