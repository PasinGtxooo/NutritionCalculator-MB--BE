// entity/Chat_Context.java
package ku.cs.NutritionCalculator.entity;

import lombok.Data;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Table(name = "chat_context")
public class Chat_Context {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "c_id")
    private UUID id;

    @Column(name = "c_key")
    private String contextKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "c_value", columnDefinition = "jsonb")
    private List<Map<String, String>> messages; // เก็บประวัติ [{role, content}, ...]

    @Column(name = "c_createdat")
    private LocalDateTime createdAt;

    @Column(name = "c_updatedat")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "u_id")
    private User user;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}