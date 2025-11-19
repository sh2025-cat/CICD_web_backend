package cat.cicd.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseRuntimeException extends RuntimeException {
	private final HttpStatus status;

	public BaseRuntimeException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public BaseRuntimeException(HttpStatus status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
	}
}
