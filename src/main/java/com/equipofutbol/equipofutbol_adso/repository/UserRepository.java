package com.equipofutbol.equipofutbol_adso.repository;

import com.equipofutbol.equipofutbol_adso.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Users que centraliza el acceso a datos de usuarios y jugadores.
 * Al extender JpaRepository, Spring Data JPA genera automáticamente la implementación en tiempo de
 * ejecución, proporcionando operaciones CRUD básicas (save, findAll, findById, deleteById) sin que
 * tengamos que escribir una línea de código SQL o JPQL. La anotación @Repository es una especialización
 * de @Component que le indica a Spring que este bean debe ser detectado durante el escaneo de
 * componentes y que además debe traducir las excepciones de persistencia (como DataAccessException)
 * a la jerarquía de excepciones de Spring. Los métodos personalizados que requieren lógica específica
 * de consulta se implementan mediante @Query con JPQL, manteniendo la coherencia con el modelo de
 * objetos en lugar de escribir SQL nativo. El repositorio incluye consultas para autenticación
 * (findByUsername), validación de unicidad (existsByNumeroCamiseta), y el algoritmo de selección de
 * titulares (obtenerPuntajesDeTodosLosJugadores).
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    /**
     * Busca un usuario por su nombre de usuario. Spring Data JPA deriva automáticamente la consulta
     * JPQL a partir del nombre del método siguiendo la convención "findBy" seguida del nombre del
     * campo. Retorna un Optional para que el llamante decida cómo manejar el caso de que el usuario
     * no exista, evitando NullPointerException. Se utiliza tanto en AuthService (register y login)
     * como en AuthService.refreshToken() para verificar que el usuario siga existiendo en la base
     * de datos antes de renovar el token.
     *
     * @param username Nombre de usuario a buscar.
     * @return Optional con el usuario si existe, Optional.empty() si no.
     */
    Optional<Users> findByUsername(String username);

    /**
     * Busca un jugador por su número de camiseta, que funciona como identificador de negocio único.
     * Esta consulta derivada es fundamental en dos flujos críticos: en UserService.createUser() se
     * utiliza indirectamente a través de existsByNumeroCamiseta() para validar que no haya duplicados,
     * y en ResultadoService.createResultado() para localizar al jugador al que se le asignará el
     * resultado del entrenamiento usando el número de camiseta que llega en el JSON.
     *
     * @param numeroCamiseta Número de camiseta del jugador.
     * @return Optional con el jugador si existe, Optional.empty() si no.
     */
    Optional<Users> findByNumeroCamiseta(Integer numeroCamiseta);

    /**
     * Verifica si ya existe un jugador con el número de camiseta indicado. Spring Data JPA deriva
     * la consulta COUNT automáticamente a partir del prefijo "existsBy", retornando true si hay al
     * menos un registro que coincida. Se utiliza en UserService.createUser() como guarda de seguridad
     * antes de persistir un nuevo jugador, evitando la excepción de integridad que lanzaría la base
     * de datos si la columna tuviera una constraint UNIQUE.
     *
     * @param numeroCamiseta Número de camiseta a verificar.
     * @return true si ya existe un jugador con ese número, false si está disponible.
     */
    boolean existsByNumeroCamiseta(Integer numeroCamiseta);

    /**
     * Consulta JPQL personalizada que obtiene el puntaje total de un jugador a partir de su número
     * de camiseta. La anotación @Query recibe el JPQL directamente, y @Param vincula el parámetro
     * ":camiseta" con el argumento del método. Se utiliza en el endpoint GET /user/{id} del
     * UserController para devolver el puntaje del jugador solicitado. El tipo de retorno Optional<Double>
     * permite manejar el caso de que no exista un jugador con esa camiseta sin lanzar excepciones
     * no controladas.
     *
     * @param camiseta Número de camiseta del jugador.
     * @return Optional con el puntaje si el jugador existe, Optional.empty() si no.
     */
    @Query("SELECT u.puntajeTotal FROM Users u WHERE u.numeroCamiseta = :camiseta")
    Optional<Double> obtenerPuntajePorCamiseta(@Param("camiseta") Integer camiseta);

    /**
     * Consulta JPQL que extrae el puntaje total de un usuario a partir de su ID interno de base de
     * datos. A diferencia de obtenerPuntajePorCamiseta, esta consulta se utiliza internamente en
     * servicios donde ya se dispone del ID del usuario (por ejemplo, a partir del token JWT) y no
     * se necesita realizar una búsqueda previa por camiseta. Retorna un Optional<Double> para
     * mantener la consistencia con el resto de métodos del repositorio.
     *
     * @param id Identificador interno del usuario.
     * @return Optional con el puntaje si el usuario existe, Optional.empty() si no.
     */
    @Query("SELECT u.puntajeTotal FROM Users u WHERE u.id = :id")
    Optional<Double> obtenerPuntaje(@Param("id") Long id);

    /**
     * Consulta JPQL que retorna todos los usuarios cuyo rol es JUGADOR, ordenados por puntajeTotal
     * de mayor a menor. Es la consulta clave para el algoritmo de selección del equipo titular: el
     * método UserService.obtenerTop5() ejecuta esta consulta y luego aplica limit(5) sobre el stream
     * para quedarse con los 5 mejores. La condición WHERE u.role = 'JUGADOR' filtra explícitamente
     * a los administradores, ya que estos no participan en los entrenamientos y no deberían aparecer
     * en la tabla de posiciones. El ORDER BY DESC garantiza que el jugador con mayor puntaje aparezca
     * primero en la lista.
     *
     * @return Lista de jugadores ordenados por puntaje descendente.
     */
    @Query("SELECT u FROM Users u WHERE u.role = 'JUGADOR' ORDER BY u.puntajeTotal DESC")
    List<Users> obtenerPuntajesDeTodosLosJugadores();
}
