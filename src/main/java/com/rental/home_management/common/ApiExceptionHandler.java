package com.rental.home_management.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ProblemDetail> business(BusinessException ex) {
    return ResponseEntity.status(ex.status)
        .body(ProblemDetail.forStatusAndDetail(ex.status, ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
    problem.setProperty(
        "errors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList());
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException ex) {
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof InvalidFormatException invalid && invalid.getTargetType().isEnum()) {
        var field =
            invalid.getPath().stream()
                .map(
                    ref ->
                        ref.getFieldName() == null
                            ? "[" + ref.getIndex() + "]"
                            : ref.getFieldName())
                .reduce((a, b) -> a + "." + b)
                .orElse("value");
        var allowed = Arrays.toString(invalid.getTargetType().getEnumConstants());
        return ResponseEntity.badRequest()
            .body(
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, field + " must be one of " + allowed));
      }
    }
    return invalidRequest();
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ProblemDetail> mismatch(MethodArgumentTypeMismatchException ex) {
    var type = ex.getRequiredType();
    if (type != null && type.isEnum()) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetail.forStatusAndDetail(
                  HttpStatus.BAD_REQUEST,
                  ex.getName() + " must be one of " + Arrays.toString(type.getEnumConstants())));
    }
    return invalidRequest();
  }

  @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
  ResponseEntity<ProblemDetail> invalid(Exception ex) {
    return invalidRequest();
  }

  private ResponseEntity<ProblemDetail> invalidRequest() {
    return ResponseEntity.badRequest()
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid value or request format"));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<ProblemDetail> conflict(Exception ex) {
    return ResponseEntity.status(409)
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Duplicate data, referenced record, or concurrent modification. Reload and retry."));
  }
}
