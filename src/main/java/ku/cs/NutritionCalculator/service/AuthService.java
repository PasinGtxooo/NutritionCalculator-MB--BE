package ku.cs.NutritionCalculator.service;

import ku.cs.NutritionCalculator.dto.ApiResponse;
import ku.cs.NutritionCalculator.dto.user.SigninRequest;
import ku.cs.NutritionCalculator.dto.user.SigninResponse;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    ApiResponse<SigninResponse> signIn(SigninRequest signinRequest);
    Boolean validateToken(String token);
}