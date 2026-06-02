package com.equipofutbol.equipofutbol_adso.dto;

import lombok.Data;

/**
 * DTO que representa el desglose detallado de un entrenamiento individual dentro de la respuesta
 * del endpoint de top 5 jugadores. A diferencia de la entidad Resultados, que almacena los valores
 * brutos de las métricas, este DTO expresa los aportes ponderados de cada métrica al puntaje total
 * del entrenamiento (aportePases, aporteVelocidad, aportePotencia) junto con el puntaje final del
 * entrenamiento. Se utiliza exclusivamente dentro de UserResponseDTO como parte de la lista de
 * entrenamientos del jugador. La anotación @Data de Lombok genera los getters, setters y el
 * constructor por defecto, permitiendo que UserService.toUserResponseDTO() construya y poble este
 * objeto sin necesidad de escribir métodos repetitivos.
 */
@Data
public class EntrenamientoDTO {

    /**
     * Número del entrenamiento dentro de la secuencia del jugador (1, 2 o 3). Se copia directamente
     * desde el campo numeroEntrenamiento de la entidad Resultados y permite al frontend identificar
     * qué entrenamiento de la semana corresponde cada bloque de estadísticas.
     */
    private Integer numeroEntrenamiento;

    /**
     * Aporte de los pases efectivos al puntaje del entrenamiento, calculado como pasesEfectivos * 0.5.
     * Al separar el aporte en un campo propio, el frontend puede visualizar qué componente de la
     * fórmula está contribuyendo más al puntaje final de cada entrenamiento, facilitando el análisis
     * del rendimiento del jugador.
     */
    private Double aportePases;

    /**
     * Aporte de la velocidad al puntaje del entrenamiento, calculado como velocidadJugador * 0.3.
     * Se incluye como campo independiente para mantener la transparencia en el cálculo del puntaje
     * y permitir que consumidores de la API validen o personalicen la visualización de los resultados.
     */
    private Double aporteVelocidad;

    /**
     * Aporte de la potencia de tiro al puntaje del entrenamiento, calculado como potenciaTiro * 0.2.
     * Aunque tiene el menor peso en la fórmula, se expone igualmente para que el desglose sea completo
     * y el usuario pueda entender cómo se compone el puntaje final.
     */
    private Double aportePotencia;

    /**
     * Puntaje total del entrenamiento, calculado como la suma de los tres aportes ponderados:
     * (pasesEfectivos * 0.5) + (velocidadJugador * 0.3) + (potenciaTiro * 0.2). Es el valor que
     * se promedia con los demás entrenamientos del jugador para determinar su puntajeTotal en la
     * entidad Users, que a su vez decide si el jugador entra en el top 5 del equipo titular.
     */
    private Double puntajeEntrenamiento;

}