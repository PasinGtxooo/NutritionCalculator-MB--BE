package ku.cs.NutritionCalculator.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ku.cs.NutritionCalculator.entity.Sex;
import ku.cs.NutritionCalculator.entity.User;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserDetailsImpl implements UserDetails {
    private UUID id;
    private String username;
    private String phone;
    private String password;
    private Sex sex;
    private String firstname;
    private String lastname;
    private float weight;
    private float height;
    private int activitylevel;
    private String maingoal;
    private boolean alcohol;
    private boolean smoking;
    private int sweetlevel;
    private int saltylevel;
    private int sourlevel;
    private float bmr;
    private float tdee;
    private LocalTime breakfastTime;
    private LocalTime lunchTime;
    private LocalTime dinnerTime;
    private float bodyfat;
    private LocalDate birthDate;
    private String role;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole()));

        return UserDetailsImpl.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .password(user.getPassword())
                .sex(user.getSex())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .weight(user.getWeight())
                .height(user.getHeight())
                .activitylevel(user.getActivitylevel())
                .maingoal(user.getMaingoal())
                .alcohol(user.isAlcohol())
                .smoking(user.isSmoking())
                .sweetlevel(user.getSweetlevel())
                .saltylevel(user.getSaltylevel())
                .sourlevel(user.getSourlevel())
                .bmr(user.getBmr())
                .tdee(user.getTdee())
                .breakfastTime(user.getBreakfastTime())
                .lunchTime(user.getLunchTime())
                .dinnerTime(user.getDinnerTime())
                .bodyfat(user.getBodyfat())
                .birthDate(user.getBirthDate())
                .role(user.getRole())
                .authorities(authorities)
                .build();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
