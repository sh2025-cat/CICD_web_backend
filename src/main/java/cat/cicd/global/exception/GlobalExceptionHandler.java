package cat.cicd.global.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

/**
 * @Summary 예외 발생에 대한 처리 클래스
 * @Description 예외 발생에 대한 처리 클래스 추후 AOP 를 이용하여 예외 발생에 대한 처리를 통합할 예정
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NoClassDefFoundError.class)
	public ResponseEntity<ErrorResponse> handleNoClassDefFoundError(NoClassDefFoundError e) {
		log.error("Class loading error occurred: {}", e.getMessage());
		String errorMessage = e.getMessage();
		if (e.getCause() != null) {
			errorMessage += " Caused by: " + e.getCause().getMessage();
		}

		ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				HttpStatus.INTERNAL_SERVER_ERROR.value(), "Class loading error: " + errorMessage);

		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler({ DateTimeParseException.class, DateTimeException.class })
	public ResponseEntity<ErrorResponse> handleDateTimeParseException(DateTimeException e) {
		log.error("Date parse error occurred: {}", e.getMessage());
		String errorMessage = e.getMessage();
		if (e.getCause() != null) {
			errorMessage += " Caused by: " + e.getCause().getMessage();
		}

		ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
				"Failed to parse date: " + errorMessage);
		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler({ JsonMappingException.class, JsonParseException.class })
	public ResponseEntity<ErrorResponse> handleParsingException(Exception e) {
		log.error("Parsing Error occurred: {}", e.getMessage());
		String errorMessage = e.getMessage();
		if (e.getCause() != null) {
			errorMessage += " Caused by: " + e.getCause().getMessage();
		}

		ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
				"Failed to parse OpenSearch response: " + errorMessage);
		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(BaseRuntimeException.class)
	public ResponseEntity<ErrorResponse> handleBaseException(BaseRuntimeException e) {
		log.error("Exception occurred {} [Status: {}]: {}", e.getClass().getName(), e.getStatus(), e.getMessage());

		ErrorResponse error = new ErrorResponse(e.getStatus(), e.getStatus().value(), e.getMessage());
		return ResponseEntity.status(e.getStatus()).body(error);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e) {
		log.error("Database Error occurred: {}", e.getMessage());

		ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		return ResponseEntity.internalServerError().body(error);
	}
}
