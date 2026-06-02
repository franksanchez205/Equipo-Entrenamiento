package com.equipofutbol.equipofutbol_adso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

/**
 * DTO que recibe los datos para crear un nuevo jugador en el sistema. Se utiliza exclusivamente en
 * el endpoint POST /user. El campo "rol" debe enviarse como 1 (JUGADOR), ya que UserService.createUser()
 * asigna forzosamente el rol JUGADOR independientemente del valor recibido, aunque el DTO lo solicita
 * para mantener la consistencia con otros DTOs del sistema. El campo numeroCamiseta se valida con
 * @Positive para garantizar números de camiseta válidos (mayores que cero), aunque no se valida el
 * límite superior. La validación de unicidad del número de camiseta se realiza en el servicio mediante
 * una consulta a la base de datos. La anotación @Data de Lombok genera los getters que Jackson necesita
 * para deserializar el JSON y que UserService utiliza para poblar la entidad Users.
 */
@Data
public class UserRequestDTO {

    /**
     * Nombre completo del jugador. Se almacena en el campo username de la entidad Users con un formato
     * especial: "jugador # <numeroCamiseta>", ya que los jugadores no inician sesión en el sistema y
     * el username no se utiliza para autenticación en este perfil. La validación @NotBlank asegura que
     * el nombre no esté vacío.
     */
    @NotBlank
    private String nombre;

    /**
     * Posición en la que juega el jugador dentro del campo (Delantero, Defensa, Medio, Portero, Lateral, etc.).
     * Es un campo descriptivo que se almacena directamente en la entidad Users y se devuelve en las
     * respuestas de la API. @NotBlank impide que se creen jugadores sin posición definida.
     */
    @NotBlank
    private String posicion;

    /**
     * Número de camiseta del jugador, que debe ser un entero positivo. Actúa como identificador de
     * negocio único, validado mediante existsByNumeroCamiseta() en el servicio. La anotación @Positive
     * de Jakarta Validation rechaza valores menores o iguales a cero antes de que lleguen al servicio.
     */
    @Positive
    private Integer numeroCamiseta;

    /**
     * Rol del usuario que se está creando. Aunque el DTO solicita este campo y lo valida con @NotNull,
     * UserService.createUser() ignora el valor recibido y asigna siempre el rol JUGADOR, ya que este
     * endpoint está diseñado exclusivamente para crear jugadores. El campo se mantiene en el DTO por
     * consistencia con RegisterRequestDTO y para permitir validación a futuro si se decidiera crear
     * administradores desde este endpoint.
     */
    @NotNull
    private Long rol;

    /**
     * Puntaje total del jugador, que se inicializa en 0.0 al crear el jugador. El campo está presente
     * en el DTO pero su valor es ignorado durante la creación, ya que el puntaje se calcula
     * automáticamente a partir de los entrenamientos registrados.
     */
    private Double puntajeTotal;

    /**
     * Método getPassword requerido por alguna interfaz o validación del framework, pero que no tiene
     * sentido en el contexto de creación de jugadores (los jugadores no tienen contraseña). Lanza
     * UnsupportedOperationException si alguien intenta invocarlo, indicando que esta operación no está
     * implementada para este DTO.
     *
     * @return No retorna, siempre lanza excepción.
     * @throws UnsupportedOperationException Siempre.
     */
    public String getPassword() {
        throw new UnsupportedOperationException("Unimplemented method 'getPassword'");
    }

}
