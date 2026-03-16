package ku.cs.NutritionCalculator.dto.user;

import java.time.LocalTime;

import lombok.Data;
@Data
public class UpdateUserRequest {
    private String phone;
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
    private LocalTime breakfastTime;
    private LocalTime lunchTime;
    private LocalTime dinnerTime;
    private float bodyfat;
}
