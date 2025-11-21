package cat.cicd.global.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Summary API 요청에 대한 오류 응답 메시지를 담는 클래스
 * @Description status: HTTP 상태 값 ex) OK, BAD_REQUEST, INTERNAL_SERVER_ERROR ... code: 오류 코드 message: 오류 메시지
 */
@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
	@JsonProperty("status")
	private HttpStatus status;
	@JsonProperty("status_code")
	private Integer statusCode;
	@JsonProperty("message")
	private String message;
	@JsonProperty("timestamp")
	private String timestamp;

	@Builder
	ErrorResponse(HttpStatus status, Integer statusCode, String message) {
		this.status = status;
		this.statusCode = statusCode;
		this.message = message;
		this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
	}
}