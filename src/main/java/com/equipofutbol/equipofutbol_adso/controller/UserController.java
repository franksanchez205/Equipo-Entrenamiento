package com.equipofutbol.equipofutbol_adso.controller;

import com.equipofutbol.equipofutbol_adso.dto.MessageResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.UserRequestDTO;
import com.equipofutbol.equipofutbol_adso.dto.UserResponseDTO;
import com.equipofutbol.equipofutbol_adso.entity.Users;
import com.equipofutbol.equipofutbol_adso.service.UserService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que expone los endpoints de gestión de jugadores bajo la ruta base "/user".
 * Está anotado con @RestController y @RequestMapping("/user") para establecer el prefijo común de
 * todas las rutas. Inyecta UserService mediante @Autowired para delegar toda la lógica de negocio
 * relacionada con jugadores: creación, consulta individual y selección del equipo titular (top 5).
 *
 * Expone cuatro endpoints: POST / para crear un nuevo jugador (requiere rol ADMIN), GET / y GET /top5
 * para obtener los 5 mejores jugadores (rutas duplicadas por compatibilidad), y GET /{id} para obtener
 * un jugador por su número de camiseta. Los endpoints GET / y GET /top5 son la funcionalidad principal
 * del sistema, ya que implementan el algoritmo de selección del equipo titular: retornan los 5 jugadores
 * con mayor puntaje promedio junto con el desglose detallado de sus entrenamientos. Aunque el filtro
 * JWT omite la validación para la ruta /user en shouldNotFilter(), el método POST tiene su propia
 * validación de rol ADMIN mediante SecurityContext en UserService.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * Servicio de usuarios inyectado por Spring que contiene la lógica de negocio para crear jugadores,
     * consultar por camiseta y obtener el top 5. Se inyecta mediante @Autowired sobre el campo privado.
     */
    @Autowired
    private UserService userService;

    /**
     * Endpoint POST para crear un nuevo jugador en el sistema. Recibe un UserRequestDTO con nombre,
     * posición, número de camiseta y rol. El parámetro está anotado con @Valid para que Spring active
     * las validaciones de Jakarta Validation (@NotBlank en nombre y posición, @Positive en número de
     * camiseta, @NotNull en rol). Internamente, UserService.createUser() valida además que el usuario
     * autenticado tenga rol ADMINISTRATOR (mediante SecurityContext) y que el número de camiseta no
     * esté duplicado. Si la validación de rol falla, se lanza SecurityAuthorizationException que
     * GlobalExceptionHandler convierte en HTTP 401.
     *
     * @param request DTO con nombre, posición, número de camiseta y rol del nuevo jugador.
     * @return ResponseEntity con MessageResponseDTO confirmando la creación.
     */
    @PostMapping
    public ResponseEntity<MessageResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    /**
     * Endpoint GET que retorna la lista de los 5 jugadores con mayor puntaje promedio (equipo titular).
     * Internamente, UserService.obtenerTop5() consulta todos los jugadores ordenados por puntajeTotal
     * descendente, toma los primeros 5 y los convierte a UserResponseDTO con el detalle completo de
     * sus entrenamientos (incluyendo los aportes ponderados de cada métrica). Si no hay jugadores
     * registrados, retorna una lista vacía. Este endpoint requiere rol ADMINISTRATOR, validado en
     * el servicio mediante SecurityContext.
     *
     * @return ResponseEntity con lista de UserResponseDTO de los 5 mejores jugadores.
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getTop5() {
        List<UserResponseDTO> top5 = userService.obtenerTop5();
        return ResponseEntity.ok(top5);
    }

    /**
     * Endpoint GET alternativo en la ruta /top5 que ejecuta exactamente la misma lógica que GET /.
     * Se incluye como ruta duplicada para proporcionar una URL semánticamente más descriptiva a los
     * consumidores de la API, permitiendo que tanto /user como /user/top5 retornen el equipo titular.
     * Mantiene la misma validación de rol ADMIN que el endpoint GET principal.
     *
     * @return ResponseEntity con lista de UserResponseDTO de los 5 mejores jugadores.
     */
    @GetMapping("/top5")
    public ResponseEntity<List<UserResponseDTO>> getTop5Alt() {
        List<UserResponseDTO> top5 = userService.obtenerTop5();
        return ResponseEntity.ok(top5);
    }

    /**
     * Endpoint GET que retorna los datos de un jugador específico identificado por su número de camiseta.
     * El parámetro {id} en la ruta se vincula al parámetro del método mediante @PathVariable. Aunque
     * el nombre del parámetro es "id", internamente se utiliza como número de camiseta para la búsqueda
     * en UserService.obtenerPuntajePorCamiseta(). Si no existe un jugador con esa camiseta, el servicio
     * lanza RuntimeException que GlobalExceptionHandler convierte en HTTP 500 con el mensaje descriptivo.
     *
     * @param id Número de camiseta del jugador a consultar.
     * @return ResponseEntity con la entidad Users del jugador encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Users> getUserByCamiseta(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.obtenerPuntajePorCamiseta(id));
    }
}
