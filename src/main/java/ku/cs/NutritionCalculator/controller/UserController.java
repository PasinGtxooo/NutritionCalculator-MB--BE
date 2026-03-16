package ku.cs.NutritionCalculator.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import ku.cs.NutritionCalculator.dto.ApiResponse;
import ku.cs.NutritionCalculator.dto.user.UpdateUserRequest;
import ku.cs.NutritionCalculator.dto.user.UserResponse;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.service.UserService;
import ku.cs.NutritionCalculator.utils.JwtUtils;

@RestController
public class UserController {
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

        List<UserResponse> users = userService.getAllCustomers()
                .stream()
                .map(user -> {
                    UserResponse res = new UserResponse();
                    res.setId(user.getId());
                    res.setUsername(user.getUsername());
                    res.setPhone(user.getPhone());
                    res.setFirstname(user.getFirstname());
                    res.setLastname(user.getLastname());
                    res.setSex(user.getSex());
                    res.setWeight(user.getWeight());
                    res.setHeight(user.getHeight());
                    res.setActivitylevel(user.getActivitylevel());
                    res.setMaingoal(user.getMaingoal());
                    res.setAlcohol(user.isAlcohol());
                    res.setSmoking(user.isSmoking());
                    res.setSweetlevel(user.getSweetlevel());
                    res.setSaltylevel(user.getSaltylevel());
                    res.setSourlevel(user.getSourlevel());
                    res.setBmr(user.getBmr());
                    res.setTdee(user.getTdee());
                    res.setBreakfastTime(user.getBreakfastTime());
                    res.setLunchTime(user.getLunchTime());
                    res.setDinnerTime(user.getDinnerTime());
                    res.setBodyfat(user.getBodyfat());
                    res.setBodyfat(user.getBodyfat());
                    res.setBirthDate(user.getBirthDate());
                    res.setRole(user.getRole());
                    return res;
                })
                .toList();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Users retrieved successfully.", users));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable UUID id) {
        Optional<User> optionalUser = userService.getUserById(id);
        return optionalUser
                .map(user -> ResponseEntity.ok(new ApiResponse<>(true, "User retrieved successfully.", user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "User not found.", null)));
    }

    @GetMapping("/user/jwt")
    public ResponseEntity<ApiResponse<UserResponse>> getUsernameByJwt(@RequestHeader("Authorization") String jwt) {
        String token = jwt.replace("Bearer ", "");
        String username;
        try {
            username = jwtUtils.getUserNameFromJwtToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Token invalid or expired.", null));
        }
        Optional<User> optionalUser = userService.getUserByUsername(username);
        UserResponse res = new UserResponse();
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            res.setUsername(user.getUsername());
            res.setId(user.getId());
            res.setRole(user.getRole());
            res.setPhone(user.getPhone());
            res.setSex(user.getSex());
            res.setFirstname(user.getFirstname());
            res.setLastname(user.getLastname());

            res.setWeight(user.getWeight());
            res.setHeight(user.getHeight());
            res.setActivitylevel(user.getActivitylevel());
            res.setMaingoal(user.getMaingoal());
            res.setAlcohol(user.isAlcohol());
            res.setSmoking(user.isSmoking());
            res.setSweetlevel(user.getSweetlevel());
            res.setSaltylevel(user.getSaltylevel());
            res.setSourlevel(user.getSourlevel());
            res.setBmr(user.getBmr());
            res.setTdee(user.getTdee());
            res.setBreakfastTime(user.getBreakfastTime());
            res.setLunchTime(user.getLunchTime());
            res.setDinnerTime(user.getDinnerTime());
            res.setBodyfat(user.getBodyfat());
            res.setBirthDate(user.getBirthDate());
            res.setRole(user.getRole());
            return ResponseEntity.ok(new ApiResponse<>(true, "User details retrieved successfully.", res));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "User not found.", null));
    }

    @PatchMapping("/user/jwt")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserByJwt(
            @RequestHeader("Authorization") String jwt,
            @RequestBody UpdateUserRequest req) {
        String token = jwt.replace("Bearer ", "");
        String username = jwtUtils.getUserNameFromJwtToken(token);

        User updatedUser = userService.updateUserByUsername(username, req);

        UserResponse res = new UserResponse();
        res.setPhone(updatedUser.getPhone());
        res.setWeight(updatedUser.getWeight());
        res.setHeight(updatedUser.getHeight());
        res.setActivitylevel(updatedUser.getActivitylevel());
        res.setMaingoal(updatedUser.getMaingoal());
        res.setAlcohol(updatedUser.isAlcohol());
        res.setSmoking(updatedUser.isSmoking());
        res.setSweetlevel(updatedUser.getSweetlevel());
        res.setSaltylevel(updatedUser.getSaltylevel());
        res.setSourlevel(updatedUser.getSourlevel());
        res.setBreakfastTime(updatedUser.getBreakfastTime());
        res.setLunchTime(updatedUser.getLunchTime());
        res.setDinnerTime(updatedUser.getDinnerTime());
        res.setBodyfat(updatedUser.getBodyfat());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User updated successfully.", res));
    }
}
