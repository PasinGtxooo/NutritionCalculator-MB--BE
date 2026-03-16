package ku.cs.NutritionCalculator.dto.user;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import ku.cs.NutritionCalculator.entity.Sex;
import lombok.Data;

@Data
public class SigninResponse {
    private String token;
    private UUID id;
    private String username;
    private String phone;
    private String password;
    private Sex sex;
    private String firstname;
    private String lastname;
    private float weight;
    private float height;
    private int activitylevel;
    private String maingoal;
    private boolean alcohol;
    private boolean smoking;
    private int sweetlevel;
    private int saltylevel;
    private int sourlevel;
    private float bmr;
    private float tdee;
    private LocalTime breakfastTime;
    private LocalTime lunchTime;
    private LocalTime dinnerTime;
    private float bodyfat;
    private LocalDate birthDate;
    private String role;

}
