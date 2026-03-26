package ku.cs.NutritionCalculator.scheduler;

import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.Message;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.MessageRepository;
import ku.cs.NutritionCalculator.repository.UserRepository;
import ku.cs.NutritionCalculator.service.FoodAiService;
import ku.cs.NutritionCalculator.service.FoodService;
import ku.cs.NutritionCalculator.service.NutritionDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class DailyMealScheduler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NutritionDataService nutritionDataService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FoodAiService foodAiService;

    @Autowired
    private FoodService foodService;

    // เก็บประวัติเมนูที่เคยแนะนำต่อ user เพื่อไม่ให้ซ้ำ
    private final Map<UUID, LinkedList<String>> recentSuggestions = new ConcurrentHashMap<>();
    private static final int MAX_RECENT = 14; // ไม่ซ้ำ 14 เมนูล่าสุด
    private final List<String> breakfastMenus = List.of(
    "ข้าวต้มหมู","โจ๊กหมู","โจ๊กไก่","ไข่กระทะ","ไข่ต้มกับนม","ขนมปังปิ้งเนย","ขนมปังปิ้งแยม",
    "แซนด์วิชทูน่า","แซนด์วิชแฮมชีส","แซนด์วิชไข่","ซีเรียลกับนม","ข้าวเหนียวหมูปิ้ง",
    "ข้าวเหนียวไก่ทอด","ปาท่องโก๋","ปาท่องโก๋กับนมข้น","ขนมครก","ขนมปังสังขยา",
    "ข้าวไข่เจียว","ข้าวไข่ดาว","ข้าวหมูทอด","ข้าวไก่ทอด","โจ๊กปลา","โจ๊กหมูใส่ไข่",
    "ข้าวต้มปลา","ข้าวต้มกุ้ง","ต้มเลือดหมู","ก๋วยจั๊บญวน","ข้าวผัดไข่","ข้าวผัดหมู",
    "ข้าวผัดไก่","ก๋วยเตี๋ยวหมูน้ำใส","ก๋วยเตี๋ยวไก่","ก๋วยเตี๋ยวต้มยำหมู","ก๋วยเตี๋ยวปลา",
    "ขนมปังทูน่า","ขนมปังไข่ดาว","โจ๊กหมูใส่ไข่ลวก","โจ๊กหมูใส่ตับ","ข้าวต้มไก่",
    "ข้าวต้มกระดูกหมู","ก๋วยเตี๋ยวหลอด","ข้าวหมูแดง","ข้าวหมูกรอบ","ข้าวหน้าไก่",
    "ข้าวหน้าเป็ด","ข้าวไข่ข้น","ข้าวไข่ข้นแฮม","ข้าวไข่ข้นกุ้ง","ข้าวไข่ข้นหมู","ข้าวไข่ข้นไก่"
    );
    private final List<String> lunchMenus = List.of(
    "ข้าวกะเพราไก่","ข้าวกะเพราหมู","ข้าวกะเพราหมูกรอบ","ข้าวมันไก่","ข้าวหมูแดง",
    "ข้าวหมูกรอบ","ข้าวหน้าไก่","ข้าวหน้าเป็ด","ข้าวไข่เจียว","ข้าวไข่ดาว",
    "ผัดไทย","ผัดซีอิ๊ว","ราดหน้า","ข้าวผัดหมู","ข้าวผัดไก่",
    "ข้าวผัดกุ้ง","ข้าวผัดปู","ข้าวผัดสับปะรด","ก๋วยเตี๋ยวหมูน้ำตก","ก๋วยเตี๋ยวหมูน้ำใส",
    "ก๋วยเตี๋ยวต้มยำหมู","ก๋วยเตี๋ยวไก่","ก๋วยเตี๋ยวเนื้อ","ก๋วยเตี๋ยวปลา","ข้าวคลุกกะปิ",
    "ข้าวขาหมู","ข้าวหมูทอดกระเทียม","ข้าวไก่ทอด","ข้าวหมูกระเทียม","ข้าวไก่กระเทียม",
    "ข้าวปลาทอด","ข้าวปลาทอดน้ำปลา","ข้าวต้มปลา","ข้าวต้มหมู","ข้าวต้มกุ้ง",
    "ต้มยำกุ้ง","ต้มยำทะเล","แกงเขียวหวานไก่","แกงแดงหมู","แกงพะแนงหมู",
    "แกงพะแนงไก่","แกงจืดเต้าหู้หมูสับ","แกงส้มชะอมกุ้ง","ข้าวผัดกะเพรา","ข้าวไข่ข้นหมู",
    "ข้าวไข่ข้นกุ้ง","ข้าวไข่ข้นไก่","ข้าวไข่ข้นปู","ข้าวหมูอบ","ข้าวไก่อบ"
    );
    private final List<String> dinnerMenus = List.of(
    "สลัดอกไก่","สลัดทูน่า","สลัดผัก","สลัดกุ้ง","สลัดไข่ต้ม",
    "ข้าวปลาเผา","ข้าวปลานึ่งมะนาว","ข้าวปลาทอด","ข้าวปลาย่าง","ข้าวปลานึ่งซีอิ๊ว",
    "ต้มจืดหมูสับ","ต้มจืดเต้าหู้","ต้มจืดสาหร่าย","ต้มยำไก่","ต้มยำปลา",
    "ต้มยำกุ้ง","แกงเขียวหวานไก่","แกงส้มปลา","แกงเลียง","แกงจืดวุ้นเส้น",
    "ข้าวไข่เจียว","ข้าวไข่ดาว","ข้าวไข่ข้น","ข้าวผัดไข่","ข้าวผัดหมู",
    "ข้าวผัดไก่","ข้าวผัดกุ้ง","ข้าวกะเพราไก่","ข้าวกะเพราหมู","ข้าวกะเพราหมูกรอบ",
    "ข้าวหมูย่าง","ข้าวไก่ย่าง","ข้าวหมูทอด","ข้าวไก่ทอด","ข้าวปลาทอดน้ำปลา",
    "ข้าวต้มปลา","ข้าวต้มหมู","ข้าวต้มกุ้ง","ข้าวคลุกกะปิ","ผัดผักรวมมิตร",
    "ผัดบรอกโคลีหมู","ผัดคะน้าหมูกรอบ","ผัดคะน้าน้ำมันหอย","ผัดเห็ดรวม","ผัดถั่วงอก",
    "สุกี้น้ำ","สุกี้แห้ง","สุกี้หมู","สุกี้ไก่","สุกี้ทะเล"
    );

    @Scheduled(cron = "0 * * * * *") // รันทุกนาที
    public void checkMealTimes() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getBreakfastTime() != null &&
                    user.getBreakfastTime().withSecond(0).withNano(0).equals(now)) {
                sendMealSuggestion(user, "อาหารเช้า");
            } else if (user.getLunchTime() != null &&
                    user.getLunchTime().withSecond(0).withNano(0).equals(now)) {
                sendMealSuggestion(user, "อาหารกลางวัน");
            } else if (user.getDinnerTime() != null &&
                    user.getDinnerTime().withSecond(0).withNano(0).equals(now)) {
                sendMealSuggestion(user, "อาหารเย็น");
            }
        }
    }

    private void sendMealSuggestion(User user, String mealType) {
        List<String> menuPool = switch (mealType) {
            case "อาหารเช้า" -> breakfastMenus;
            case "อาหารกลางวัน" -> lunchMenus;
            default -> dinnerMenus;
        };
        LinkedList<String> recent = recentSuggestions.computeIfAbsent(user.getId(), k -> new LinkedList<>());

        // กรองเมนูที่เคยแนะนำออก
        List<String> available = new ArrayList<>(menuPool);
        available.removeAll(recent);

        // ถ้าหมดแล้วให้เริ่มใหม่
        if (available.isEmpty()) {
            recent.clear();
            available = new ArrayList<>(menuPool);
        }

        // สุ่ม 1 เมนู
        Collections.shuffle(available, new Random());
        List<String> chosen = new ArrayList<>(available.subList(0, 1));

        // บันทึกลงประวัติ เพื่อกันซ้ำครั้งถัดไป
        for (String food : chosen) {
            recent.addLast(food);
        }
        while (recent.size() > MAX_RECENT) {
            recent.removeFirst();
        }

        // วิเคราะห์โภชนาการและบันทึก Food_Logging เหมือน user ส่ง /foods/analyze-text
        StringBuilder content = new StringBuilder();
        content.append("เมนู").append(mealType).append("ที่แนะนำสำหรับวันนี้\n");
        for (int i = 0; i < chosen.size(); i++) {
            String foodName = chosen.get(i);
            content.append(i + 1).append(". ").append(foodName).append("\n");
            try {
                String aiResult = foodAiService.analyzeFood(foodName, 1);
                Food_Logging food = new Food_Logging();
                food.setUser(user);
                food.setText(foodName);
                food.setAi(aiResult);
                food.setDish(1);
                food.setDatetimeFood(LocalDateTime.now());
                Food_Logging saved = foodService.createFood(food);
                foodService.runAndSaveWeeklyAnalysis(saved, user);
                System.out.println("[MealScheduler] Analyzed & saved Food_Logging for: " + foodName);
            } catch (Exception e) {
                System.err.println("[MealScheduler] Failed to analyze food: " + foodName + " - " + e.getMessage());
            }
        }

        // บันทึกเป็น bot message
        Message message = new Message();
        message.setUser(user);
        message.setSender(false);
        message.setContent(content.toString().trim());
        message.setCreatedat(LocalDateTime.now());
        messageRepository.save(message);

        System.out.println("[MealScheduler] Sent " + mealType + " suggestion to user: " + user.getUsername());
    }
}
