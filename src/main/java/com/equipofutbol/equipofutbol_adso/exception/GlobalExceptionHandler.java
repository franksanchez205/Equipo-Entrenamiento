package com.equipofutbol.equipofutbol_adso.exception;

import com.equipofutbol.equipofutbol_adso.dto.MessageResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones anotado con @RestControllerAdvice, que es una especialización de
 * @ControllerAdvice específica para controladores REST. Spring aplica automáticamente los métodos
 * de esta clase a todos los controladores de la aplicación, interceptando las excepciones que se
 * lanzan durante el procesamiento de las peticiones y transformándolas en respuestas HTTP con formato
 * JSON uniforme (MessageResponseDTO). Esto evita tener que repetir bloques try-catch en cada
 * controlador y garantiza que todos los errores sigan la misma estructura, facilitando el manejo
 * de errores del lado del cliente. Se definen tres manejadores específicos: uno para errores de
 * validación de Jakarta Validation (MethodArgumentNotValidException), otro para RuntimeException
 * genéricas, y otro para la excepción personalizada de autorización (SecurityAuthorizationException),
 * cada uno con su código de estado HTTP apropiado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja las excepciones de validación lanzadas por Jakarta Validation cuando un @Valid falla
     * en un controlador. Extrae todos los errores de campo del BindingResult y los concatena en un
     * solo mensaje legible (ej: "Error de validación: username: no debe estar vacío, password: no
     * debe estar vacío"). Devuelve un ResponseEntity con HTTP 400 (BAD_REQUEST) y el mensaje de error
     * compuesto, permitiendo al cliente saber exactamente qué campos no pasaron la validación y por qué.
     *
     * @param ex Excepción lanzada por la validación de un @RequestBody anotado con @Valid.
     * @return ResponseEntity con MessageResponseDTO conteniendo los errores de validación.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new MessageResponseDTO("Error de validación: " + errors));
    }

    /**
     * Manejador genérico para cualquier RuntimeException no capturada por otros manejadores más
     * específicos. Captura excepciones como las que lanzan los servicios cuando un usuario no existe,
     * un número de camiseta ya está registrado, o un jugador ya tiene 3 entrenamientos. Devuelve una
     * respuesta HTTP 500 (INTERNAL_SERVER_ERROR) con el mensaje de la excepción, que en estos casos
     * contiene información útil para el cliente (como "El jugador ya tiene 3 entrenamientos registrados").
     *
     * @param ex Excepción de runtime lanzada por la lógica de negocio.
     * @return ResponseEntity con MessageResponseDTO conteniendo el mensaje de la excepción.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponseDTO> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDTO(ex.getMessage()));
    }

    /**
     * Maneja específicamente las excepciones de autorización (SecurityAuthorizationException) que se
     * lanzan cuando un usuario intenta acceder a un recurso sin el rol adecuado. A diferencia del
     * manejador genérico de RuntimeException, este devuelve HTTP 401 (UNAUTHORIZED) en lugar de 500,
     * que es el código de estado semánticamente correcto para errores de autenticación/autorización.
     * El mensaje de la excepción incluye el rol actual del usuario y la operación que intentaba realizar.
     *
     * @param ex Excepción de autorización lanzada por servicios como UserService.createUser().
     * @return ResponseEntity con MessageResponseDTO y estado HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler(SecurityAuthorizationException.class)
    public ResponseEntity<MessageResponseDTO> handleSecurity(SecurityAuthorizationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDTO(ex.getMessage()));
    }
}
