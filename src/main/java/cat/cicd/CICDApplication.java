package cat.cicd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CICDApplication {

	public static void main(String[] args) {
		SpringApplication.run(CICDApplication.class, args);
	}

}
