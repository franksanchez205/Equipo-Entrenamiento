package com.equipofutbol.equipofutbol_adso.repository;

import com.equipofutbol.equipofutbol_adso.entity.Resultados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Resultados que gestiona el acceso a datos de los entrenamientos
 * registrados para cada jugador. Al extender JpaRepository, hereda todas las operaciones CRUD
 * estándar y, además, define un método personalizado con @Query para contar cuántos entrenamientos
 * tiene registrados un jugador específico. Esta consulta de conteo es fundamental para la regla de
 * negocio que limita a un máximo de 3 entrenamientos por jugador. La anotación @Repository registra
 * esta interfaz como un bean de Spring que puede ser inyectado mediante @Autowired en los servicios
 * que lo necesiten, como ResultadoService.
 */
@Repository
public interface ResultadoRepository extends JpaRepository<Resultados, Long> {

    /**
     * Cuenta el número de entrenamientos registrados para un usuario específico a partir de su ID.
     * La consulta JPQL utiliza COUNT(r) para contar las filas de la tabla resultados cuya clave
     * foránea r.users.id coincida con el parámetro proporcionado. El resultado se utiliza en
     * ResultadoService.createResultado() como validación previa a la persistencia: si count >= 3,
     * se lanza una RuntimeException que impide registrar más entrenamientos para ese jugador.
     * Esta validación se realiza a nivel de aplicación (no de base de datos) para poder devolver
     * un mensaje de error descriptivo al cliente HTTP.
     *
     * @param userId ID del usuario cuyos entrenamientos se quieren contar.
     * @return Número entero de entrenamientos registrados (0, 1, 2 o 3).
     */
    @Query("SELECT COUNT(r) FROM Resultados r WHERE r.users.id = :userId")
    int countNumeroEntrenamiento(@Param("userId") Long userId);
}
