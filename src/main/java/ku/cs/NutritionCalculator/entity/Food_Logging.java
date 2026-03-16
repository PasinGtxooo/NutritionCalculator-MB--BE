package ku.cs.NutritionCalculator.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class Food_Logging {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)

    @Column(name = "f_id")
    private UUID id;

    @Column(name = "f_datetime")
    private LocalDateTime datetimeFood;

    @Column(name = "f_dish")
    private int dish;

    @Column(name = "f_image")
    private String imagePath;

    @Column(name = "f_text")
    private String text;

    @Column(name = "f_ai", columnDefinition = "TEXT")
    private String ai;

    @Column(name = "f_weekly",columnDefinition = "TEXT")
    private String weekly;
    
    @ManyToOne
    @JoinColumn(name = "u_id", nullable = false)
    private User user;

}
