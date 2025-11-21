package cat.cicd.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		// 보안 스키마 설정을 하려면 아래에 보안 설정 추가 (JWT 토큰 인증)

		return new OpenAPI()
				.info(apiInfo());

	}

	private Info apiInfo() {
		return new Info()
				.title("CI/CD Web App API")
				.description("CI/CD Webb App API 명세서 및 테스트")
				.version("1.0.0");
	}
}
