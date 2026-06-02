package com.equipofutbol.equipofutbol_adso.entity;

import com.equipofutbol.equipofutbol_adso.enums.UserRole;
import jakarta.persistence.*;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Entidad central del dominio que representa a un usuario del sistema, ya sea un administrador
 * o un jugador del equipo de fútbol. Está anotada con @Entity para que JPA la reconozca como
 * una entidad persistente y con @Table(name = "users") para mapearla explícitamente a la tabla
 * "users" en MySQL, evitando posibles conflictos con palabras reservadas del motor de base de
 * datos. La anotación @Data de Lombok genera automáticamente los getters, setters, toString,
 * equals y hashCode, reduciendo significativamente el boilerplate y manteniendo el código limpio.
 * La clase incluye una relación @OneToMany hacia Resultados, lo que permite navegar desde un
 * jugador a todos sus entrenamientos registrados. El atributo puntajeTotal se actualiza cada vez
 * que se persiste un nuevo resultado, sirviendo como base para el algoritmo que selecciona a los
 * 5 jugadores titulares con mejor rendimiento promedio.
 */
@Data
@Entity
@Table(name = "users")
public class Users {

    /**
     * Identificador único autogenerado por la base de datos mediante la estrategia IDENTITY,
     * que delega en el auto-increment de MySQL para asignar el valor secuencialmente.
     * Este ID se utiliza como clave foránea en la entidad Resultados y como claim "userId"
     * dentro del token JWT para identificar al usuario autenticado sin necesidad de consultar
     * la base de datos en cada petición.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de usuario único utilizado para la autenticación. Se valida en AuthService.register()
     * mediante una consulta JPA que verifica que no exista previamente, garantizando la unicidad
     * a nivel de aplicación (además de poder agregar una constraint UNIQUE en la base de datos).
     */
    private String username;

    /**
     * Contraseña cifrada con BCrypt mediante el PasswordEncoder configurado en AppConfig.
     * Nunca se almacena en texto plano: el proceso de registro aplica passwordEncoder.encode()
     * y el de login verifica con passwordEncoder.matches(). El hash generado por BCrypt incluye
     * un salt aleatorio incorporado, lo que hace que dos usuarios con la misma contraseña tengan
     * hashes diferentes y protege contra ataques de rainbow tables.
     */
    private String password;

    /**
     * Rol del usuario mapeado como String en la base de datos gracias a @Enumerated(EnumType.STRING).
     * Se eligió STRING en lugar de ORDINAL para que la columna almacene valores como "ADMINISTRATOR"
     * o "JUGADOR" en lugar de números 0/1, lo que hace que la base de datos sea auto-documentada
     * y evita errores si en el futuro se agregan o reordenan valores en el enum. La columna se
     * marca como nullable = false porque todo usuario debe tener un rol definido.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * Posición en la que juega el jugador dentro del campo (Delantero, Defensa, Medio, Portero, etc.).
     * Es un campo de texto libre que el administrador define al crear el jugador. Para los usuarios
     * con rol ADMINISTRATOR este campo puede permanecer null, ya que los administradores no participan
     * en los entrenamientos ni en la selección del equipo titular.
     */
    private String posicion;

    /**
     * Número de camiseta del jugador, que actúa como identificador único de negocio (no confundir con
     * el ID de base de datos). Se valida en UserService.createUser() mediante existsByNumeroCamiseta()
     * para garantizar que no haya dos jugadores con el mismo número. Además, el endpoint GET /user/{id}
     * lo utiliza para buscar jugadores, y el servicio ResultadoService lo recibe en el DTO para asociar
     * el entrenamiento al jugador correcto.
     */
    private Integer numeroCamiseta;

    /**
     * Puntaje total acumulado del jugador, calculado como el promedio de los puntajes de todos sus
     * entrenamientos registrados. Se actualiza en ResultadoService.actualizarPuntajeTotal() cada vez
     * que se guarda un nuevo resultado. Es el campo clave sobre el que se basa el algoritmo de
     * selección de los 5 jugadores titulares: el repositorio EmployeesRepository.obtenerPuntajesDeTodosLosJugadores()
     * ordena a los jugadores por este campo en orden descendente, y UserService.obtenerTop5() toma
     * los primeros 5 para conformar el equipo titular.
     */
    private Double puntajeTotal;

    /**
     * Indica si la cuenta del usuario está activa. Se inicializa en true por defecto tanto a nivel
     * de Java (en la declaración del campo) como en el constructor de la entidad. El servicio de
     * autenticación AuthService.login() verifica este flag antes de generar el token JWT: si el
     * usuario fue desactivado por un administrador, no podrá iniciar sesión aunque sus credenciales
     * sean correctas, permitiendo un control de acceso granular sin eliminar registros de la base
     * de datos.
     */
    private Boolean active = true;

    /**
     * Fecha y hora en que se creó el registro del usuario. Se asigna automáticamente en el momento
     * de la instanciación mediante LocalDateTime.now(), pero también podría gestionarse a través de
     * @PrePersist o @CreatedDate de Spring Data JPA. Se mapea explícitamente a la columna "created_at"
     * usando @Column con un nombre personalizado para seguir la convención snake_case de la base de
     * datos, que difiere del estilo camelCase usado en Java.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Lista de entrenamientos (Resultados) asociados a este usuario, mapeada mediante una relación
     * @OneToMany donde el lado propietario es la entidad Resultados (mappedBy = "users"). La estrategia
     * CascadeType.ALL propaga todas las operaciones (persist, merge, remove) desde el jugador hacia
     * sus resultados, lo que significa que al eliminar un jugador también se eliminarán todos sus
     * entrenamientos en cascada. El fetch se configura como LAZY para evitar cargar todos los
     * entrenamientos cada vez que se consulta un jugador, optimizando el rendimiento de las consultas
     * que solo necesitan los datos básicos. Se inicializa como ArrayList vacío para evitar
     * NullPointerException al agregar elementos sin tener que instanciarlo explícitamente en cada
     * método que lo utiliza.
     */
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Resultados> listResultados = new ArrayList<>();

}