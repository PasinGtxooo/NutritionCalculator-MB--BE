// service/FoodAiService.java
package ku.cs.NutritionCalculator.service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.User;

@Service
public class FoodAiService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${usda.api.key:}")
    private String usdaApiKey;

    @Autowired
    private NutritionDataService nutritionDataService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String USDA_SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";

    // ใช้ llama-4-scout สำหรับทั้งรูปและ text (รองรับ Vision)
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String TEXT_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private static final java.util.Set<String> BANNED_WORDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "เหี้ย", "สัส", "เชี่ย", "ห่า", "ระยำ", "เย็ด", "ควย", "หี", "เงี่ยน",
        "fuck", "fucking", "shit", "bullshit", "asshole", "bitch", "bastard",
        "damn", "dick", "pussy", "slut", "whore", "motherfucker", "mf"
    ));

    private void checkProfanity(String text) {
        if (text == null) return;
        String lower = text.toLowerCase();
        for (String word : BANNED_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                throw new RuntimeException("PROFANITY:กรุณาใช้คำสุภาพในการกรอกข้อมูลอาหาร");
            }
        }
    }

    // Prompt ระบุเมนูละเอียด + แตก ingredients (ใช้ชื่อตามฐานข้อมูลอาหารไทย)
    private static final String IDENTIFY_PROMPT = "ก่อนอื่น ตรวจสอบว่าข้อความที่ได้รับเป็นชื่ออาหารหรือเมนูอาหารจริงหรือไม่ "
            + "ถ้าไม่ใช่อาหาร (เช่น ชื่อคน ชื่อสัตว์ ของใช้ วัตถุ หรือสิ่งที่ไม่สามารถกินได้) "
            + "ให้ตอบ: {\"isFood\":false,\"message\":\"ข้อมูลที่ใส่ไม่ใช่อาหาร กรุณาใส่ชื่อเมนูอาหารเท่านั้น\"} "
            + "ถ้าเป็นอาหาร ให้ระบุชื่อเมนูอาหารให้ละเอียด (เช่น ข้าวผัดหมู ไม่ใช่แค่ ข้าวผัด) "
            + "แล้วแตกส่วนประกอบ (ingredients) พร้อมปริมาณต่อ 1 จาน (กรัม) "
            + "สำคัญมาก: ชื่อ ingredient ต้องใช้ชื่อตามฐานข้อมูลอาหารไทย เช่น "
            + "'ข้าวเจ้า, นึ่ง' แทน 'ข้าวสวย', 'น้ำมัน, ถั่วเหลือง' แทน 'น้ำมันพืช' "
            + "ตอบเป็น JSON เท่านั้น ห้ามตอบอย่างอื่น ตัวอย่าง: "
            + "{\"isFood\":true,\"dishName\":\"ข้าวผัดหมู\",\"dishNameEn\":\"Pork Fried Rice\","
            + "\"ingredients\":[{\"name\":\"ข้าวเจ้า, นึ่ง\",\"nameEn\":\"rice steamed\",\"amount\":200},"
            + "{\"name\":\"หมู, สันนอก\",\"nameEn\":\"pork loin\",\"amount\":50},"
            + "{\"name\":\"ไข่ไก่, ทั้งฟอง\",\"nameEn\":\"egg whole\",\"amount\":50},"
            + "{\"name\":\"น้ำมัน, ถั่วเหลือง\",\"nameEn\":\"soybean oil\",\"amount\":15}]}";

    // Prompt สำหรับโหมดกับข้าว — รองรับหลายเมนูและหน่วยวัดไทย
    private static final String GUBKHAO_PROMPT = "ก่อนอื่น ตรวจสอบว่าข้อความที่ได้รับเป็นรายการอาหารจริงหรือไม่ "
            + "ถ้าไม่ใช่อาหาร ให้ตอบ: {\"isFood\":false,\"message\":\"ข้อมูลที่ใส่ไม่ใช่อาหาร กรุณาใส่ชื่อเมนูอาหารเท่านั้น\"} "
            + "ถ้าเป็นอาหาร: ผู้ใช้กินกับข้าวหลายเมนู ระบุเป็นชื่ออาหารพร้อมจำนวนและหน่วย\n"
            + "หน่วยวัดไทยและการแปลงเป็นกรัม (ใช้ตามบริบทของอาหาร):\n"
            + "- ทัพพี (ข้าว) = 150 กรัม\n"
            + "- ไม้ (ไก่ย่าง/หมูปิ้ง) = 60 กรัม\n"
            + "- ช้อนทาน/ช้อนโต๊ะ (แกง/กับข้าว) = 50 กรัม\n"
            + "- ช้อนชา (น้ำมัน/ซอส) = 5 กรัม\n"
            + "- ถ้วย = 200 กรัม\n"
            + "- จาน/ชาม = 1 serving ปกติ\n"
            + "- ชิ้น (เนื้อ/ผัก) = 30 กรัม\n"
            + "งาน: แยกแต่ละรายการอาหาร แตก ingredients พร้อมปริมาณ (กรัม) ตามหน่วยที่ระบุ\n"
            + "ถ้าไม่มีหน่วย ให้ใช้ปริมาณ 1 จานปกติ\n"
            + "สำคัญ: ชื่อ ingredient ใช้ชื่อตามฐานข้อมูลอาหารไทย เช่น 'ข้าวเจ้า, นึ่ง' แทน 'ข้าวสวย'\n"
            + "ตอบเป็น JSON เท่านั้น ห้ามตอบอย่างอื่น ตัวอย่าง input: 'ข้าว 2 ทัพพี ไก่ยาง 2 ไม้ แกงเขียวหวาน 2 ช้อน'\n"
            + "ตัวอย่าง output: "
            + "{\"dishName\":\"กับข้าว: ข้าว ไก่ยาง แกงเขียวหวาน\","
            + "\"dishNameEn\":\"Thai Side Dishes\","
            + "\"ingredients\":["
            + "{\"name\":\"ข้าวเจ้า, นึ่ง\",\"nameEn\":\"rice steamed\",\"amount\":300},"
            + "{\"name\":\"ไก่ย่าง\",\"nameEn\":\"grilled chicken\",\"amount\":120},"
            + "{\"name\":\"แกงเขียวหวานไก่\",\"nameEn\":\"green curry chicken\",\"amount\":100}]}";


    /**
     * วิเคราะห์อาหารรายสัปดาห์ — aggregate รายวันแล้วส่งให้ AI วิเคราะห์
     */
    public String analyzeWeeklyFood(User user, List<Food_Logging> weeklyFoods) {
        try {
            // 1. Aggregate ข้อมูลรายวัน
            Map<LocalDate, Map<String, Object>> dailyData = new LinkedHashMap<>();

            for (Food_Logging food : weeklyFoods) {
                LocalDate date = food.getDatetimeFood().toLocalDate();
                Map<String, Object> dayEntry = dailyData.computeIfAbsent(date, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("totalCalories", 0.0);
                    m.put("protein", 0.0);
                    m.put("carbs", 0.0);
                    m.put("fat", 0.0);
                    m.put("foods", new ArrayList<String>());
                    return m;
                });

                // Parse AI JSON ของแต่ละมื้อ
                if (food.getAi() != null && !food.getAi().isEmpty()) {
                    try {
                        Map<String, Object> aiData = objectMapper.readValue(food.getAi(), Map.class);
                        dayEntry.put("totalCalories",
                                ((Number) dayEntry.get("totalCalories")).doubleValue()
                                        + toDouble(aiData.get("calories")));
                        dayEntry.put("protein",
                                ((Number) dayEntry.get("protein")).doubleValue()
                                        + toDouble(aiData.get("protein")));
                        dayEntry.put("carbs",
                                ((Number) dayEntry.get("carbs")).doubleValue()
                                        + toDouble(aiData.get("carbs")));
                        dayEntry.put("fat",
                                ((Number) dayEntry.get("fat")).doubleValue()
                                        + toDouble(aiData.get("fat")));

                        String foodName = (String) aiData.get("name");
                        if (foodName != null) {
                            ((List<String>) dayEntry.get("foods")).add(foodName);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse ai JSON for food: " + food.getId());
                    }
                }
            }

            // 2. สร้าง JSON สรุปส่ง AI
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("userInfo", Map.of(
                    "tdee", user.getTdee(),
                    "bmr", user.getBmr(),
                    "maingoal", user.getMaingoal() != null ? user.getMaingoal() : "ไม่ระบุ",
                    "weight", user.getWeight(),
                    "height", user.getHeight()));
            // แปลง LocalDate key เป็น String
            Map<String, Object> weeklyMap = new LinkedHashMap<>();
            for (Map.Entry<LocalDate, Map<String, Object>> entry : dailyData.entrySet()) {
                Map<String, Object> rounded = new HashMap<>(entry.getValue());
                rounded.put("totalCalories", Math.round(((Number) rounded.get("totalCalories")).doubleValue()));
                rounded.put("protein", Math.round(((Number) rounded.get("protein")).doubleValue()));
                rounded.put("carbs", Math.round(((Number) rounded.get("carbs")).doubleValue()));
                rounded.put("fat", Math.round(((Number) rounded.get("fat")).doubleValue()));
                weeklyMap.put(entry.getKey().toString(), rounded);
            }
            summaryData.put("weeklyData", weeklyMap);

            String dataJson = objectMapper.writeValueAsString(summaryData);

            // 3. เรียก Groq API
            String prompt = "คุณเป็นนักโภชนาการ วิเคราะห์ข้อมูลอาหารรายสัปดาห์ต่อไปนี้ "
                    + "โดยดูจาก TDEE และเป้าหมายของผู้ใช้ "
                    + "วิเคราะห์: 1) กินแคลอรี่เกิน/ขาดจาก TDEE แค่ไหน "
                    + "2) สัดส่วน protein/carbs/fat สมดุลไหม "
                    + "3) แนวโน้มการกิน วันไหนกินมาก/น้อย "
                    + "4) คำแนะนำสั้นๆ ตามเป้าหมายของผู้ใช้ "
                    + "ตอบเป็นภาษาไทย สรุปกระชับ ไม่ต้องยาวมาก "
                    + "ตอบเป็น JSON format: "
                    + "{\"summary\":\"สรุปภาพรวม\","
                    + "\"calorieAnalysis\":\"วิเคราะห์แคลอรี่\","
                    + "\"nutritionBalance\":\"วิเคราะห์สัดส่วนสารอาหาร\","
                    + "\"trend\":\"แนวโน้มการกิน\","
                    + "\"recommendations\":\"คำแนะนำ\"}"
                    + "\n\nข้อมูล:\n" + dataJson;

            String aiResponse = callGroqApi(prompt, "");
            return extractJson(aiResponse);

        } catch (Exception e) {
            throw new RuntimeException("Weekly analysis failed: " + e.getMessage());
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Step 1: ใช้ Vision AI อ่านรูปภาพแล้วส่งชื่ออาหารกลับให้ user แก้ไขได้
     * return JSON: {"dishName":"...", "dishNameEn":"..."}
     */
    public String identifyFoodFromImage(String imageData) {
        try {
            String aiResponse;
            if (isImageData(imageData)) {
                aiResponse = callGroqVisionApi(imageData);
            } else {
                throw new RuntimeException("ไม่ใช่รูปภาพ");
            }

            System.out.println("Identify AI Response: " + aiResponse);

            // ตรวจสอบว่าเป็นอาหารหรือไม่
            checkIsFood(aiResponse);

            // ดึงชื่อเมนูจาก AI response
            String dishName = extractDishName(aiResponse);
            String dishNameEn = extractDishNameEn(aiResponse);

            Map<String, Object> result = new HashMap<>();
            result.put("dishName", dishName);
            result.put("dishNameEn", dishNameEn);

            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            throw new RuntimeException("AI identify failed: " + e.getMessage());
        }
    }

    public String analyzeFood(String foodDescription) {
        return analyzeFood(foodDescription, 1);
    }

    public String analyzeFood(String foodDescription, int dishType) {
        try {
            // ตรวจคำหยาบเฉพาะ text input (ไม่ตรวจ URL รูป)
            if (!isImageData(foodDescription)) {
                checkProfanity(foodDescription);
            }

            String aiResponse;
            if (isImageData(foodDescription)) {
                aiResponse = callGroqVisionApi(foodDescription);
            } else if (dishType == 2) {
                aiResponse = callGroqApi(GUBKHAO_PROMPT, foodDescription);
            } else {
                aiResponse = callGroqApi(IDENTIFY_PROMPT, foodDescription);
            }

            System.out.println("AI Response: " + aiResponse);

            return buildResultFromAiResponse(aiResponse, foodDescription);

        } catch (Exception e) {
            throw new RuntimeException("AI analysis failed: " + e.getMessage());
        }
    }

    /**
     * สร้าง result JSON จาก AI response ที่มี ingredients
     * ค้นหา nutrition ของแต่ละ ingredient จาก CSV แล้วคำนวณรวม
     */
    private String buildResultFromAiResponse(String aiResponse, String userInput) throws Exception {
        // ตรวจสอบว่าเป็นอาหารหรือไม่ก่อนทำอะไรทั้งหมด
        checkIsFood(aiResponse);

        Map<String, Object> result = new HashMap<>();

        // ดึงชื่อเมนู
        String dishName = extractDishName(aiResponse);
        String dishNameEn = extractDishNameEn(aiResponse);
        result.put("name", dishName);
        result.put("nameEn", dishNameEn);

        // ดึง ingredients จาก AI response
        List<Map<String, Object>> ingredients = extractIngredients(aiResponse);
        System.out.println("Extracted ingredients: " + ingredients);

        if (ingredients != null && !ingredients.isEmpty()) {
            // คำนวณ nutrition: ลอง CSV ก่อน ถ้า score ต่ำ → USDA API
            double totalCalories = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0;
            double totalFiber = 0, totalSodium = 0, totalSugar = 0;
            List<Map<String, Object>> ingredientDetails = new ArrayList<>();

            for (Map<String, Object> ing : ingredients) {
                String name = (String) ing.get("name");
                String nameEn = ing.get("nameEn") != null ? (String) ing.get("nameEn") : "";
                double amount = 100.0;
                if (ing.get("amount") instanceof Number) {
                    amount = ((Number) ing.get("amount")).doubleValue();
                }
                double ratio = amount / 100.0;

                Map<String, Object> detail = new HashMap<>();
                detail.put("name", name);
                detail.put("nameEn", nameEn);

                // 1. ลอง CSV ก่อน (ใช้ alias mapping)
                Map<String, Object> csvResult = nutritionDataService.calculateNutritionFromIngredients(
                        List.of(ing));
                List<Map<String, Object>> csvDetails = (List<Map<String, Object>>) csvResult.get("ingredients");
                Map<String, Object> csvDetail = csvDetails.get(0);
                boolean csvFound = Boolean.TRUE.equals(csvDetail.get("found"));
                // ตรวจว่า CSV match ดีพอไหม (matchScore >= 70)
                int csvMatchScore = csvFound ? ((Number) csvDetail.getOrDefault("matchScore", 0)).intValue() : 0;

                if (csvFound && csvMatchScore >= 70) {
                    double ingCal = ((Number) csvDetail.get("calories")).doubleValue();
                    // sanity check: kcal/g ไม่ควรเกิน 9 (ไขมันบริสุทธิ์) → match ผิดแน่
                    double calPerGram = amount > 0 ? ingCal / amount : 0;
                    if (calPerGram > 9.0) {
                        System.out.println("[SanityCheck] Skipping suspicious match for '" + name
                                + "' → matched '" + csvDetail.get("matchedFood")
                                + "' cal/g=" + calPerGram);
                        detail.put("found", false);
                        ingredientDetails.add(detail);
                        continue;
                    }
                    // ใช้ CSV
                    detail.put("matchedFood", csvDetail.get("matchedFood"));
                    detail.put("calories", csvDetail.get("calories"));
                    detail.put("protein", csvDetail.get("protein"));
                    detail.put("fat", csvDetail.get("fat"));
                    detail.put("carbs", csvDetail.get("carbs"));
                    detail.put("found", true);
                    detail.put("source", "Thai DB");

                    totalCalories += ingCal;
                    totalProtein += ((Number) csvDetail.get("protein")).doubleValue();
                    totalFat += ((Number) csvDetail.get("fat")).doubleValue();
                    totalCarbs += ((Number) csvDetail.get("carbs")).doubleValue();
                } else {
                    // 2. CSV ไม่เจอ/ไม่ดี → ลอง USDA API
                    String searchQuery = !nameEn.isEmpty() ? nameEn : name;
                    Map<String, Object> usdaResult = searchUsdaFood(searchQuery);

                    if (usdaResult != null) {
                        double uCal = ((Number) usdaResult.getOrDefault("calories", 0)).doubleValue() * ratio;
                        double uPro = ((Number) usdaResult.getOrDefault("protein", 0)).doubleValue() * ratio;
                        double uFat = ((Number) usdaResult.getOrDefault("fat", 0)).doubleValue() * ratio;
                        double uCarbs = ((Number) usdaResult.getOrDefault("carbs", 0)).doubleValue() * ratio;

                        detail.put("matchedFood", usdaResult.get("matchedFood"));
                        detail.put("calories", Math.round(uCal * 100.0) / 100.0);
                        detail.put("protein", Math.round(uPro * 100.0) / 100.0);
                        detail.put("fat", Math.round(uFat * 100.0) / 100.0);
                        detail.put("carbs", Math.round(uCarbs * 100.0) / 100.0);
                        detail.put("found", true);
                        detail.put("source", "USDA");

                        totalCalories += uCal;
                        totalProtein += uPro;
                        totalFat += uFat;
                        totalCarbs += uCarbs;
                    } else {
                        detail.put("found", false);
                    }
                }

                ingredientDetails.add(detail);
            }

            result.put("dataSource", "Ingredient-based (Thai DB + USDA)");
            result.put("matchType", "ingredient_breakdown");
            result.put("matchedFood", dishName);

            if (totalCalories > 3000) {
                System.out.println("[SanityCheck] WARNING: totalCalories=" + totalCalories
                        + " for dish '" + dishName + "' seems too high!");
            }

            result.put("calories", Math.round(totalCalories * 100.0) / 100.0);
            result.put("protein", Math.round(totalProtein * 100.0) / 100.0);
            result.put("carbs", Math.round(totalCarbs * 100.0) / 100.0);
            result.put("fat", Math.round(totalFat * 100.0) / 100.0);

            // log ingredients แต่ไม่เก็บใน result
            System.out.println("Ingredient details: " + ingredientDetails);

            int foundCount = 0;
            for (Map<String, Object> detail : ingredientDetails) {
                if (Boolean.TRUE.equals(detail.get("found"))) {
                    foundCount++;
                }
            }

            // ถ้าไม่เจอข้อมูลโภชนาการเลยสักรายการ → ไม่บันทึก
            if (foundCount == 0) {
                throw new RuntimeException("NOT_FOUND:ไม่พบข้อมูลโภชนาการของเมนู \"" + dishName + "\" กรุณาลองใส่ชื่ออาหารอื่น");
            }

            result.put("description", "คำนวณจากส่วนประกอบ " + foundCount + "/" + ingredientDetails.size()
                    + " รายการ ของเมนู " + dishName);

        } else {
            // AI ไม่สามารถแตก ingredients ได้ -> fallback ใช้วิธีเดิม
            System.out.println("No ingredients from AI, falling back to direct search...");
            String foodName = dishName != null && !dishName.isEmpty() ? dishName : userInput;

            Map<String, Object> searchResult = nutritionDataService.findFoodWithSimilarity(foodName);
            if (searchResult == null && dishNameEn != null && !dishNameEn.isEmpty()) {
                searchResult = nutritionDataService.findFoodWithSimilarity(dishNameEn);
            }

            if (searchResult != null) {
                Map<String, String> nutritionData = (Map<String, String>) searchResult.get("food");
                result.put("dataSource", "Thai Food Database");
                result.put("matchedFood", nutritionData.get("nameTh"));
                result.put("matchType", searchResult.get("matchType"));
                result.put("calories", parseDouble(nutritionData.get("energy")));
                result.put("protein", parseDouble(nutritionData.get("protein")));
                result.put("carbs", parseDouble(nutritionData.get("carbohydrate")));
                result.put("fat", parseDouble(nutritionData.get("fat")));
                result.put("fiber", parseDouble(nutritionData.get("fiber")));
                result.put("sodium", parseDouble(nutritionData.get("sodium")));
                result.put("water", parseDouble(nutritionData.get("water")));
                result.put("calcium", parseDouble(nutritionData.get("calcium")));
                result.put("phosphorus", parseDouble(nutritionData.get("phosphorus")));
                result.put("iron", parseDouble(nutritionData.get("iron")));
                result.put("vitaminA", parseDouble(nutritionData.get("vitaminA")));
                result.put("vitaminC", parseDouble(nutritionData.get("vitaminC")));
                result.put("vitaminE", parseDouble(nutritionData.get("vitaminE")));
                result.put("sugar", parseDouble(nutritionData.get("sugar")));
                result.put("description", "พบข้อมูล: " + nutritionData.get("nameTh"));
            } else {
                // ลอง USDA
                String searchQuery = dishNameEn != null && !dishNameEn.isEmpty() && !dishNameEn.equals("Unknown")
                    ? dishNameEn : foodName;
                Map<String, Object> usdaResult = searchUsdaFood(searchQuery);
                if (usdaResult != null) {
                    result.putAll(usdaResult);
                    result.put("name", foodName);
                    result.put("nameEn", dishNameEn);
                } else {
                    throw new RuntimeException("NOT_FOUND:ไม่พบข้อมูลโภชนาการของเมนู \"" + foodName + "\" กรุณาลองใส่ชื่ออาหารอื่น");
                }
            }
        }

        return objectMapper.writeValueAsString(result);
    }

    /**
     * วิเคราะห์อาหารจากรูปภาพ + text ที่ user ใส่มา
     */
    public String analyzeFoodWithImageAndText(String imageUrl, String userText) {
        try {
            checkProfanity(userText);

            // เรียก Vision API พร้อม text ที่ user ใส่มา
            String aiResponse = callGroqVisionApiWithText(imageUrl, userText);
            System.out.println("AI Response (image+text): " + aiResponse);

            // ใช้ buildResultFromAiResponse เหมือน analyzeFood
            return buildResultFromAiResponse(aiResponse, userText);

        } catch (Exception e) {
            throw new RuntimeException("AI analysis failed: " + e.getMessage());
        }
    }

    /**
     * เรียก Vision API พร้อม text จาก user
     */
    private String callGroqVisionApiWithText(String imageUrl, String userText) {
        System.out.println("=== callGroqVisionApiWithText ===");
        System.out.println("Image URL: " + imageUrl);
        System.out.println("User Text: " + userText);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            String prompt = "ดูรูปนี้ก่อน ถ้าในรูปไม่มีอาหาร (เช่น รูปคน สัตว์ วัตถุ สิ่งของ หรือสิ่งที่ไม่ใช่อาหาร) "
                    + "ให้ตอบ: {\"isFood\":false,\"message\":\"รูปที่ใส่ไม่ใช่รูปอาหาร กรุณาใส่รูปอาหารเท่านั้น\"} "
                    + "ถ้าเป็นรูปอาหาร ผู้ใช้บอกว่าเป็น \"" + userText + "\" "
                    + "ช่วยระบุชื่อเมนูให้ละเอียด แล้วแตกส่วนประกอบพร้อมปริมาณต่อ 1 จาน (กรัม) "
                    + "สำคัญ: ชื่อ ingredient ใช้ชื่อตามฐานข้อมูลอาหารไทย เช่น 'ข้าวเจ้า, นึ่ง' แทน 'ข้าวสวย' "
                    + "ตอบเป็น JSON เท่านั้น ห้ามตอบอย่างอื่น ตัวอย่าง: "
                    + "{\"isFood\":true,\"dishName\":\"ข้าวผัดหมู\",\"dishNameEn\":\"Pork Fried Rice\","
                    + "\"ingredients\":[{\"name\":\"ข้าวเจ้า, นึ่ง\",\"nameEn\":\"rice steamed\",\"amount\":200},"
                    + "{\"name\":\"หมู, สันนอก\",\"nameEn\":\"pork loin\",\"amount\":50}]}";

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", List.of(
                    Map.of("type", "text", "text", prompt),
                    Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))));

            Map<String, Object> body = Map.of(
                    "model", VISION_MODEL,
                    "messages", List.of(userMsg),
                    "temperature", 0.0,
                    "max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            Map message = (Map) choices.get(0).get("message");
            String content = (String) message.get("content");

            return content.replace("```json", "").replace("```", "").trim();

        } catch (Exception e) {
            System.err.println("Groq Vision API ERROR: " + e.getMessage());
            throw new RuntimeException("Groq Vision failed: " + e.getMessage());
        }
    }

    private String callGroqApi(String prompt, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            // รวม prompt + user เป็น message เดียว
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");

            // ตัด userMessage ถ้ายาวเกิน (กันพัง)
            String safeUserMessage = userMessage;
            if (safeUserMessage.length() > 300) {
                safeUserMessage = safeUserMessage.substring(0, 300);
            }

            userMsg.put(
                    "content",
                    prompt + "\n" + safeUserMessage);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(userMsg);

            Map<String, Object> body = new HashMap<>();
            body.put("model", TEXT_MODEL);
            body.put("messages", messages);
            body.put("temperature", 0.0);
            body.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            System.out.println("Calling Groq VISION MODEL: " + VISION_MODEL);
            System.out.println("Calling Groq API with model: " + TEXT_MODEL);

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from Groq API");
            }

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in Groq API response");
            }

            Map message = (Map) choices.get(0).get("message");
            String result = (String) message.get("content");

            return result
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

        } catch (Exception e) {
            System.err.println("Groq API error: " + e.getMessage());
            throw new RuntimeException("Groq API call failed: " + e.getMessage());
        }
    }

    /**
     * ตรวจสอบ isFood flag จาก AI response
     * ถ้า isFood == false ให้โยน exception พร้อม message จาก AI
     */
    private void checkIsFood(String aiResponse) {
        if (aiResponse == null) return;

        // 1. ลอง parse JSON ตรงๆ ก่อน
        try {
            String jsonPart = extractJson(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(jsonPart, Map.class);
            Object isFoodObj = parsed.get("isFood");
            if (isFoodObj instanceof Boolean && Boolean.FALSE.equals(isFoodObj)) {
                String message = (String) parsed.getOrDefault("message",
                        "ข้อมูลที่ใส่ไม่ใช่อาหาร กรุณาใส่ชื่อเมนูอาหารเท่านั้น");
                throw new RuntimeException("NOT_FOOD:" + message);
            }
            // ถ้า parse ได้และ isFood == true หรือไม่มี field นี้ -> ผ่าน
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignore) {
            // parse ไม่ได้ -> ตรวจแบบ string แทน
        }

        // 2. Fallback: ตรวจสอบ string pattern กรณี AI ตอบเป็น text ผสม JSON
        String lower = aiResponse.toLowerCase();
        if (lower.contains("\"isfood\":false") || lower.contains("\"isfood\": false")
                || lower.contains("isfood false") || lower.contains("is not food")
                || lower.contains("not a food") || lower.contains("ไม่ใช่อาหาร")) {
            // พยายามดึง message จาก JSON ที่ฝังอยู่ใน text
            String message = "ข้อมูลที่ใส่ไม่ใช่อาหาร กรุณาใส่ชื่อเมนูอาหารเท่านั้น";
            try {
                String jsonPart = extractJson(aiResponse);
                Map<String, Object> parsed = objectMapper.readValue(jsonPart, Map.class);
                if (parsed.containsKey("message")) {
                    message = (String) parsed.get("message");
                }
            } catch (Exception ignore) {}
            throw new RuntimeException("NOT_FOOD:" + message);
        }

        // 3. ถ้า AI ตอบเป็น text ล้วน (ไม่มี JSON เลย) หรือ JSON ที่ไม่มี dishName
        // แสดงว่า AI ไม่รู้จักว่าเป็นอาหาร → reject
        try {
            String jsonPart = extractJson(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(jsonPart, Map.class);
            // ถ้า JSON ไม่มี dishName และไม่มี foodName → ไม่ใช่ food analysis response
            boolean hasDishName = parsed.containsKey("dishName") && parsed.get("dishName") != null
                    && !((String) parsed.get("dishName")).isBlank();
            boolean hasFoodName = parsed.containsKey("foodName") && parsed.get("foodName") != null;
            if (!hasDishName && !hasFoodName) {
                throw new RuntimeException("NOT_FOOD:ข้อมูลที่ใส่ไม่ใช่อาหาร กรุณาใส่ชื่อเมนูอาหารเท่านั้น");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // ถ้า parse JSON ไม่ได้เลย = AI ตอบเป็น text ล้วน → ไม่ใช่อาหาร
            throw new RuntimeException("NOT_FOOD:ข้อมูลที่ใส่ไม่ใช่อาหาร กรุณาใส่ชื่อเมนูอาหารที่ถูกต้อง");
        }
    }

    private String extractDishName(String aiResponse) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(aiResponse, Map.class);
            // ลอง dishName ก่อน (format ใหม่) แล้วค่อย foodName (format เก่า)
            String name = (String) parsed.get("dishName");
            if (name == null || name.trim().isEmpty()) {
                name = (String) parsed.get("foodName");
            }
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
        } catch (Exception ignore) {
        }
        return aiResponse.replaceAll("[^ก-๙a-zA-Z ]", "").trim();
    }

    private String extractDishNameEn(String aiResponse) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(aiResponse, Map.class);
            String name = (String) parsed.get("dishNameEn");
            if (name == null || name.trim().isEmpty()) {
                name = (String) parsed.get("foodNameEn");
            }
            return name != null ? name : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractIngredients(String aiResponse) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(aiResponse, Map.class);
            Object ingredientsObj = parsed.get("ingredients");
            if (ingredientsObj instanceof List) {
                return (List<Map<String, Object>>) ingredientsObj;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-") || value.equals("tr")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private boolean isImageData(String text) {
        if (text == null)
            return false;
        String lower = text.toLowerCase();
        System.out.println("=== isImageData check ===");
        System.out.println("Input length: " + text.length());

        // ตรวจสอบว่าเป็น Base64 (ยาวมากๆ) หรือ URL รูปภาพ
        boolean isBase64 = text.length() > 1000; // Base64 ของรูปจะยาวมาก
        boolean isUrl = lower.startsWith("http") && (
                lower.contains(".jpg") ||
                lower.contains(".jpeg") ||
                lower.contains(".png") ||
                lower.contains(".gif") ||
                lower.contains(".webp") ||lower.contains("supabase") ||
                lower.contains("/storage/")
        );

        boolean isImage = isBase64 || isUrl;
        System.out.println("Is Base64: " + isBase64 + ", Is URL: " + isUrl);
        return isImage;
    }

    private String callGroqVisionApi(String imageData) {
        System.out.println("=== callGroqVisionApi CALLED ===");
        System.out.println("Using VISION MODEL: " + VISION_MODEL);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            // สร้าง image URL - ถ้าเป็น Base64 ให้แปลงเป็น data URL
            String imageUrl;
            if (imageData.startsWith("http")) {
                imageUrl = imageData;
                System.out.println("Using URL: " + imageUrl);
            } else {
                // เป็น Base64 - แปลงเป็น data URL
                imageUrl = "data:image/jpeg;base64," + imageData;
                System.out.println("Using Base64 data URL (length: " + imageUrl.length() + ")");
            }

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", List.of(
                    Map.of("type", "text",
                            "text", "ดูรูปนี้ก่อน ถ้าในรูปไม่มีอาหาร (เช่น รูปคน สัตว์ วัตถุ สิ่งของ หรือสิ่งที่ไม่ใช่อาหาร) "
                            + "ให้ตอบ: {\"isFood\":false,\"message\":\"รูปที่ใส่ไม่ใช่รูปอาหาร กรุณาใส่รูปอาหารเท่านั้น\"} "
                            + "ถ้าเป็นรูปอาหาร ให้ระบุชื่อเมนูให้ละเอียด แล้วแตกส่วนประกอบพร้อมปริมาณต่อ 1 จาน (กรัม) "
                            + "สำคัญ: ชื่อ ingredient ใช้ชื่อตามฐานข้อมูลอาหารไทย เช่น 'ข้าวเจ้า, นึ่ง' แทน 'ข้าวสวย' "
                            + "ตอบเป็น JSON เท่านั้น ห้ามตอบอย่างอื่น ตัวอย่าง: "
                            + "{\"isFood\":true,\"dishName\":\"ข้าวผัดหมู\",\"dishNameEn\":\"Pork Fried Rice\","
                            + "\"ingredients\":[{\"name\":\"ข้าวเจ้า, นึ่ง\",\"nameEn\":\"rice steamed\",\"amount\":200},"
                            + "{\"name\":\"หมู, สันนอก\",\"nameEn\":\"pork loin\",\"amount\":50}]}"),
                    Map.of("type", "image_url",
                            "image_url", Map.of("url", imageUrl))));

            Map<String, Object> body = Map.of(
                    "model", VISION_MODEL,
                    "messages", List.of(userMsg),
                    "temperature", 0.0,
                    "max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            System.out.println("Sending request to Groq Vision API...");

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);
            System.out.println("Groq Vision Response: " + response.getBody());

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            Map message = (Map) choices.get(0).get("message");
            String content = (String) message.get("content");

            System.out.println("Vision AI Response: " + content);

            return content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Groq Vision API HTTP ERROR: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Groq Vision failed: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Groq Vision API ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Groq Vision failed: " + e.getMessage());
        }
    }

    /**
     * ค้นหาข้อมูลโภชนาการจาก USDA FoodData Central API
     * ใช้เป็น fallback เมื่อไม่พบข้อมูลใน Thai Food Database
     */
    private Map<String, Object> searchUsdaFood(String foodName) {
        if (usdaApiKey == null || usdaApiKey.isEmpty()) {
            System.out.println("USDA API key not configured");
            return null;
        }

        try {
            System.out.println("=== Searching USDA API for: " + foodName + " ===");

            // สร้าง URL พร้อม query parameters
            String url = USDA_SEARCH_URL + "?api_key=" + usdaApiKey +
                        "&query=" + java.net.URLEncoder.encode(foodName, "UTF-8") +
                        "&pageSize=1&dataType=Foundation,SR%20Legacy,Survey%20(FNDDS)";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() == null) {
                System.out.println("USDA API returned null");
                return null;
            }

            List<Map<String, Object>> foods = (List<Map<String, Object>>) response.getBody().get("foods");
            if (foods == null || foods.isEmpty()) {
                System.out.println("No foods found in USDA");
                return null;
            }

            Map<String, Object> usdaFood = foods.get(0);
            System.out.println("USDA Food found: " + usdaFood.get("description"));

            // แปลงข้อมูลจาก USDA format เป็น format ที่เราใช้
            Map<String, Object> result = new HashMap<>();
            result.put("name", usdaFood.get("description"));
            result.put("nameEn", usdaFood.get("description"));
            result.put("dataSource", "USDA FoodData Central");
            result.put("matchedFood", usdaFood.get("description"));
            result.put("matchType", "usda_api");

            // ดึงค่าโภชนาการจาก foodNutrients
            List<Map<String, Object>> nutrients = (List<Map<String, Object>>) usdaFood.get("foodNutrients");
            if (nutrients != null) {
                for (Map<String, Object> nutrient : nutrients) {
                    String nutrientName = (String) nutrient.get("nutrientName");
                    Object valueObj = nutrient.get("value");
                    double value = 0;
                    if (valueObj instanceof Number) {
                        value = ((Number) valueObj).doubleValue();
                    }

                    if (nutrientName == null) continue;

                    // Map USDA nutrient names to our format
                    String unitName = nutrient.get("unitName") != null ? (String) nutrient.get("unitName") : "";
                    if (nutrientName.contains("Energy") && unitName.equalsIgnoreCase("KCAL")) {
                        result.put("calories", value);
                    } else if (nutrientName.contains("Protein")) {
                        result.put("protein", value);
                    } else if (nutrientName.contains("Carbohydrate")) {
                        result.put("carbs", value);
                    } else if (nutrientName.contains("Total lipid") || nutrientName.equals("Fat")) {
                        result.put("fat", value);
                    } else if (nutrientName.contains("Fiber")) {
                        result.put("fiber", value);
                    } else if (nutrientName.contains("Sodium")) {
                        result.put("sodium", value);
                    } else if (nutrientName.contains("Sugars")) {
                        result.put("sugar", value);
                    } else if (nutrientName.contains("Calcium")) {
                        result.put("calcium", value);
                    } else if (nutrientName.contains("Iron")) {
                        result.put("iron", value);
                    } else if (nutrientName.contains("Vitamin A")) {
                        result.put("vitaminA", value);
                    } else if (nutrientName.contains("Vitamin C")) {
                        result.put("vitaminC", value);
                    } else if (nutrientName.contains("Vitamin E")) {
                        result.put("vitaminE", value);
                    }
                }
            }

            // ใส่ค่า default ถ้าไม่มีข้อมูล
            result.putIfAbsent("calories", 0.0);
            result.putIfAbsent("protein", 0.0);
            result.putIfAbsent("carbs", 0.0);
            result.putIfAbsent("fat", 0.0);
            result.putIfAbsent("fiber", 0.0);
            result.putIfAbsent("sodium", 0.0);
            result.putIfAbsent("sugar", 0.0);

            result.put("description", "ข้อมูลจาก USDA: " + usdaFood.get("description"));

            return result;

        } catch (Exception e) {
            System.err.println("USDA API error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
