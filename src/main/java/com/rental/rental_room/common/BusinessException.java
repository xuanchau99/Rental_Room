package com.rental.rental_room.common;
import org.springframework.http.HttpStatus;
public class BusinessException extends RuntimeException {
    public final HttpStatus status;
    public BusinessException(HttpStatus status, String message) { super(message); this.status = status; }
    public static BusinessException invalid(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, message); }
    public static BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
    public static BusinessException missing(String message) { return new BusinessException(HttpStatus.NOT_FOUND, message); }
}
