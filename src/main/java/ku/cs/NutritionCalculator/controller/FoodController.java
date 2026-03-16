// controller/FoodController.java
package ku.cs.NutritionCalculator.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import ku.cs.NutritionCalculator.repository.FoodRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ku.cs.NutritionCalculator.dto.ApiResponse;
import ku.cs.NutritionCalculator.dto.food.FoodCreateRequest;
import ku.cs.NutritionCalculator.dto.food.FoodsResponse;
import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.UserRepository;
import ku.cs.NutritionCalculator.service.FoodAiService;
import ku.cs.NutritionCalculator.service.FoodService;
import ku.cs.NutritionCalculator.service.ImageService;
import ku.cs.NutritionCalculator.service.UserDetailsImpl;

@RestController
@RequestMapping("/foods")
public class FoodController {

    private final FoodService foodService;
    private final FoodAiService foodAiService;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;

    public FoodController(FoodService foodService, FoodAiService foodAiService,
            ImageService imageService, UserRepository userRepository,
            FoodRepository foodRepository) {
        this.foodService = foodService;
        this.foodAiService = foodAiService;
        this.imageService = imageService;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
    }

    /**
     * วิเคราะห์อาหารด้วย AI + บันทึกลง database (รับ JSON)
     * สำหรับกรณีใส่ text อย่างเดียว
     */
    @PostMapping(value = "/analyze-text", consumes = "application/json")
    @Transactional
    public ResponseEntity<ApiResponse<Food_Logging>> analyzeFoodJson(
            @org.springframework.web.bind.annotation.RequestBody FoodCreateRequest request) {
        System.out.println("=== analyzeFoodJson (JSON endpoint) ===");
        System.out.println("Request: " + request);
        try {
            String text = request.getText();
            System.out.println("Text from request: " + text);
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "กรุณาใส่ข้อความ", null));
            }

            int dishType = request.getDish() > 0 ? request.getDish() : 1;
            System.out.println("Analyzing text (JSON): " + text + " | dishType: " + dishType);
            String aiResult = foodAiService.analyzeFood(text, dishType);
            System.out.println("AI Result: " + aiResult);

            // ดึง user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // สร้าง Food_Logging
            Food_Logging food = new Food_Logging();
            food.setDatetimeFood(request.getDatetimeFood() != null ? request.getDatetimeFood() : java.time.LocalDateTime.now());
            food.setDish(request.getDish() > 0 ? request.getDish() : 1);
            food.setText(text);
            food.setAi(aiResult);
            food.setUser(user);

            Food_Logging saved = foodService.createFood(food);
            System.out.println("Saved with ID: " + saved.getId());

            // รัน weekly analysis แล้วเก็บลง f_weekly ของ food ตัวนี้
            runAndSaveWeeklyAnalysis(saved, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "วิเคราะห์และบันทึกสำเร็จ", saved));

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && (msg.startsWith("NOT_FOOD:") || msg.startsWith("NOT_FOUND:") || msg.startsWith("PROFANITY:"))) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, msg.substring(msg.indexOf(':') + 1), null));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "AI วิเคราะห์ไม่สำเร็จ: " + msg, null));
        }
    }

    /**
     * วิเคราะห์อาหารด้วย AI + บันทึกลง database (รับ multipart/form-data)
     * รองรับ 3 กรณี:
     * 1. ใส่รูปอย่างเดียว -> AI วิเคราะห์รูป
     * 2. ใส่ text อย่างเดียว -> AI วิเคราะห์ text
     * 3. ใส่ทั้งรูปและ text -> AI วิเคราะห์รูป + text
     */
    /**
     * Step 1: ให้ AI อ่านรูปแล้วส่งชื่ออาหารกลับ (ยังไม่บันทึก)
     * user สามารถแก้ไขชื่อได้ก่อนกด confirm
     */
    @PostMapping(value = "/identify", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> identifyFood(
            @RequestPart(value = "image") MultipartFile image) {
        System.out.println("=== identifyFood ===");
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "กรุณาใส่รูปภาพ", null));
            }

            String imagePath = imageService.saveImage(image);
            System.out.println("Image saved to: " + imagePath);

            String identifyResult = foodAiService.identifyFoodFromImage(imagePath);
            System.out.println("Identify Result: " + identifyResult);

            return ResponseEntity.ok(new ApiResponse<>(true, "ระบุอาหารสำเร็จ", identifyResult));

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && (msg.startsWith("NOT_FOOD:") || msg.startsWith("NOT_FOUND:") || msg.startsWith("PROFANITY:"))) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, msg.substring(msg.indexOf(':') + 1), null));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "AI ระบุอาหารไม่สำเร็จ: " + msg, null));
        }
    }

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<ApiResponse<Food_Logging>> analyzeFood(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "text", required = false) String text) {
        System.out.println("=== analyzeFood (Multipart endpoint) ===");
        System.out.println("Image: " + (image != null ? image.getOriginalFilename() : "null"));
        System.out.println("Text: " + text);
        try {
            boolean hasImage = image != null && !image.isEmpty();
            boolean hasText = text != null && !text.trim().isEmpty();

            if (!hasImage && !hasText) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "กรุณาใส่รูปภาพหรือข้อความอย่างน้อย 1 อย่าง", null));
            }

            String imagePath = null;
            String aiResult;

            if (hasImage) {
                // บันทึกรูปก่อน
                imagePath = imageService.saveImage(image);
                System.out.println("Image saved to: " + imagePath);

                if (hasText) {
                    // กรณี 3: มีทั้งรูปและ text -> วิเคราะห์รูป + text
                    System.out.println("Analyzing image + text: " + text);
                    aiResult = foodAiService.analyzeFoodWithImageAndText(imagePath, text);
                } else {
                    // กรณี 1: มีรูปอย่างเดียว -> วิเคราะห์รูป
                    System.out.println("Analyzing image only");
                    aiResult = foodAiService.analyzeFood(imagePath);
                }
            } else {
                // กรณี 2: มี text อย่างเดียว -> วิเคราะห์ text
                System.out.println("Analyzing text only: " + text);
                aiResult = foodAiService.analyzeFood(text);
            }

            System.out.println("AI Result: " + aiResult);

            // ดึง user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // สร้าง Food_Logging
            Food_Logging food = new Food_Logging();
            food.setDatetimeFood(LocalDateTime.now());
            food.setDish(1);
            food.setImagePath(imagePath);
            food.setText(text);
            food.setAi(aiResult);
            food.setUser(user);

            Food_Logging saved = foodService.createFood(food);
            System.out.println("Saved with ID: " + saved.getId());

            // รัน weekly analysis แล้วเก็บลง f_weekly ของ food ตัวนี้
            runAndSaveWeeklyAnalysis(saved, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "วิเคราะห์และบันทึกสำเร็จ", saved));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "อ่านรูปภาพไม่สำเร็จ", null));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: " + e.getMessage());
            String msg = e.getMessage();
            if (msg != null && (msg.startsWith("NOT_FOOD:") || msg.startsWith("NOT_FOUND:") || msg.startsWith("PROFANITY:"))) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, msg.substring(msg.indexOf(':') + 1), null));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "AI วิเคราะห์ไม่สำเร็จ: " + msg, null));
        }
    }

    @PostMapping(consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<ApiResponse<Food_Logging>> createFood(
            @RequestPart("food") FoodCreateRequest foodCreateRequest,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            String imagePath = null;

            if (image != null && !image.isEmpty()) {
                imagePath = imageService.saveImage(image);
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Food_Logging createdFood = foodService.createFoodLogging(foodCreateRequest, imagePath, user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Food created successfully.", createdFood));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to save image", null));
        }
    }

    @GetMapping("/weekly-analysis")
    public ResponseEntity<ApiResponse<String>> getWeeklyAnalysis() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ดึง f_weekly จาก food ตัวล่าสุดของ user
            Food_Logging latestFood = foodRepository.findFirstByUserOrderByDatetimeFoodDesc(user);

            if (latestFood == null || latestFood.getWeekly() == null || latestFood.getWeekly().isEmpty()) {
                return ResponseEntity.ok(
                        new ApiResponse<>(false, "ไม่มีข้อมูลวิเคราะห์รายสัปดาห์", null));
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "วิเคราะห์สำเร็จ", latestFood.getWeekly()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "วิเคราะห์รายสัปดาห์ไม่สำเร็จ: " + e.getMessage(), null));
        }
    }

    /**
     * รัน weekly analysis แล้วเก็บผลลง f_weekly ของ food ตัวที่เพิ่งบันทึก
     */
    private void runAndSaveWeeklyAnalysis(Food_Logging savedFood, User user) {
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(7);

            List<Food_Logging> weeklyFoods = foodRepository
                    .findByUserAndDatetimeFoodBetweenOrderByDatetimeFoodAsc(user, start, end);

            if (!weeklyFoods.isEmpty()) {
                String analysis = foodAiService.analyzeWeeklyFood(user, weeklyFoods);
                savedFood.setWeekly(analysis);
                foodRepository.save(savedFood);
                System.out.println("Weekly analysis saved to food: " + savedFood.getId());
            }
        } catch (Exception e) {
            System.err.println("Weekly analysis failed (non-blocking): " + e.getMessage());
        }
    }

    @GetMapping
    public List<FoodsResponse> getFoods() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return foodService.getFoodsByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FoodsResponse toResponse(Food_Logging food) {
        FoodsResponse res = new FoodsResponse();
        res.setId(food.getId());
        res.setDatetimeFood(food.getDatetimeFood());
        res.setDish(food.getDish());
        res.setImagePath(food.getImagePath());
        res.setText(food.getText());
        res.setAi(food.getAi());
        res.setUserId(food.getUser().getId());
        return res;
    }
}