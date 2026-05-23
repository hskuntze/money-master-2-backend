package br.com.kuntzedevprojects.money_master_2.exceptions;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.kuntzedevprojects.money_master_2.dtos.common.ApiError;
import br.com.kuntzedevprojects.money_master_2.dtos.common.FieldErrorDetail;
import br.com.kuntzedevprojects.money_master_2.services.FailureLogService;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final FailureLogService failureLogService;

    public GlobalExceptionHandler(FailureLogService failureLogService) {
        this.failureLogService = failureLogService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para executar esta ação.", request, ex);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class, LockedException.class, AuthenticationException.class})
    ResponseEntity<ApiError> handleAuthentication(RuntimeException ex, HttpServletRequest request) {
        String message = "Não foi possível autenticar com as credenciais informadas.";
        if (ex instanceof DisabledException) {
            message = "Usuário desabilitado ou e-mail ainda não confirmado.";
        }
        if (ex instanceof LockedException) {
            message = "Usuário bloqueado.";
        }
        return build(HttpStatus.UNAUTHORIZED, message, request, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toDetail)
                .toList();

        String fieldMessage = fields.stream()
                .map(field -> field.field() + ": " + field.message())
                .collect(Collectors.joining("; "));
        String message = "Existem campos inválidos na requisição." + (fieldMessage.isBlank() ? "" : " " + fieldMessage);
        failureLogService.record(ex, HttpStatus.BAD_REQUEST, request, message);

        ApiError body = new ApiError(
                java.time.Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Existem campos inválidos na requisição.",
                request.getRequestURI(),
                fields
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.", request, ex);
    }

    private FieldErrorDetail toDetail(FieldError fieldError) {
        return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request, Exception exception) {
        failureLogService.record(exception, status, request, message);
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
