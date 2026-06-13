package com.erp.products.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acces refuse — permission insuffisante");
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentification requise");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Violation integrite donnees: {}", detail);
        String message = mapIntegrityMessage(detail);
        return buildResponse(HttpStatus.CONFLICT, message);
    }

    private static String mapIntegrityMessage(String detail) {
        if (detail == null) {
            return "Operation impossible : contrainte de donnees non respectee.";
        }
        String lower = detail.toLowerCase();
        if (lower.contains("seller_id")
                || (lower.contains("null value") && lower.contains("not-null"))) {
            return "Donnees vente incompletes (vendeur manquant). Reessayez ou recreez la vente.";
        }
        if (lower.contains("sales_status_check")) {
            return "Statut de vente non autorise en base — redemarrez le backend pour appliquer la migration POS.";
        }
        if (lower.contains("payments") && (lower.contains("cashier_id") || lower.contains("pos_session_id"))) {
            return "Donnees paiement incompletes. Contactez un administrateur pour resynchroniser le POS.";
        }
        if (lower.contains("foreign key") || lower.contains("violates foreign key")) {
            return "Operation impossible : reference invalide ou entite liee manquante.";
        }
        if (lower.contains("still referenced") || lower.contains("delete") || lower.contains("update or delete")) {
            return "Suppression impossible : cette entite est encore utilisee ailleurs.";
        }
        return "Operation impossible : contrainte de donnees non respectee.";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonError(HttpMessageNotReadableException ex) {
        String message = "Corps de requête JSON invalide";
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            String field = invalidFormat.getPath().isEmpty()
                    ? "inconnu"
                    : invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            message = "Valeur invalide pour le champ « " + field + " »";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String param = ex.getName() != null ? ex.getName() : "paramètre";
        return buildResponse(HttpStatus.BAD_REQUEST, "Paramètre invalide : " + param);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
