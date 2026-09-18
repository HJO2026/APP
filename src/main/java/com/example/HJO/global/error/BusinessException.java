package com.example.HJO.global.error;

/**
 * 서비스 계층에서 던지는 예외. 응답 변환은 {@link GlobalExceptionHandler}가 한곳에서 한다.
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

}
