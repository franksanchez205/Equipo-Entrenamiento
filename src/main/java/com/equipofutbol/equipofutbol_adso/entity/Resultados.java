package com.equipofutbol.equipofutbol_adso.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Entidad que representa el resultado de un entrenamiento individual registrado para un jugador.
 * Cada instancia almacena las métricas de rendimiento de un jugador en una sesión de entrenamiento
 * específica (pases efectivos, potencia de tiro y velocidad), junto con el número de entrenamiento
 * (1, 2 o 3) y una referencia al jugador al que pertenece. La tabla se mapea como "resultados" en la
 * base de datos. La anotación @Data de Lombok se encarga de generar getters, setters y los métodos
 * toString, equals y hashCode. La relación @ManyToOne apunta a Users y establece una clave foránea
 * llamada "users_id" que permite navegar desde el resultado hasta el jugador, pero también desde el
 * jugador hasta sus resultados gracias al mappedBy definido en la entidad Users. Cada jugador puede
 * tener hasta 3 resultados (validado en ResultadoService.createResultado()), y el promedio del puntaje
 * calculado a partir de estos registros determina el puntajeTotal del jugador, que a su vez decide
 * si forma parte del equipo titular (top 5).
 */
@Data
@Entity
@Table(name = "resultados")
public class Resultados {

    /**
     * Identificador único autogenerado por la base de datos. Se utiliza internamente como clave primaria
     * y no se expone directamente en las respuestas de la API (el frontend referencia los resultados
     * a través del número de entrenamiento y la camiseta del jugador).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número secuencial del entrenamiento dentro de la semana (1, 2 o 3). Se utiliza en la validación
     * de negocio que limita a 3 entrenamientos por jugador (ResultadoRepository.countNumeroEntrenamiento()
     * cuenta cuántos registros existen para un usuario). Aunque en la versión actual el sistema no exige
     * los 3 entrenamientos para calcular el top 5, el diseño contempla esta validación para futuras
     * iteraciones donde se requiera que los jugadores hayan completado la semana completa de entrenamientos
     * antes de ser elegibles para el equipo titular.
     */
    private Integer numeroEntrenamiento;

    /**
     * Cantidad de pases efectivos realizados por el jugador durante el entrenamiento. Es una de las tres
     * métricas de rendimiento que alimentan la fórmula de cálculo del puntaje: pasesEfectivos * 0.5.
     * Representa el 50% del peso total del puntaje del entrenamiento, siendo la componente con mayor
     * ponderación, lo que refleja la importancia que el algoritmo asigna a la precisión en los pases
     * como indicador de rendimiento.
     */
    private Double pasesEfectivos;

    /**
     * Potencia de tiro alcanzada por el jugador durante la sesión de entrenamiento. Contribuye al
     * puntaje con un peso del 20% (potenciaTiro * 0.2). Aunque tiene la menor ponderación de las tres
     * métricas, sigue siendo relevante para diferenciar jugadores con estadísticas similares en pases y
     * velocidad, actuando como factor de desempate en la clasificación final.
     */
    private Double potenciaTiro;

    /**
     * Velocidad alcanzada por el jugador durante el entrenamiento. Aporta un 30% al puntaje total
     * mediante la fórmula velocidadJugador * 0.3. Esta ponderación intermedia reconoce la velocidad
     * como una cualidad importante en el fútbol moderno, pero sin equipararla al peso de los pases
     * efectivos que siguen siendo el indicador principal de rendimiento.
     */
    private Double velocidadJugador;

    /**
     * Referencia ManyToOne hacia el jugador (Users) al que pertenece este resultado de entrenamiento.
     * La clave foránea se almacena en la columna "users_id" de la tabla resultados. El fetch se
     * configura como LAZY para que JPA no cargue automáticamente los datos completos del jugador cada
     * vez que se consulta un resultado, mejorando el rendimiento de las consultas. Esta es la relación
     * propietaria (dueña de la clave foránea), mientras que en Users se define el mappedBy que apunta
     * al campo "users" aquí declarado para completar la relación bidireccional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id")
    @JsonIgnoreProperties("listResultados")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Users users;
}
