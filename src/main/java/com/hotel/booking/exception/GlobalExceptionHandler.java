package com.hotel.booking.exception;

import com.hotel.booking.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return (uri != null && uri.startsWith("/api/")) ||
               (accept != null && accept.contains("application/json"));
    }

    private Object buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        if (isApiRequest(request)) {
            ErrorResponse error = new ErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
            return new ResponseEntity<>(error, status);
        } else {
            ModelAndView mav = new ModelAndView("error");
            mav.addObject("status", status.value());
            mav.addObject("error", status.getReasonPhrase());
            mav.addObject("message", message);
            mav.addObject("timestamp", LocalDateTime.now());
            mav.setStatus(status);
            return mav;
        }
    }

    @ExceptionHandler({ResourceNotFoundException.class, BookingNotFoundException.class})
    public Object handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({RoomNotAvailableException.class, UserAlreadyExistsException.class, DuplicateResourceException.class})
    public Object handleConflict(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidBookingException.class, PaymentFailedException.class, IllegalArgumentException.class, IllegalStateException.class})
    public Object handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, errors, request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "An unexpected internal error occurred.", request);
    }
}
