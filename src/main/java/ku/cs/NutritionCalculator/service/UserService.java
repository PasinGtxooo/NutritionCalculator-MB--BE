package ku.cs.NutritionCalculator.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ku.cs.NutritionCalculator.dto.ApiResponse;
import ku.cs.NutritionCalculator.dto.user.SignupRequest;
import ku.cs.NutritionCalculator.dto.user.SignupResponse;
import ku.cs.NutritionCalculator.dto.user.UpdateUserRequest;
import ku.cs.NutritionCalculator.entity.Sex;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public ApiResponse<SignupResponse> createUser(SignupRequest user) {
        SignupResponse signupResponse = new SignupResponse();
        Optional<User> existedUser = userRepository.findByUsername(user.getUsername());

        if (existedUser.isPresent()) {
            signupResponse.setMessage("Username already exists");
            return new ApiResponse<>(false, signupResponse.getMessage(), signupResponse);
        }

        String username = user.getUsername();
        String phone = user.getPhone();
        String password = user.getPassword();
        String confirmPassword = user.getConfirmPassword();
        String role = (user.getRole() != null && !user.getRole().isEmpty()) ? user.getRole() : "CUSTOMER";

        if (!password.equals(confirmPassword)) {
            signupResponse.setMessage("Passwords do not match");
            return new ApiResponse<>(false, signupResponse.getMessage(), signupResponse);
        }

        String encodedPassword = passwordEncoder.encode(password);

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setPhone(user.getPhone());
        newUser.setPassword(encodedPassword);
        newUser.setFirstname(user.getFirstname());
        newUser.setLastname(user.getLastname());
        newUser.setSex(user.getSex());
        newUser.setWeight(user.getWeight());
        newUser.setHeight(user.getHeight());
        newUser.setActivitylevel(user.getActivitylevel());
        newUser.setMaingoal(user.getMaingoal());
        newUser.setBreakfastTime(user.getBreakfastTime());
        newUser.setLunchTime(user.getLunchTime());
        newUser.setDinnerTime(user.getDinnerTime());
        newUser.setBirthDate(user.getBirthDate());
        newUser.setRole(user.getRole());
        newUser.setAlcohol(user.isAlcohol());
        newUser.setSmoking(user.isSmoking());
        newUser.setSweetlevel(user.getSweetlevel());
        newUser.setSaltylevel(user.getSaltylevel());
        newUser.setSourlevel(user.getSourlevel());
        newUser.setBodyfat(user.getBodyfat());
        if (user.getBodyfat() > 0) {
            newUser.setBmr((float) (370 + 21.6 * (1 - user.getBodyfat() / 100) * user.getWeight()));
        } else {
            if (user.getSex() == Sex.MALE) {
                newUser.setBmr((float) ((10 * user.getWeight()) + (6.25 * user.getHeight()) - (5 * user.getAge()) + 5));
            } else if (user.getSex() == Sex.FEMALE) {
                newUser.setBmr(
                        (float) ((10 * user.getWeight()) + (6.25 * user.getHeight()) - (5 * user.getAge()) + 5) - 161);
            }
        }
        if (newUser.getBmr() > 0) {
            newUser.setTdee(newUser.getBmr() * user.getPAL());
        }
        
        userRepository.save(newUser);

        signupResponse.setMessage("User created");
        return new ApiResponse<>(true, signupResponse.getMessage(), signupResponse);
    }

    public List<User> getAllCustomers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public User updateUserByUsername(String username, UpdateUserRequest req) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getFirstname() != null)
            user.setFirstname(req.getFirstname());
        if (req.getLastname() != null)
            user.setLastname(req.getLastname());
        if (req.getPhone() != null)
            user.setPhone(req.getPhone());

        if (req.getWeight() > 0)
            user.setWeight(req.getWeight());
        if (req.getHeight() > 0)
            user.setHeight(req.getHeight());
        if (req.getActivitylevel() > 0)
            user.setActivitylevel(req.getActivitylevel());
        if (req.getMaingoal() != null)
            user.setMaingoal(req.getMaingoal());

        if (req.getSweetlevel() > 0)
            user.setSweetlevel(req.getSweetlevel());
        if (req.getSaltylevel() > 0)
            user.setSaltylevel(req.getSaltylevel());
        if (req.getSourlevel() > 0)
            user.setSourlevel(req.getSourlevel());
        if (req.getBodyfat() > 0)
            user.setBodyfat(req.getBodyfat());

        if (req.getBreakfastTime() != null)
            user.setBreakfastTime(req.getBreakfastTime());
        if (req.getLunchTime() != null)
            user.setLunchTime(req.getLunchTime());
        if (req.getDinnerTime() != null)
            user.setDinnerTime(req.getDinnerTime());

        user.setAlcohol(req.isAlcohol());
        user.setSmoking(req.isSmoking());
        return userRepository.save(user);
    }

}
