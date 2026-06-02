package com.equipofutbol.equipofutbol_adso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * DTO que encapsula la solicitud de registro de un nuevo usuario en el sistema. El cliente envía
 * nombre de usuario, contraseña y un valor numérico para el rol (0 = ADMINISTRATOR, 1 = JUGADOR) que
 * posteriormente se convierte al enum UserRole en AuthService.register(). Se utiliza exclusivamente
 * en el endpoint POST /auth/register. Las anotaciones de Jakarta Validation (@NotBlank, @NotNull)
 * delegan la validación de entrada en Spring antes de que el controlador invoque al servicio,
 * garantizando que los datos lleguen en el formato esperado y reduciendo la cantidad de validaciones
 * manuales en la capa de servicio.
 */
@Data
public class RegisterRequestDTO {

    /**
     * Nombre de usuario deseado para la nueva cuenta. La validación @NotBlank asegura que no se
     * puedan registrar usuarios con nombre vacío. AuthService.register() verifica además que este
     * username no exista previamente en la base de datos mediante una consulta a EmployeesRepository,
     * lanzando una excepción si ya está registrado para mantener la unicidad del campo.
     */
    @NotBlank
    private String username;

    /**
     * Contraseña en texto plano que será cifrada con BCrypt antes de almacenarse. La validación
     * @NotBlank evita contraseñas vacías, pero no impone políticas de complejidad (longitud mínima,
     * caracteres especiales, etc.), que podrían agregarse en el futuro mediante una anotación
     * @Pattern o una validación personalizada.
     */
    @NotBlank
    private String password;

    /**
     * Valor numérico del rol que se asignará al nuevo usuario. Debe ser 0 para ADMINISTRATOR o 1 para
     * JUGADOR. Se mapea como Long para facilitar la conversión a entero y el acceso por índice al
     * array de UserRole.values(). La validación @NotNull impide que se omita el campo, y el servicio
     * adicionalmente valida que el índice esté dentro del rango permitido, lanzando una excepción con
     * un mensaje claro si el cliente envía un valor inválido como 2 o -1.
     */
    @NotNull
    private Long rol;

}
