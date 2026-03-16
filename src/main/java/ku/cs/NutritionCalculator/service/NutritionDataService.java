package ku.cs.NutritionCalculator.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class NutritionDataService {

    private List<Map<String, String>> foodData = new ArrayList<>();

    // Mapping ชื่อ ingredient ทั่วไป → ชื่อใน CSV
    private static final Map<String, String> INGREDIENT_ALIASES = new HashMap<>();
    static {
        INGREDIENT_ALIASES.put("ข้าวสวย", "ข้าวเจ้า, นึ่ง");
        INGREDIENT_ALIASES.put("ข้าวหุง", "ข้าวเจ้า, นึ่ง");
        INGREDIENT_ALIASES.put("ข้าวเปล่า", "ข้าวเจ้า, นึ่ง");
        INGREDIENT_ALIASES.put("cooked rice", "rice steamed");
        INGREDIENT_ALIASES.put("น้ำมันพืช", "น้ำมัน, ถั่วเหลือง");
        INGREDIENT_ALIASES.put("vegetable oil", "soybean oil");
        INGREDIENT_ALIASES.put("น้ำมันปาล์ม", "น้ำมัน, ปาล์ม");
        INGREDIENT_ALIASES.put("หมูสับ", "หมู, สันนอก");
        INGREDIENT_ALIASES.put("minced pork", "pork loin");
        INGREDIENT_ALIASES.put("อกไก่", "ไก่, อกไก่");
        INGREDIENT_ALIASES.put("chicken breast", "chicken breast");
        INGREDIENT_ALIASES.put("น้ำตาลทราย", "น้ำตาล, ทราย");
        INGREDIENT_ALIASES.put("sugar", "sugar, granulated");
        INGREDIENT_ALIASES.put("กระเทียม", "กระเทียม, กลีบ");
        INGREDIENT_ALIASES.put("ข้าวเหนียว", "ข้าวเหนียว, นึ่ง");
        INGREDIENT_ALIASES.put("sticky rice", "rice glutinous steamed");
        INGREDIENT_ALIASES.put("ไข่ไก่", "ไข่ไก่, ทั้งฟอง");
        INGREDIENT_ALIASES.put("ไข่", "ไข่ไก่, ทั้งฟอง");
        INGREDIENT_ALIASES.put("egg", "egg whole");
        INGREDIENT_ALIASES.put("ต้นหอม", "ต้นหอม");
        INGREDIENT_ALIASES.put("เนื้อปู", "ปู, เนื้อ");
        INGREDIENT_ALIASES.put("เนื้อปูแกะ", "ปู, เนื้อ");
        INGREDIENT_ALIASES.put("กุ้งขาว", "กุ้งกุลาดำ, เนื้อ, สด");
        INGREDIENT_ALIASES.put("กุ้ง", "กุ้งกุลาดำ, เนื้อ, สด");
        INGREDIENT_ALIASES.put("กุ้งสด", "กุ้งกุลาดำ, เนื้อ, สด");
        INGREDIENT_ALIASES.put("shrimp", "กุ้งกุลาดำ, เนื้อ, สด");
        INGREDIENT_ALIASES.put("white shrimp", "กุ้งกุลาดำ, เนื้อ, สด");
    }

    @PostConstruct
    public void loadData() {
        try {
            ClassPathResource resource = new ClassPathResource("csv/Data_food_normalize - data.csv");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            // อ่าน header
            String headerLine = reader.readLine();
            String[] headers = parseCSVLine(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] values = parseCSVLine(line);
                if (values.length < 3 || values[2].trim().isEmpty())
                    continue;

                Map<String, String> food = new HashMap<>();
                food.put("nameTh", cleanValue(values[2])); // ชื่อไทย
                food.put("nameEn", values.length > 3 ? cleanValue(values[3]) : ""); // ชื่ออังกฤษ
                food.put("energy", values.length > 4 ? cleanValue(values[4]) : "0"); // Energy
                food.put("water", values.length > 5 ? cleanValue(values[5]) : "0");
                food.put("protein", values.length > 6 ? cleanValue(values[6]) : "0");
                food.put("fat", values.length > 7 ? cleanValue(values[7]) : "0");
                food.put("carbohydrate", values.length > 8 ? cleanValue(values[8]) : "0");
                food.put("fiber", values.length > 9 ? cleanValue(values[9]) : "0");
                food.put("ash", values.length > 10 ? cleanValue(values[10]) : "0");
                food.put("calcium", values.length > 11 ? cleanValue(values[11]) : "0");
                food.put("phosphorus", values.length > 12 ? cleanValue(values[12]) : "0");
                food.put("magnesium", values.length > 13 ? cleanValue(values[13]) : "0");
                food.put("sodium", values.length > 14 ? cleanValue(values[14]) : "0");
                food.put("potassium", values.length > 15 ? cleanValue(values[15]) : "0");
                food.put("iron", values.length > 16 ? cleanValue(values[16]) : "0");
                food.put("copper", values.length > 17 ? cleanValue(values[17]) : "0");
                food.put("zinc", values.length > 18 ? cleanValue(values[18]) : "0");
                food.put("vitaminA", values.length > 21 ? cleanValue(values[21]) : "0");
                food.put("vitaminB1", values.length > 22 ? cleanValue(values[22]) : "0");
                food.put("vitaminB2", values.length > 23 ? cleanValue(values[23]) : "0");
                food.put("niacin", values.length > 24 ? cleanValue(values[24]) : "0");
                food.put("vitaminC", values.length > 25 ? cleanValue(values[25]) : "0");
                food.put("vitaminE", values.length > 26 ? cleanValue(values[26]) : "0");
                food.put("sugar", values.length > 27 ? cleanValue(values[27]) : "0");

                foodData.add(food);
            }
            reader.close();
            System.out.println("Loaded " + foodData.size() + " food items from CSV");
        } catch (Exception e) {
            System.err.println("Error loading nutrition data: " + e.getMessage());
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String cleanValue(String value) {
        if (value == null)
            return "0";
        value = value.trim();
        // ลบวงเล็บ เช่น (350) -> 350
        value = value.replace("(", "").replace(")", "");
        // ถ้าเป็น - หรือ tr ให้เป็น 0
        if (value.equals("-") || value.equals("tr") || value.isEmpty()) {
            return "0";
        }
        return value;
    }

    public Map<String, String> findFood(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return null;
        }

        String searchName = foodName.toLowerCase().trim();

        // ค้นหาแบบตรงกันก่อน
        for (Map<String, String> food : foodData) {
            String nameTh = food.get("nameTh").toLowerCase();
            String nameEn = food.get("nameEn").toLowerCase();
            if (nameTh.contains(searchName) || nameEn.contains(searchName) ||
                    searchName.contains(nameTh) || searchName.contains(nameEn)) {
                return food;
            }
        }

        // ค้นหาแบบแยกคำ
        String[] keywords = searchName.split("[,\\s]+");
        int maxMatch = 0;
        Map<String, String> bestMatch = null;

        for (Map<String, String> food : foodData) {
            String nameTh = food.get("nameTh").toLowerCase();
            String nameEn = food.get("nameEn").toLowerCase();
            int matchCount = 0;

            for (String keyword : keywords) {
                if (keyword.length() > 1 && (nameTh.contains(keyword) || nameEn.contains(keyword))) {
                    matchCount++;
                }
            }

            if (matchCount > maxMatch) {
                maxMatch = matchCount;
                bestMatch = food;
            }
        }

        return bestMatch;
    }

    public List<Map<String, String>> searchFoods(String keyword, int limit) {
        List<Map<String, String>> results = new ArrayList<>();
        String search = keyword.toLowerCase().trim();

        for (Map<String, String> food : foodData) {
            if (results.size() >= limit)
                break;

            String nameTh = food.get("nameTh").toLowerCase();
            String nameEn = food.get("nameEn").toLowerCase();

            if (nameTh.contains(search) || nameEn.contains(search)) {
                results.add(food);
            }
        }
        return results;
    }

    public List<String> getAllFoodNames() {
        List<String> names = new ArrayList<>();
        for (Map<String, String> food : foodData) {
            names.add(food.get("nameTh"));
        }
        return names;
    }

    // ค้นหาอาหารที่ใกล้เคียงที่สุด โดยใช้ similarity score
    public Map<String, Object> findFoodWithSimilarity(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return null;
        }

        String searchName = foodName.toLowerCase().trim();
        Map<String, String> bestMatch = null;
        int bestScore = 0;
        String matchType = "none";

        // 1. ค้นหาแบบตรงกันเป๊ะ
        for (Map<String, String> food : foodData) {
            String nameTh = food.get("nameTh").toLowerCase();
            String nameEn = food.get("nameEn").toLowerCase();

            if (nameTh.equals(searchName) || nameEn.equals(searchName)) {
                bestMatch = food;
                bestScore = 100;
                matchType = "exact";
                break;
            }
        }

        // 2. ค้นหาแบบ contains
        if (bestMatch == null) {
            for (Map<String, String> food : foodData) {
                String nameTh = food.get("nameTh").toLowerCase();
                String nameEn = food.get("nameEn").toLowerCase();

                if (nameTh.contains(searchName) || searchName.contains(nameTh)) {
                    int score = calculateSimilarity(searchName, nameTh);
                    if (score > bestScore && score >= 30) {
                        bestScore = score;
                        bestMatch = food;
                        matchType = "contains";
                    }
                }
                if (nameEn.contains(searchName) || searchName.contains(nameEn)) {
                    int score = calculateSimilarity(searchName, nameEn);
                    if (score > bestScore && score >= 30) {
                        bestScore = score;
                        bestMatch = food;
                        matchType = "contains";
                    }
                }
            }
        }

        // 3. ค้นหาแบบ keyword matching (นับจำนวน keyword ที่ match และให้น้ำหนัก)
        if (bestScore < 50) {
            List<String> searchKeywords = extractKeywords(searchName);
            int searchKeywordCount = searchKeywords.size();

            for (Map<String, String> food : foodData) {
                String nameTh = food.get("nameTh").toLowerCase();
                String nameEn = food.get("nameEn").toLowerCase();

                int matchedWeight = 0;
                int matchedCount = 0;

                // นับ keyword จาก searchName ที่พบใน food name
                for (String keyword : searchKeywords) {
                    if (nameTh.contains(keyword) || nameEn.contains(keyword)) {
                        matchedWeight += getKeywordWeight(keyword);
                        matchedCount++;
                    }
                }

                // คำนวณ score: ยิ่ง match หลายคำ ยิ่งได้ score สูง
                if (matchedCount > 0) {
                    // Base score จากสัดส่วนคำที่ match
                    int score = (matchedCount * 100) / searchKeywordCount;

                    // Bonus จาก weight ของคำที่ match (คำสำคัญได้ bonus มากกว่า)
                    score = Math.min(100, score + (matchedWeight * 2));

                    // Bonus พิเศษถ้า match หลายคำ
                    if (matchedCount >= 2) {
                        score = Math.min(100, score + 15);
                    }

                    // Penalty ถ้า match แค่คำที่ไม่สำคัญ (เช่น สี)
                    if (matchedCount == 1 && matchedWeight <= 1) {
                        score = score / 2; // ลด score ลงครึ่งหนึ่ง
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = food;
                        matchType = "keyword";
                    }
                }
            }
        }

        // 4. ค้นหาตามหมวดหมู่อาหาร (ถ้ายังไม่พบ)
        if (bestScore < 30) {
            String category = detectFoodCategory(searchName);
            if (category != null) {
                for (Map<String, String> food : foodData) {
                    String nameTh = food.get("nameTh").toLowerCase();
                    if (nameTh.contains(category)) {
                        bestMatch = food;
                        bestScore = 25;
                        matchType = "category";
                        break;
                    }
                }
            }
        }

        if (bestMatch == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("food", bestMatch);
        result.put("score", bestScore);
        result.put("matchType", matchType);
        return result;
    }

    private int calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 100;
        if (s1.contains(s2) || s2.contains(s1)) {
            int shorter = Math.min(s1.length(), s2.length());
            int longer = Math.max(s1.length(), s2.length());
            return (shorter * 100) / longer;
        }
        return 0;
    }

    // แยกคำจากชื่ออาหาร (รองรับทั้งไทยและอังกฤษ)
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.isEmpty()) return keywords;

        // แยกตาม space และ comma
        String[] parts = text.split("[,\\s]+");
        for (String part : parts) {
            if (part.length() > 1) {
                keywords.add(part);
            }
        }

        // สำหรับภาษาไทย ลองแยกคำสำคัญออกมา
        String[] thaiKeywords = {"ข้าว", "หมู", "ไก่", "เนื้อ", "ปลา", "กุ้ง", "ผัด", "ต้ม", "แกง", "ทอด", "ยำ", "ผัก",
                                 "ขาหมู", "หมูแดง", "หมูกรอบ", "เป็ด", "ไข่", "ก๋วยเตี๋ยว", "บะหมี่", "ขนมจีน",
                                 "แดง", "เขียว", "เหลือง", "ขาว", "ดำ"};
        for (String keyword : thaiKeywords) {
            if (text.contains(keyword) && !keywords.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        return keywords;
    }

    // ให้น้ำหนักคำตามความสำคัญ (คำที่บ่งบอกประเภทอาหารหลักจะได้ weight สูง)
    private int getKeywordWeight(String keyword) {
        // คำสำคัญหลัก - ประเภทอาหาร/วัตถุดิบหลัก (weight 5)
        String[] primaryKeywords = {"ข้าว", "หมู", "ไก่", "เนื้อ", "ปลา", "กุ้ง", "เป็ด", "ไข่",
                                    "ก๋วยเตี๋ยว", "บะหมี่", "ขนมจีน", "ขาหมู", "หมูแดง", "หมูกรอบ",
                                    "rice", "pork", "chicken", "beef", "fish", "shrimp", "duck", "egg", "noodle"};
        for (String pk : primaryKeywords) {
            if (keyword.equals(pk) || keyword.contains(pk)) {
                return 5;
            }
        }

        // คำสำคัญรอง - วิธีทำ (weight 3)
        String[] secondaryKeywords = {"ผัด", "ต้ม", "แกง", "ทอด", "ยำ", "นึ่ง", "ย่าง", "อบ",
                                      "fried", "boiled", "curry", "grilled", "steamed", "soup"};
        for (String sk : secondaryKeywords) {
            if (keyword.equals(sk) || keyword.contains(sk)) {
                return 3;
            }
        }

        // คำทั่วไป - สี/ลักษณะ (weight 1)
        String[] colorKeywords = {"แดง", "เขียว", "เหลือง", "ขาว", "ดำ", "red", "green", "yellow", "white", "black"};
        for (String ck : colorKeywords) {
            if (keyword.equals(ck)) {
                return 1;
            }
        }

        // คำอื่นๆ (weight 2)
        return 2;
    }

    private String detectFoodCategory(String foodName) {
        // ตรวจหาหมวดหมู่อาหารจากคำสำคัญ
        if (foodName.contains("ข้าว") || foodName.contains("rice")) return "ข้าว";
        if (foodName.contains("ก๋วยเตี๋ยว") || foodName.contains("noodle")) return "ก๋วยเตี๋ยว";
        if (foodName.contains("ผัด") || foodName.contains("stir") || foodName.contains("fried")) return "ผัด";
        if (foodName.contains("ต้ม") || foodName.contains("boil") || foodName.contains("soup")) return "ต้ม";
        if (foodName.contains("แกง") || foodName.contains("curry")) return "แกง";
        if (foodName.contains("ทอด") || foodName.contains("deep fried")) return "ทอด";
        if (foodName.contains("ยำ") || foodName.contains("salad")) return "ยำ";
        if (foodName.contains("ปลา") || foodName.contains("fish")) return "ปลา";
        if (foodName.contains("หมู") || foodName.contains("pork")) return "หมู";
        if (foodName.contains("ไก่") || foodName.contains("chicken")) return "ไก่";
        if (foodName.contains("กุ้ง") || foodName.contains("shrimp")) return "กุ้ง";
        if (foodName.contains("เนื้อ") || foodName.contains("beef")) return "เนื้อ";
        if (foodName.contains("ผัก") || foodName.contains("vegetable")) return "ผัก";
        if (foodName.contains("ขนม") || foodName.contains("dessert") || foodName.contains("sweet")) return "ขนม";
        if (foodName.contains("น้ำ") || foodName.contains("drink") || foodName.contains("juice")) return "น้ำ";
        return null;
    }

    /**
     * คำนวณ nutrition รวมจาก ingredients หลายตัว
     * แต่ละ ingredient มี name, nameEn, amount (กรัม)
     * ข้อมูลใน CSV เป็นต่อ 100g ดังนั้นต้องคูณ amount/100
     */
    public Map<String, Object> calculateNutritionFromIngredients(List<Map<String, Object>> ingredients) {
        Map<String, Double> totalNutrition = new HashMap<>();
        String[] nutrientKeys = {"energy", "water", "protein", "fat", "carbohydrate", "fiber",
                "calcium", "phosphorus", "magnesium", "sodium", "potassium", "iron",
                "copper", "zinc", "vitaminA", "vitaminB1", "vitaminB2", "niacin",
                "vitaminC", "vitaminE", "sugar"};

        for (String key : nutrientKeys) {
            totalNutrition.put(key, 0.0);
        }

        List<Map<String, Object>> ingredientDetails = new ArrayList<>();

        for (Map<String, Object> ingredient : ingredients) {
            String name = (String) ingredient.get("name");
            String nameEn = ingredient.get("nameEn") != null ? (String) ingredient.get("nameEn") : "";
            double amount = 100.0;
            if (ingredient.get("amount") instanceof Number) {
                amount = ((Number) ingredient.get("amount")).doubleValue();
            }

            // ใช้ alias mapping ก่อนค้นหา
            String resolvedName = INGREDIENT_ALIASES.getOrDefault(name, name);
            String resolvedNameEn = INGREDIENT_ALIASES.getOrDefault(nameEn, nameEn);

            // ค้นหา ingredient ใน CSV
            Map<String, Object> found = findFoodWithSimilarity(resolvedName);
            if (found == null && !resolvedNameEn.isEmpty()) {
                found = findFoodWithSimilarity(resolvedNameEn);
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("name", name);
            detail.put("nameEn", nameEn);

            if (found != null) {
                Map<String, String> foodInfo = (Map<String, String>) found.get("food");
                int matchScore = (int) found.get("score");
                detail.put("matchedFood", foodInfo.get("nameTh"));
                detail.put("matchScore", matchScore);

                // คำนวณ nutrition ตามปริมาณ (CSV = ต่อ 100g)
                double ratio = amount / 100.0;
                for (String key : nutrientKeys) {
                    double val = parseNutrientValue(foodInfo.get(key)) * ratio;
                    totalNutrition.put(key, totalNutrition.get(key) + val);
                }
                // เก็บแค่ 4 ค่าหลักต่อ ingredient
                detail.put("calories", Math.round(parseNutrientValue(foodInfo.get("energy")) * ratio * 100.0) / 100.0);
                detail.put("protein", Math.round(parseNutrientValue(foodInfo.get("protein")) * ratio * 100.0) / 100.0);
                detail.put("fat", Math.round(parseNutrientValue(foodInfo.get("fat")) * ratio * 100.0) / 100.0);
                detail.put("carbs", Math.round(parseNutrientValue(foodInfo.get("carbohydrate")) * ratio * 100.0) / 100.0);
                detail.put("found", true);
            } else {
                detail.put("found", false);
            }

            ingredientDetails.add(detail);
        }

        // ปัดเศษ total
        Map<String, Double> roundedTotal = new HashMap<>();
        for (Map.Entry<String, Double> entry : totalNutrition.entrySet()) {
            roundedTotal.put(entry.getKey(), Math.round(entry.getValue() * 100.0) / 100.0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ingredients", ingredientDetails);
        result.put("totalNutrition", roundedTotal);
        return result;
    }

    private double parseNutrientValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-") || value.equals("tr") || value.equals("0")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replace(",", "").replace("(", "").replace(")", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ค้นหาอาหารที่คล้ายกันหลายรายการ
    public List<Map<String, Object>> findSimilarFoods(String foodName, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (foodName == null || foodName.trim().isEmpty()) {
            return results;
        }

        String searchName = foodName.toLowerCase().trim();
        String[] keywords = searchName.split("[,\\s]+");

        for (Map<String, String> food : foodData) {
            String nameTh = food.get("nameTh").toLowerCase();
            String nameEn = food.get("nameEn").toLowerCase();
            int score = 0;

            // คำนวณ score
            if (nameTh.contains(searchName) || searchName.contains(nameTh)) {
                score = calculateSimilarity(searchName, nameTh);
            } else if (nameEn.contains(searchName) || searchName.contains(nameEn)) {
                score = calculateSimilarity(searchName, nameEn);
            } else {
                int matchCount = 0;
                int totalKeywords = 0;
                for (String keyword : keywords) {
                    if (keyword.length() > 1) {
                        totalKeywords++;
                        if (nameTh.contains(keyword) || nameEn.contains(keyword)) {
                            matchCount++;
                        }
                    }
                }
                if (totalKeywords > 0) {
                    score = (matchCount * 70) / totalKeywords;
                }
            }

            if (score > 20) {
                Map<String, Object> result = new HashMap<>();
                result.put("food", food);
                result.put("score", score);
                results.add(result);
            }
        }

        // เรียงตาม score มากไปน้อย
        results.sort((a, b) -> (Integer) b.get("score") - (Integer) a.get("score"));

        // จำกัดจำนวน
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }
}
