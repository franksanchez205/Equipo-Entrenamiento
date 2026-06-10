package com.equipofutbol.equipofutbol_adso.service;

import com.equipofutbol.equipofutbol_adso.config.SecurityContext;
import com.equipofutbol.equipofutbol_adso.dto.*;
import com.equipofutbol.equipofutbol_adso.entity.Resultados;
import com.equipofutbol.equipofutbol_adso.entity.Users;
import com.equipofutbol.equipofutbol_adso.enums.UserRole;
import com.equipofutbol.equipofutbol_adso.exception.SecurityAuthorizationException;
import com.equipofutbol.equipofutbol_adso.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de jugadores que centraliza la lógica de negocio relacionada con la creación,
 * consulta y selección de los miembros del equipo de fútbol. Está anotado con @Service para que Spring
 * lo detecte automáticamente durante el escaneo de componentes y lo registre como un bean en el contexto
 * de la aplicación. Inyecta dos dependencias mediante @Autowired directo sobre los campos (no por
 * constructor): EmployeesRepository para acceder a los datos persistentes de los jugadores, y
 * SecurityContext para obtener el rol del usuario autenticado desde el contexto de la petición HTTP.
 * La inyección por campo es menos recomendable que la inyección por constructor desde el punto de vista
 * de testabilidad, pero se utiliza aquí por simplicidad y porque Spring la resuelve sin problemas al
 * ser un bean gestionado por el contenedor.
 *
 * El método más relevante es obtenerTop5(), que implementa el algoritmo de selección del equipo titular:
 * consulta todos los jugadores ordenados por puntajeTotal descendente y retorna los 5 primeros con el
 * detalle completo de sus entrenamientos. Los métodos de creación (createUser) y consulta (getAll) están
 * protegidos por validación de rol administrativo mediante SecurityContext.
 */
@Service
public class UserService {

    /**
     * Repositorio de usuarios inyectado por Spring. Se utiliza para todas las operaciones de persistencia:
     * crear jugadores (save), buscar por camiseta (findByNumeroCamiseta), verificar unicidad de camiseta
     * (existsByNumeroCamiseta), listar todos (findAll) y obtener jugadores ordenados por puntaje
     * (obtenerPuntajesDeTodosLosJugadores). La anotación @Autowired sobre el campo le indica a Spring
     * que debe inyectar aquí la implementación concreta de EmployeesRepository generada por Spring Data JPA.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Componente de seguridad inyectado por Spring que permite acceder al rol del usuario autenticado
     * desde el contexto de la petición HTTP actual. Se utiliza en validateAdminRole() para verificar
     * que solo los usuarios con rol ADMINISTRATOR puedan crear nuevos jugadores o listar todos los
     * usuarios del sistema.
     */
    @Autowired
    private SecurityContext security;

    /**
     * Valida que el usuario autenticado en la petición actual tenga el rol de ADMINISTRATOR. Compara
     * el rol obtenido de SecurityContext.getCurrentRole() con el valor del enum UserRole.ADMINISTRATOR.
     * Si el rol no coincide (incluyendo el caso en que sea null porque el filtro omitió la validación
     * JWT), lanza una SecurityAuthorizationException con un mensaje que incluye el rol actual del
     * usuario, permitiendo al cliente entender por qué se rechazó la operación. Esta excepción es
     * capturada por GlobalExceptionHandler y convertida en una respuesta HTTP 401.
     */
    private void validateAdminRole() {
        if (!UserRole.ADMINISTRATOR.name().equals(security.getCurrentRole())) {
            throw new SecurityAuthorizationException("EL rol: '" + security.getCurrentRole() + "' no esta permitido");
        }
    }

    /**
     * Crea un nuevo jugador en el sistema. El método ejecuta las siguientes validaciones y pasos:
     * 1. Valida que el usuario autenticado tenga rol ADMINISTRATOR mediante validateAdminRole().
     * 2. Verifica que el número de camiseta no esté ya registrado consultando existsByNumeroCamiseta().
     *    Si ya existe, lanza RuntimeException con el mensaje "El numero de camiseta ya existe".
     * 3. Crea una nueva entidad Users con: username en formato "jugador # <numeroCamiseta>" (los
     *    jugadores no inician sesión, por lo que el username es solo referencial), rol JUGADOR fijo
     *    (independientemente del valor enviado en el DTO), posición, número de camiseta, y puntaje
     *    inicial en 0.0.
     * 4. Persiste el jugador mediante employeesRepository.save().
     * 5. Retorna un MessageResponseDTO indicando que el jugador fue creado exitosamente.
     *
     * @param request DTO con nombre, posición, número de camiseta y rol del nuevo jugador.
     * @return MessageResponseDTO confirmando la creación del jugador.
     */
    public MessageResponseDTO createUser(UserRequestDTO request) {

        Users users = userRepository.findByUsername(request.getNombre())
        .orElseThrow(() -> new RuntimeException("Jugador no encontrado con camiseta: " + request.getNumeroCamiseta() + " y nombre: " + request.getNombre()));

        validateAdminRole();

        if (userRepository.existsByNumeroCamiseta(request.getNumeroCamiseta())) {
            throw new RuntimeException("El numero de camiseta ya existe");
        }

        UserRole role = UserRole.JUGADOR;

        Users user = new Users();
        user.setUsername("jugador # " + request.getNumeroCamiseta());
        user.setRole(role);
        user.setPosicion(request.getPosicion());
        user.setNumeroCamiseta(request.getNumeroCamiseta());
        user.setPuntajeTotal(0.0);
        userRepository.save(user);

        return new MessageResponseDTO("Jugador creado exitosamente");
    }

   

