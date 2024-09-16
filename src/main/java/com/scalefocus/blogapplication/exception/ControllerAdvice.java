package com.scalefocus.blogapplication.exception;

import com.scalefocus.blogapplication.exception.custom.BadRequestException;
import com.scalefocus.blogapplication.exception.custom.RefreshTknExpireException;
import com.scalefocus.blogapplication.exception.custom.UniqueUsernameException;
import com.scalefocus.blogapplication.exception.response.BaseErrorResponse;
import com.scalefocus.blogapplication.exception.response.ValidationErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.*;


@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UniqueUsernameException.class)
    protected ResponseEntity<Object> handleUniqueDataException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(CONFLICT.value())
                .error("Unique Username Exception")
                .message("Username already exists")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(NOT_FOUND.value())
                .error("Entity Not Found Exception")
                .message("Entity not found")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, NOT_FOUND);
    }

    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<Object> handleNoSuchElementException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(NOT_FOUND.value())
                .error("No Such Element Exception")
                .message("Element not found")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, NOT_FOUND);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    protected ResponseEntity<Object> handleUsernameNotFoundException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(NOT_FOUND.value())
                .error("Username Not Found Exception")
                .message("Username not found")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleSQLException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(CONFLICT.value())
                .error("Data Integrity Violation Exception")
                .message("Data integrity violation")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<Object> handleBadRequestException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(BAD_REQUEST.value())
                .error("Bad Request Exception")
                .message("Bad request")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, BAD_REQUEST);
    }

    @ExceptionHandler(RefreshTknExpireException.class)
    protected ResponseEntity<Object> handleRefreshTknExpiredException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(FORBIDDEN.value())
                .error("Refresh Tkn Expire Exception")
                .message("Refresh token expired")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(FORBIDDEN.value())
                .error("Access Denied Exception")
                .message("Access denied")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, FORBIDDEN);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    protected ResponseEntity<Object> handleExpiredJwtException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(UNAUTHORIZED.value())
                .error("Expired Jwt Exception")
                .message("JWT expired")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<Object> handleBadCredentialsException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(UNAUTHORIZED.value())
                .error("Bad Credentials Exception")
                .message("Bad credentials")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(LockedException.class)
    protected ResponseEntity<Object> handleLockedException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(UNAUTHORIZED.value())
                .error("Locked Exception")
                .message("Too many login attempts, account locked for 10 minutes")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(AccountExpiredException.class)
    protected ResponseEntity<Object> handleAccountExpiredException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(UNAUTHORIZED.value())
                .error("Account Expired Exception")
                .message("Account has been locked")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, UNAUTHORIZED);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        ValidationErrorResponse validationErrorResponse = ValidationErrorResponse.builder()
                .statusCode(BAD_REQUEST.value())
                .error("Method Argument Not Valid Exception")
                .message("Validation error")
                .path(request.getDescription(false))
                .errors(this.convertFieldErrors(ex))
                .build();

        return new ResponseEntity<>(validationErrorResponse, HttpStatus.BAD_REQUEST);
    }

    @Order()
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnspecifiedException(WebRequest request) {
        BaseErrorResponse baseErrorResponse = BaseErrorResponse.builder()
                .statusCode(INTERNAL_SERVER_ERROR.value())
                .error("Exception")
                .message("Internal server error")
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(baseErrorResponse, INTERNAL_SERVER_ERROR);
    }

    private Map<String, List<String>> convertFieldErrors(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        return Map.of("errors", errors);
    }
}