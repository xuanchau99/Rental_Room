package com.rental.rental_room.common;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> business(BusinessException ex) {
        return ResponseEntity.status(ex.status).body(ProblemDetail.forStatusAndDetail(ex.status, ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList());
        return ResponseEntity.badRequest().body(problem);
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid value or request format"));
    }
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> conflict(Exception ex) {
        return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Duplicate data, referenced record, or concurrent modification. Reload and retry."));
    }
}
