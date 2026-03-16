package ku.cs.NutritionCalculator.repository;

import ku.cs.NutritionCalculator.entity.Message;
import ku.cs.NutritionCalculator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    // ดึงประวัติแชทของ user เรียงตามเวลาจากเก่าไปใหม่
    List<Message> findByUserOrderByCreatedatAsc(User user);

    // ดึงประวัติแชทของ user เรียงตามเวลาจากใหม่ไปเก่า (สำหรับแสดงใน UI)
    List<Message> findByUserOrderByCreatedatDesc(User user);

    // ลบประวัติแชททั้งหมดของ user
    void deleteByUser(User user);
}
