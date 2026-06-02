package com.equipofutbol.equipofutbol_adso.dto;

import lombok.Data;

/**
 * DTO que recibe los datos de un resultado de entrenamiento desde el cliente HTTP. El campo "users"
 * contiene el número de camiseta del jugador como String (no el ID interno), y es responsabilidad de
 * ResultadoService.createResultado() convertirlo a Integer y buscar al jugador correspondiente en la
 * base de datos mediante EmployeesRepository.findByNumeroCamiseta(). El diseño del DTO expone los
 * tres valores métricos (pasesEfectivos, potenciaTiro, velocidadJugador) que alimentarán la fórmula
 * de cálculo del puntaje del entrenamiento. La anotación @Data de Lombok genera los getters necesarios
 * para que Jackson deserialice el JSON del cuerpo de la petición y para que el servicio acceda a los
 * valores.
 */
@Data
public class ResultadoRequestDTO {

    /**
     * Identificador del resultado (opcional en la creación, ya que la base de datos lo genera
     * automáticamente mediante auto-increment). Se incluye en el DTO por si en el futuro se habilita
     * la actualización de resultados existentes, pero actualmente no se utiliza en el flujo de creación.
     */
    private Long id;

    /**
     * Número del entrenamiento dentro de la semana (1, 2 o 3). Se almacena directamente en la entidad
     * Resultados y se utiliza actualmente solo con fines informativos dentro del desglose del DTO de
     * respuesta. La validación del máximo de 3 entrenamientos se realiza contando los registros existentes
     * del jugador, no validando el valor de este campo.
     */
    private Integer numeroEntrenamiento;

    /**
     * Cantidad de pases efectivos del jugador en este entrenamiento. Es el componente con mayor peso
     * en la fórmula del puntaje (50%), por lo que valores altos aquí impactan significativamente en el
     * puntajeTotal del jugador y en su posición en el ranking del equipo titular.
     */
    private Double pasesEfectivos;

    /**
     * Potencia de tiro alcanzada durante el entrenamiento. Aporta un 20% al puntaje del entrenamiento
     * y, aunque tiene el menor peso de las tres métricas, puede ser determinante como factor de desempate
     * entre jugadores con estadísticas similares en pases y velocidad.
     */
    private Double potenciaTiro;

    /**
     * Velocidad del jugador registrada durante la sesión de entrenamiento. Representa el 30% del puntaje
     * del entrenamiento y refleja la importancia de la velocidad como atributo diferenciador en el
     * rendimiento futbolístico.
     */
    private Double velocidadJugador;

    /**
     * Número de camiseta del jugador al que pertenece este resultado, enviado como String. Se almacena
     * como texto en el DTO porque el cliente lo envía como cadena en el JSON (por ejemplo, "9" en lugar
     * de 9). ResultadoService.createResultado() lo convierte a Integer con Integer.parseInt() y lo
     * utiliza para buscar al jugador en la base de datos. Es importante destacar que el nombre del campo
     * es "users" (en plural) por convención del DTO, pero hace referencia a un único jugador.
     */
    private String users;
}
