package com.equipofutbol.equipofutbol_adso.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * DTO que encapsula la solicitud de inicio de sesión. Contiene únicamente el nombre de usuario y la
 * contraseña que el cliente envía mediante POST /auth/login. Ambos campos están anotados con @NotBlank
 * de Jakarta Validation, lo que garantiza que Spring rechace automáticamente la petición con un error
 * 400 si alguno de ellos está vacío o es null, sin necesidad de validaciones manuales en el servicio.
 * La anotación @Data de Lombok genera los getters que Jackson utiliza para deserializar el JSON del
 * cuerpo de la petición y que AuthService.login() utiliza para acceder a los valores.
 */
@Data
public class LoginRequestDTO {

    /**
     * Nombre de usuario registrado en el sistema. La validación @NotBlank asegura que el campo no sea
     * null, no esté vacío y no contenga solo espacios en blanco. AuthService.login() utiliza este valor
     * para buscar al usuario en la base de datos mediante EmployeesRepository.findByUsername().
     */
    @NotBlank
    private String username;

    /**
     * Contraseña del usuario en texto plano (se cifrará del lado del servidor para la comparación).
     * Aunque viaja en texto plano por HTTPS, el servicio la compara con el hash BCrypt almacenado
     * usando PasswordEncoder.matches(), sin almacenarla ni exponerla en logs. La validación @NotBlank
     * impide que se envíen contraseñas vacías.
     */
    @NotBlank
    private String password;

}
