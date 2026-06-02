package com.equipofutbol.equipofutbol_adso.exception;

/**
 * Excepción personalizada que se lanza cuando un usuario intenta acceder a un recurso o ejecutar una
 * operación para la cual no tiene el rol adecuado. Extiende RuntimeException para que sea una excepción
 * no verificada (unchecked), lo que significa que no obliga al llamante a declararla en la cláusula
 * throws ni a capturarla con try-catch, simplificando el código de los servicios que la utilizan.
 * GlobalExceptionHandler captura específicamente esta excepción mediante @ExceptionHandler y devuelve
 * una respuesta HTTP 401 (UNAUTHORIZED) con un mensaje descriptivo, permitiendo que la seguridad se
 * implemente de manera declarativa sin acoplar la lógica de autorización a los controladores.
 */
public class SecurityAuthorizationException extends RuntimeException {

    /**
     * Constructor que recibe el mensaje descriptivo del error de autorización. El mensaje se construye
     * en el punto donde se lanza la excepción (por ejemplo, en UserService.createUser()) e incluye
     * información contextual como el rol que el usuario tenía y qué rol se requería, facilitando el
     * debugging tanto para el desarrollador como para el consumidor de la API que recibe el error.
     *
     * @param message Descripción del error de autorización.
     */
    public SecurityAuthorizationException(String message) {
        super(message);
    }
}
