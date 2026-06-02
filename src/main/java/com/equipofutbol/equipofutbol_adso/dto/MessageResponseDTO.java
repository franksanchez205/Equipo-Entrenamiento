package com.equipofutbol.equipofutbol_adso.dto;

import lombok.Data;

/**
 * DTO genérico de respuesta utilizado en toda la API para devolver mensajes de texto simples al
 * cliente, tanto en respuestas exitosas como en errores controlados. Su versatilidad permite usarlo
 * en múltiples contextos: mensaje de éxito al registrar un usuario ("Empleado registrado exitosamente"),
 * mensaje de error de validación ("Error de validación: campo: mensaje"), o cualquier otra notificación
 * de texto que deba comunicarse al consumidor de la API. GlobalExceptionHandler también lo utiliza
 * como cuerpo de las respuestas de error, garantizando una estructura uniforme en todas las respuestas
 * de la aplicación. La anotación @Data de Lombok genera getters y setters, y los dos constructores
 * (vacío y con mensaje) permiten tanto la creación mediante setters como la creación directa con el
 * mensaje predefinido.
 */
@Data
public class MessageResponseDTO {

    /**
     * Texto del mensaje que se devuelve al cliente. Puede contener cualquier tipo de notificación:
     * confirmación de operación exitosa, descripción de un error de validación o advertencia de
     * seguridad. La flexibilidad de este campo permite que MessageResponseDTO sea el DTO de respuesta
     * más reutilizado en la aplicación.
     */
    private String message;

    /**
     * Constructor sin argumentos requerido por Jackson para la deserialización en caso de que se
     * necesite construir el objeto a partir de un JSON. Aunque en la práctica este DTO solo se usa
     * como respuesta (serialización), se incluye el constructor vacío por compatibilidad y para
     * cumplir con las convenciones de Java Beans.
     */
    public MessageResponseDTO() {}

    /**
     * Constructor con el mensaje que permite crear una instancia lista para usar en una sola línea.
     * Es el constructor más utilizado en los servicios y controladores: authService.register() retorna
     * un new MessageResponseDTO("Empleado registrado exitosamente"), y GlobalExceptionHandler construye
     * las respuestas de error con el mensaje de la excepción.
     *
     * @param message Texto del mensaje a devolver al cliente.
     */
    public MessageResponseDTO(String message) {
        this.message = message;
    }
}