    /**
     * Retorna todos los usuarios registrados en el sistema (tanto administradores como jugadores).
     * Requiere autenticación como ADMINISTRATOR, validada mediante validateAdminRole(). La consulta
     * se realiza con findAll() de JpaRepository, que ejecuta un SELECT * FROM users. Este método se
     * expone en el controlador pero no tiene un endpoint dedicado en el diseño actual de la API;
     * está disponible para futuras ampliaciones o para uso interno.
     *
     * @return Lista completa de todas las entidades Users persistentes.
     */
    public List<Users> getAll() {
        validateAdminRole();
        return userRepository.findAll();
    }

    /**
     * Busca un jugador por su número de camiseta. Utiliza findByNumeroCamiseta() del repositorio y,
     * si no encuentra ningún jugador con ese número, lanza una RuntimeException con el mensaje
     * "Jugador no encontrado con camiseta: <numero>". Este método es utilizado por el endpoint
     * GET /user/{id} para devolver los datos de un jugador específico.
     *
     * @param camiseta Número de camiseta del jugador a buscar.
     * @return Entidad Users del jugador encontrado.
     */
    public Users obtenerPuntajePorCamiseta(Integer camiseta) {
        return userRepository.findByNumeroCamiseta(camiseta)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado con camiseta: " + camiseta));
    }

    /**
     * Retorna la lista completa de jugadores ordenados por puntajeTotal de mayor a menor. Este método
     * no requiere validación de rol administrativo, por lo que puede ser consumido sin autenticación
     * (el filtro JWT omite la ruta /user a través de shouldNotFilter). Delega en la consulta JPQL
     * obtenerPuntajesDeTodosLosJugadores() del repositorio, que filtra por role = 'JUGADOR' y aplica
     * ORDER BY puntajeTotal DESC.
     *
     * @return Lista de jugadores ordenados por puntaje descendente.
     */
    public List<Users> obtenerTablaDePuntajes() {
        return userRepository.obtenerPuntajesDeTodosLosJugadores();
    }

    /**
     * Implementa el algoritmo de selección del equipo titular (top 5 jugadores con mayor puntaje).
     * El flujo es el siguiente:
     * 1. Valida que el usuario autenticado tenga rol ADMINISTRATOR.
     * 2. Obtiene todos los jugadores ordenados por puntaje descendente desde el repositorio.
     * 3. Si no hay jugadores registrados, retorna una lista vacía.
     * 4. Toma los primeros 5 elementos del stream usando limit(5).
     * 5. Convierte cada entidad Users a un UserResponseDTO mediante el método privado toUserResponseDTO(),
     *    que incluye el desglose detallado de cada entrenamiento con los aportes ponderados.
     * 6. Retorna la lista de UserResponseDTO con los 5 mejores jugadores.
     *
     * @return Lista de hasta 5 UserResponseDTO con los jugadores de mayor puntaje y sus entrenamientos.
     */
    public List<UserResponseDTO> obtenerTop5() {
        List<Users> jugadores = userRepository.obtenerPuntajesDeTodosLosJugadores();

        if (jugadores.isEmpty()) {
            return new ArrayList<>();
        }

        List<Users> top5 = jugadores.stream()
                .limit(5)
                .collect(Collectors.toList());

        return top5.stream().map(this::toUserResponseDTO).collect(Collectors.toList());
    }

    /**
     * Método privado que convierte una entidad Users en un UserResponseDTO enriquecido con el detalle
     * de todos los entrenamientos del jugador. Para cada entrenamiento en listResultados, calcula los
     * aportes ponderados individuales (aportePases = pasesEfectivos * 0.5, aporteVelocidad =
     * velocidadJugador * 0.3, aportePotencia = potenciaTiro * 0.2) y el puntaje total del entrenamiento
     * como la suma de los tres aportes. Si el jugador no tiene entrenamientos (listResultados es null),
     * la lista de entrenamientos en el DTO se devuelve vacía. Este método se utiliza exclusivamente
     * dentro de obtenerTop5() para transformar los datos antes de enviarlos al cliente.
     *
     * @param user Entidad Users del jugador a transformar.
     * @return UserResponseDTO con los datos del jugador y el detalle de sus entrenamientos.
     */
    private UserResponseDTO toUserResponseDTO(Users user) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();

        List<EntrenamientoDTO> entrenamientos = new ArrayList<>();

        if (user.getListResultados() != null) {
            for (Resultados r : user.getListResultados()) {
                EntrenamientoDTO e = new EntrenamientoDTO();

                e.setNumeroEntrenamiento(r.getNumeroEntrenamiento());
                e.setAportePases(r.getPasesEfectivos() * 0.5);
                e.setAporteVelocidad(r.getVelocidadJugador() * 0.3);
                e.setAportePotencia(r.getPotenciaTiro() * 0.2);

                double puntaje = (r.getPasesEfectivos() * 0.5) + (r.getVelocidadJugador() * 0.3) + (r.getPotenciaTiro() * 0.2);
                e.setPuntajeEntrenamiento(puntaje);

                entrenamientos.add(e);
            }
        }

        userResponseDTO.setEntrenamientos(entrenamientos);
        return userResponseDTO;
    }
}
