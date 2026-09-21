package com.mugloar.web;

import com.mugloar.application.GameNotFoundException;
import com.mugloar.infrastructure.UpstreamGameException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(GameNotFoundException.class)
    ProblemDetail notFound(GameNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Game not found", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ProblemDetail invalid(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Action cannot be completed", exception.getMessage());
    }

    @ExceptionHandler(UpstreamGameException.class)
    ProblemDetail upstream(UpstreamGameException exception) {
        return problem(HttpStatus.BAD_GATEWAY, "Mugloar service unavailable", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
