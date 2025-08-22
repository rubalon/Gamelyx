package com.gamelyx.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la aplicación.
 * Intercepta las excepciones antes de que Spring Security las convierta en 403.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de campos (@Valid)
     * Ejemplo: email inválido, campo requerido faltante
     * Devuelve: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Validation Error");

        // Extraer errores de cada campo
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        response.put("errors", fieldErrors);
        response.put("message", "Invalid input data");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja errores de integridad de datos
     * Ejemplo: username duplicado, violación de constraint único
     * Devuelve: 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 409);
        response.put("error", "Conflict");

        // Intentar extraer un mensaje más amigable
        String message = "Data integrity violation";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("username")) {
                message = "Username already exists";
            } else if (ex.getMessage().contains("email")) {
                message = "Email already exists";
            }
        }

        response.put("message", message);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Maneja IllegalStateException
     * Ejemplo: solicitudes duplicadas, conflictos de estado
     * Devuelve: 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 409);
        response.put("error", "Conflict");

        // Mostrar mensaje detallado solo en desarrollo
        if (isDevEnvironment()) {
            response.put("message", ex.getMessage());
        } else {
            response.put("message", "Conflicto con el estado actual");
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Maneja errores generales de acceso a datos
     * Ejemplo: conexión perdida con BD, query mal formada
     * Devuelve: 500 Internal Server Error
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseError(
            DataAccessException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Database Error");
        response.put("message", "Error accessing database");

        // En desarrollo puedes incluir más detalles
        // En producción, es mejor no exponer detalles de la BD
        if (isDevEnvironment()) {
            response.put("debug", ex.getMostSpecificCause().getMessage());
        }

        ex.printStackTrace(); // Para logs del servidor

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Maneja errores de autenticación
     * Ejemplo: credenciales incorrectas
     * Devuelve: 401 Unauthorized
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationError(
            AuthenticationException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 401);
        response.put("error", "Unauthorized");
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "Authentication failed");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Maneja recursos estáticos no encontrados
     * Spring a veces trata rutas API como recursos estáticos
     * Devuelve: 404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(
            NoResourceFoundException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 404);
        response.put("error", "Not Found");
        response.put("message", "Endpoint not found: " + ex.getResourcePath());
        response.put("path", "/" + ex.getResourcePath());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Maneja endpoints no encontrados (404)
     * Se activa cuando spring.mvc.throw-exception-if-no-handler-found=true
     * Devuelve: 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 404);
        response.put("error", "Not Found");
        response.put("message", "Endpoint not found: " + ex.getRequestURL());
        response.put("path", ex.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Maneja errores de tipo de argumento incorrecto
     * Ejemplo: se espera un número pero llega texto
     * Devuelve: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Bad Request");
        response.put("message", String.format("Invalid value for parameter '%s': %s",
                ex.getName(), ex.getValue()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja errores de WebClient (llamadas a APIs externas como RAWG)
     * Devuelve: el mismo código que devolvió la API externa
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(
            WebClientResponseException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", ex.getStatusCode().value());
        response.put("error", "External API Error");
        response.put("message", "Error calling external service: " + ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Maneja IllegalArgumentException
     * Ejemplo: parámetros inválidos en la lógica de negocio
     * Devuelve: 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja cualquier excepción no capturada específicamente
     * Último recurso para evitar que errores inesperados se conviertan en 403
     * Devuelve: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        response.put("path", request.getDescription(false).replace("uri=", ""));

        // Log completo del error
        ex.printStackTrace();

        // En desarrollo, incluir más información
        if (isDevEnvironment()) {
            response.put("exception", ex.getClass().getSimpleName());
            response.put("debug", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Helper method para determinar si estamos en desarrollo
     * Puedes cambiar esta lógica según tu configuración
     */
    private boolean isDevEnvironment() {
        // Opción 1: Usar una variable de entorno
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        return "dev".equals(env) || "development".equals(env);

        // Opción 2: Siempre true en desarrollo, cambiar a false para producción
        // return true;
    }
}