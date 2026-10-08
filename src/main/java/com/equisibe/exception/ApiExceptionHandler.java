package com.equisibe.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }
    @ExceptionHandler(AuthenticationException.class)
public ProblemDetail handleAuthenticationException(
        AuthenticationException exception) {
    return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Correo o contraseña incorrectos."
    );
}
}