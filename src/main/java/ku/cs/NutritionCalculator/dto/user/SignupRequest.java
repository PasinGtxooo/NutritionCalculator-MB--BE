package ku.cs.NutritionCalculator.dto.user;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ku.cs.NutritionCalculator.entity.Sex;
import lombok.Data;

@Data
public class SignupRequest {

    // ===== Account =====
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric only")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20)
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "\\d+", message = "Phone must be numeric")
    @Size(min = 10, max = 15)
    private String phone;

    // ===== Personal Info =====
    @NotBlank(message = "Firstname is required")
    private String firstname;

    @NotBlank(message = "Lastname is required")
    private String lastname;

    @NotNull(message = "Sex is required")
    private Sex sex;

    // ===== Body =====
    @Positive(message = "Weight must be positive")
    private float weight;

    @Positive(message = "Height must be positive")
    private float height;

    // ===== Activity & Goal =====
    @Min(1)
    @Max(5)
    private int activitylevel;

    @NotBlank(message = "Main goal is required")
    private String maingoal;
    // ===== Nutrition =====

    @NotNull(message = "Meal type is required")
    private LocalTime breakfastTime;
    @NotNull(message = "Meal type is required")
    private LocalTime lunchTime;
    @NotNull(message = "Meal type is required")
    private LocalTime dinnerTime;

    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;

    private float bodyfat;
    private boolean alcohol;
    private boolean smoking;
    @Min(1)
    @Max(5)
    private int sweetlevel;
    @Min(1)
    @Max(5)
    private int saltylevel;
    @Min(1)
    @Max(5)
    private int sourlevel;
    // ===== Role =====
    private String role = "CUSTOMER";

    public int getAge() {
        if (birthDate == null)
            return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public float getPAL() {
        return switch (activitylevel) {
            case 1 -> 1.2f;
            case 2 -> 1.375f;
            case 3 -> 1.45f;
            case 4 -> 1.55f;
            case 5 -> 1.725f;
            default -> 1.0f;
        };
    }
}
