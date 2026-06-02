package com.equipofutbol.equipofutbol_adso.dto;

import java.util.List;

import lombok.Data;

/**
 * DTO que encapsula la respuesta con los datos de un jugador y el detalle completo de sus
 * entrenamientos. Se utiliza principalmente en el endpoint GET /user/top5 (y GET /user) para devolver
 * la lista de los mejores jugadores con información enriquecida. A diferencia de la entidad Users,
 * que incluye campos internos como password, este DTO expone solo la información relevante para el
 * consumidor de la API: datos básicos del jugador y una lista de objetos EntrenamientoDTO con el
 * desglose de cada entrenamiento. La anotación @Data de Lombok genera los getters que Jackson necesita
 * para serializar la respuesta a JSON.
 */
@Data
public class UserResponseDTO {

    /**
     * Identificador interno del jugador en la base de datos. Se obtiene del campo id de la entidad
     * Users y permite al frontend referenciar al jugador de forma única si necesita hacer consultas
     * adicionales.
     */
    private Long id;

    /**
     * Nombre del jugador. Se obtiene indirectamente del campo username de la entidad Users y representa
     * el nombre visible del jugador en la interfaz de usuario.
     */
    private String nombre;

    /**
     * Posición en la que juega el jugador (Delantero, Defensa, etc.). Proviene directamente del campo
     * posicion de la entidad Users.
     */
    private String posicion;

    /**
     * Número de camiseta del jugador, que actúa como identificador de negocio visible y es el campo
     * que los clientes HTTP utilizan para buscar jugadores y asociar resultados de entrenamiento.
     */
    private Integer numeroCamiseta;

    /**
     * Puntaje total promedio del jugador calculado a partir de todos sus entrenamientos registrados.
     * Es el valor que determina la posición del jugador en el ranking y decide si forma parte del
     * equipo titular (top 5).
     */
    private Double puntajeTotal;

    /**
     * Lista de objetos EntrenamientoDTO que contiene el detalle de cada entrenamiento del jugador:
     * número de entrenamiento, aportes ponderados de cada métrica y puntaje del entrenamiento. Se
     * construye en UserService.toUserResponseDTO() iterando sobre listResultados de la entidad Users.
     */
    private List<EntrenamientoDTO> entrenamientos;
    
}