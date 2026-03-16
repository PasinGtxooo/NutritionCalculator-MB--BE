package ku.cs.NutritionCalculator.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)

    @Column(name = "m_id")
    private UUID id;

    @Column(name = "m_sender")
    private boolean sender;

    @Column(name = "m_content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "m_createdat")
    private LocalDateTime createdat;

    @ManyToOne
    @JoinColumn(name = "u_id")
    private User user;
}