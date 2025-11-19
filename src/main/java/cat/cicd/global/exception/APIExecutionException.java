package cat.cicd.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

public class APIExecutionException extends BaseRuntimeException {

	@Nullable
	@Getter
	private final String reason;

	public APIExecutionException(HttpStatus status) {
		this(status, null, null);
	}

	public APIExecutionException(HttpStatus status, @Nullable String reason) {
		this(status, reason, null);
	}

	public APIExecutionException(HttpStatus status, @Nullable String reason, @Nullable Throwable cause) {
		super(status, generateMessage(status, reason), cause);
		this.reason = reason;
	}

	private static String generateMessage(HttpStatus status, @Nullable String reason) {
		return status + (reason != null ? " \"" + reason + "\"" : "");
	}
}