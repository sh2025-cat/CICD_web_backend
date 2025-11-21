package cat.cicd.global.common;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record CommonResponse<T>(HttpStatus status, String message, T data) {

	public static <T> CommonResponse<T> of(T data) {
		return new CommonResponse<>(HttpStatus.OK, "Success", data);
	}

	public static <T> CommonResponse<T> of(T data, String message) {
		return new CommonResponse<>(HttpStatus.OK, message, data);
	}

	public static <T> CommonResponse<T> error(HttpStatus status, String message, T data) {
		return new CommonResponse<>(status, message, data);
	}
}
