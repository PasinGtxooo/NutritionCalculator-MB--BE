package ku.cs.NutritionCalculator.dto.user;

import lombok.Data;

@Data
public class SigninRequest {
    private String username;
    private String password;
}
