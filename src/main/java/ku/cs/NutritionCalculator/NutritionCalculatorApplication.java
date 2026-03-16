package ku.cs.NutritionCalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NutritionCalculatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(NutritionCalculatorApplication.class, args);
	}

}
