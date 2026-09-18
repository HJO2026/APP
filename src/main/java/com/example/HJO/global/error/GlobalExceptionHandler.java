package com.example.HJO.global.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * 컨트롤러 밖으로 나온 예외를 한곳에서 {@link ErrorResponse}로 바꾼다.
 * 인증 필터에서 나는 401은 여기까지 오지 않으므로 {@code JsonAuthenticationEntryPoint}가 같은 형식으로 응답한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
		return toResponse(ex.getErrorCode());
	}

	/** @Valid 요청 본문 검증 실패 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
				.toList();
		return toResponse(ErrorCode.INVALID_INPUT, errors);
	}

	/** 파라미터 검증(@Min, @Max 등) 실패 */
	@ExceptionHandler({ HandlerMethodValidationException.class, ConstraintViolationException.class })
	ResponseEntity<ErrorResponse> handleInvalidParameter(Exception ex) {
		return toResponse(ErrorCode.INVALID_INPUT);
	}

	/** JSON 파싱 실패, 필수 파라미터 누락, 타입 불일치 */
	@ExceptionHandler({ HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class })
	ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
		return toResponse(ErrorCode.INVALID_INPUT);
	}

	/** FK 위반 등 DB 제약 위반. 없는 게시글/댓글/게시판을 404로 바꾼다. 대응표에 없으면 서버 버그로 보고 500 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
		return ConstraintErrorMapping.resolve(ex)
				.map(GlobalExceptionHandler::toResponse)
				.orElseGet(() -> {
					log.error("Unmapped data integrity violation", ex);
					return toResponse(ErrorCode.INTERNAL_ERROR);
				});
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
		return toResponse(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
		return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return toResponse(ErrorCode.INTERNAL_ERROR);
	}

	private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}

	private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, List<ErrorResponse.FieldError> errors) {
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, errors));
	}

}
