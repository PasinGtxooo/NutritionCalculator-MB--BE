package ku.cs.NutritionCalculator.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "u_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "u_name")
  private String username;

  @Column(name = "u_phone")
  private String phone;

  @Column(name = "u_password")
  private String password;

  @Column(name = "u_firstname")
  private String firstname;

  @Column(name = "u_lastname")
  private String lastname;

  @Enumerated(EnumType.STRING)
  @Column(name = "u_sex")
  private Sex sex;

  @Column(name = "u_weight")
  private float weight;

  @Column(name = "u_height")
  private float height;

  @Column(name = "u_activity_level")
  private int activitylevel;

  @Column(name = "u_main_goal")
  private String maingoal;

  @Column(name = "u_alcohol")
  private boolean alcohol;

  @Column(name = "u_smoking")
  private boolean smoking;

  @Column(name = "u_sweet_level")
  private int sweetlevel;

  @Column(name = "u_salty_level")
  private int saltylevel;

  @Column(name = "u_sour_level")
  private int sourlevel;

  @Column(name = "u_bmr")
  private float bmr;

  @Column(name = "u_tdee")
  private float tdee;

  @Column(name = "u_breakfasttime")
  private LocalTime breakfastTime;

  @Column(name = "u_lunchtime")
  private LocalTime lunchTime;

  @Column(name = "u_dinnertime")
  private LocalTime dinnerTime;

  @Column(name = "u_bodyfat")
  private float bodyfat;

  @Column(name = "u_role")
  private String role;

  @Column(name = "u_birthDate")
  private LocalDate birthDate; // เพิ่มวันเกิด

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  private List<Food_Logging> foodLoggings;

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  private List<Chat_Context> chatContexts;

  // @OneToMany(mappedBy = "u_id")
  // @JsonIgnore
  // private List<Message> messages;

  // @OneToMany(mappedBy = "u_id")
  // @JsonIgnore
  // private List<Chat_Context> chatContexts;

}
