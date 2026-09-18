package com.example.HJO.global.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;

/**
 * 컨트롤러 밖으로 나온 예외를 한곳에서 {@link ErrorResponse}로 바꾼다.
 * <p>
 * Spring MVC 표준 예외(400 형식 오류, 404 경로 없음, 405, 406, 415 등)는 {@link ResponseEntityExceptionHandler}가
 * 올바른 상태 코드를 정하고, 여기서는 본문만 공통 형식으로 바꾼다. 그래서 클라이언트 실수가 500으로 새지 않는다.
 * 인증 필터에서 나는 401은 여기까지 오지 않으므로 {@code JsonAuthenticationEntryPoint}가 같은 형식으로 응답한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
		return toResponse(ex.getErrorCode());
	}

	/** FK 위반, 잘못된 데이터 등 DB 무결성 오류. 대응표에 없으면 서버 버그로 보고 500 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
		return DataIntegrityErrorMapping.resolve(ex)
				.map(GlobalExceptionHandler::toResponse)
				.orElseGet(() -> {
					log.error("Unmapped data integrity violation", ex);
					return toResponse(ErrorCode.INTERNAL_ERROR);
				});
	}

	/** @Validated 등 AOP 방식 메서드 검증 실패 (MVC 기본 파라미터 검증은 handleHandlerMethodValidationException) */
	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		List<ErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
				.map(v -> new ErrorResponse.FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
				.toList();
		return toResponse(ErrorCode.INVALID_INPUT, errors);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return toResponse(ErrorCode.INTERNAL_ERROR);
	}

	// ---- Spring MVC 표준 예외: 필드 정보를 붙이는 것들 ----

	/** @Valid 요청 본문 검증 실패 → errors에 필드별 이유 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
				.toList();
		return body(ErrorCode.INVALID_INPUT, errors, headers);
	}

	/** 요청 파라미터·경로 변수 검증(@Min, @Max 등) 실패 → errors에 파라미터별 이유 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(
								result.getMethodParameter().getParameterName(), message(error))))
				.toList();
		return body(ErrorCode.INVALID_INPUT, errors, headers);
	}

	/** 파라미터·경로 변수 타입 변환 실패(size=abc, 범위를 넘는 id 등) → errors에 파라미터 이름 */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getPropertyName() == null
				? List.of()
				: List.of(new ErrorResponse.FieldError(ex.getPropertyName(), "형식이 올바르지 않습니다"));
		return body(ErrorCode.INVALID_INPUT, errors, headers);
	}

	// ---- 나머지 Spring MVC 표준 예외: 상태 코드는 그대로 두고 본문만 공통 형식으로 ----

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (statusCode.is5xxServerError()) {
			log.error("Server error while handling request", ex);
		}
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		if (status == HttpStatus.NOT_ACCEPTABLE) {
			// 클라이언트가 JSON을 받지 않겠다고 했으므로 본문 없이 상태 코드만 돌려준다
			return ResponseEntity.status(statusCode).headers(headers).build();
		}
		ErrorCode errorCode = toErrorCode(statusCode);
		return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(errorCode));
	}

	private static ErrorCode toErrorCode(HttpStatusCode status) {
		return switch (status.value()) {
			case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_INPUT;
		};
	}

	private static ResponseEntity<Object> body(ErrorCode errorCode, List<ErrorResponse.FieldError> errors,
			HttpHeaders headers) {
		return ResponseEntity.status(errorCode.getStatus()).headers(headers).body(ErrorResponse.of(errorCode, errors));
	}

	private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}

	private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, List<ErrorResponse.FieldError> errors) {
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, errors));
	}

	private static String message(MessageSourceResolvable error) {
		return error.getDefaultMessage() != null ? error.getDefaultMessage() : "올바르지 않은 값입니다";
	}

	private static String lastNode(String path) {
		int dot = path.lastIndexOf('.');
		return dot < 0 ? path : path.substring(dot + 1);
	}

}
