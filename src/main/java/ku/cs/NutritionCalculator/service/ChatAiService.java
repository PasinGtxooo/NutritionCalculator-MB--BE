package ku.cs.NutritionCalculator.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ku.cs.NutritionCalculator.entity.Food_Logging;
import ku.cs.NutritionCalculator.entity.Message;
import ku.cs.NutritionCalculator.entity.User;
import ku.cs.NutritionCalculator.repository.FoodRepository;
import ku.cs.NutritionCalculator.repository.MessageRepository;

@Service
public class ChatAiService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FoodRepository foodRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String CHAT_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final int MAX_HISTORY = 20;

    private static final java.util.Set<String> BANNED_WORDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "เหี้ย", "สัส", "เชี่ย", "ห่า", "ควาย", "ระยำ", "เย็ด", "ควย", "หี", "เงี่ยน",
        "fuck", "fucking", "shit", "bullshit", "asshole", "bitch", "bastard",
        "damn", "dick", "pussy", "slut", "whore", "motherfucker", "mf"
    ));

    private void checkProfanity(String message) {
        if (message == null) return;
        String lower = message.toLowerCase();
        for (String word : BANNED_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                throw new RuntimeException("PROFANITY:กรุณาใช้คำสุภาพในการสนทนา");
            }
        }
    }

    /**
     * แชทกับ AI พร้อมเก็บประวัติลง Supabase
     */
    @Transactional
    public String chat(User user, String userMessage) {
        // ตรวจคำหยาบก่อนบันทึกหรือส่ง AI
        checkProfanity(userMessage);

        // 1. เก็บข้อความของ user ลง database
        Message userMsg = new Message();
        userMsg.setUser(user);
        userMsg.setSender(true); // true = user
        userMsg.setContent(userMessage);
        userMsg.setCreatedat(LocalDateTime.now());
        messageRepository.save(userMsg);

        // 2. ดึงประวัติแชทล่าสุด
        List<Message> history = messageRepository.findByUserOrderByCreatedatAsc(user);
        if (history.size() > MAX_HISTORY) {
            history = history.subList(history.size() - MAX_HISTORY, history.size());
        }

        // 3. สร้าง messages สำหรับ Groq API
        List<Map<String, String>> messages = new ArrayList<>();

        // system prompt พร้อมข้อมูลผู้ใช้และอาหารรายสัปดาห์
        String userContext = buildUserContext(user);
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "คุณคือผู้เชี่ยวชาญด้านโภชนาการและสุขภาพ ชื่อ NutriBot "
                + "ให้คำแนะนำเกี่ยวกับอาหาร การกิน วิตามิน และสุขภาพ "
                + "ตอบเป็นภาษาไทยที่เข้าใจง่าย กระชับ และเป็นมิตร "
                + "ใช้ข้อมูลผู้ใช้และประวัติอาหารด้านล่างเพื่อให้คำแนะนำที่เหมาะกับผู้ใช้คนนี้โดยเฉพาะ"
                + "\n\n" + userContext);
        messages.add(systemMsg);

        // ประวัติแชท
        for (Message msg : history) {
            Map<String, String> chatMsg = new HashMap<>();
            chatMsg.put("role", msg.isSender() ? "user" : "assistant");
            chatMsg.put("content", msg.getContent());
            messages.add(chatMsg);
        }

        // 4. เรียก Groq API
        String aiResponse = callGroqChatApi(messages);

        // 5. เก็บคำตอบจาก AI ลง database
        Message aiMsg = new Message();
        aiMsg.setUser(user);
        aiMsg.setSender(false); // false = bot
        aiMsg.setContent(aiResponse);
        aiMsg.setCreatedat(LocalDateTime.now());
        messageRepository.save(aiMsg);

        return aiResponse;
    }

    /**
     * ดึงประวัติแชทของ user จาก Supabase
     */
    public List<Map<String, Object>> getHistory(User user) {
        List<Message> messages = messageRepository.findByUserOrderByCreatedatAsc(user);
        return messages.stream().map(msg -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", msg.getId().toString());
            m.put("text", msg.getContent());
            m.put("isBot", !msg.isSender());
            m.put("timestamp", msg.getCreatedat().toString());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * ลบประวัติแชททั้งหมดของ user
     */
    @Transactional
    public void clearHistory(User user) {
        messageRepository.deleteByUser(user);
    }

    /**
     * สร้าง context ข้อมูลผู้ใช้ + อาหารรายสัปดาห์ สำหรับ system prompt
     */
    @SuppressWarnings("unchecked")
    private String buildUserContext(User user) {
        StringBuilder sb = new StringBuilder();

        // ข้อมูลผู้ใช้
        sb.append("=== ข้อมูลผู้ใช้ ===\n");
        sb.append("น้ำหนัก: ").append(user.getWeight()).append(" kg\n");
        sb.append("ส่วนสูง: ").append(user.getHeight()).append(" cm\n");
        sb.append("TDEE: ").append(user.getTdee()).append(" แคลอรี่/วัน\n");
        sb.append("BMR: ").append(user.getBmr()).append(" แคลอรี่/วัน\n");
        sb.append("เป้าหมาย: ").append(user.getMaingoal() != null ? user.getMaingoal() : "ไม่ระบุ").append("\n\n");

        // อาหารรายสัปดาห์
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(7);
            List<Food_Logging> weeklyFoods = foodRepository
                    .findByUserAndDatetimeFoodBetweenOrderByDatetimeFoodAsc(user, start, end);

            if (!weeklyFoods.isEmpty()) {
                sb.append("=== อาหารที่กินใน 7 วันล่าสุด ===\n");

                Map<LocalDate, List<String>> dailyFoods = new HashMap<>();
                Map<LocalDate, Double> dailyCalories = new HashMap<>();

                for (Food_Logging food : weeklyFoods) {
                    LocalDate date = food.getDatetimeFood().toLocalDate();
                    dailyFoods.computeIfAbsent(date, k -> new ArrayList<>());
                    dailyCalories.putIfAbsent(date, 0.0);

                    if (food.getAi() != null && !food.getAi().isEmpty()) {
                        try {
                            Map<String, Object> aiData = objectMapper.readValue(food.getAi(), Map.class);
                            String foodName = (String) aiData.get("name");
                            if (foodName != null) {
                                dailyFoods.get(date).add(foodName);
                            }
                            Object cal = aiData.get("calories");
                            if (cal instanceof Number) {
                                dailyCalories.put(date, dailyCalories.get(date) + ((Number) cal).doubleValue());
                            }
                        } catch (Exception e) {
                            // skip unparseable entries
                        }
                    }
                }

                for (Map.Entry<LocalDate, List<String>> entry : dailyFoods.entrySet()) {
                    sb.append(entry.getKey()).append(": ");
                    sb.append(String.join(", ", entry.getValue()));
                    sb.append(" (รวม ~").append(Math.round(dailyCalories.get(entry.getKey()))).append(" kcal)\n");
                }
            } else {
                sb.append("ยังไม่มีข้อมูลอาหารในสัปดาห์นี้\n");
            }
        } catch (Exception e) {
            sb.append("ไม่สามารถโหลดข้อมูลอาหารได้\n");
        }

        return sb.toString();
    }

    /**
     * เรียก Groq Chat API (pattern เดียวกับ FoodAiService)
     */
    @SuppressWarnings("unchecked")
    private String callGroqChatApi(List<Map<String, String>> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", CHAT_MODEL);
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            System.out.println("Calling Groq Chat API with model: " + CHAT_MODEL);

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from Groq API");
            }

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in Groq API response");
            }

            Map message = (Map) choices.get(0).get("message");
            String content = (String) message.get("content");

            return content.trim();

        } catch (Exception e) {
            System.err.println("Groq Chat API error: " + e.getMessage());
            throw new RuntimeException("Groq Chat API call failed: " + e.getMessage());
        }
    }
}
