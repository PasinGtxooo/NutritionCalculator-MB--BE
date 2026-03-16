package ku.cs.NutritionCalculator.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.User;

public interface FoodRepository extends JpaRepository<Food_Logging, UUID> {
    List<Food_Logging> findByUserOrderByDatetimeFoodDesc(User user);

    List<Food_Logging> findByUserAndDatetimeFoodBetweenOrderByDatetimeFoodAsc(
            User user, LocalDateTime start, LocalDateTime end);

    Food_Logging findFirstByUserOrderByDatetimeFoodDesc(User user);
}
