package com.equipofutbol.equipofutbol_adso.controller;

import com.equipofutbol.equipofutbol_adso.dto.MessageResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.ResultadoRequestDTO;
import com.equipofutbol.equipofutbol_adso.service.ResultadoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que expone los endpoints para el registro de resultados de entrenamiento bajo la
 * ruta base "/resultados". Está anotado con @RestController para que Spring serialice automáticamente
 * las respuestas a JSON, y con @RequestMapping("/resultados") para establecer el prefijo de la ruta.
 * Inyecta ResultadoService mediante @Autowired para delegar en la capa de servicio toda la lógica de
 * registro y actualización de puntajes. Este controlador expone un único endpoint POST que recibe los
 * datos del entrenamiento (métricas de rendimiento y número de camiseta del jugador), los valida y
 * persiste. El filtro JWT (JwtValidationFilter) omite la validación para esta ruta en shouldNotFilter(),
 * por lo que el registro de resultados es accesible sin autenticación, simplificando el flujo de
 * pruebas desde Postman o desde aplicaciones cliente que no manejan tokens JWT.
 */
@RestController
@RequestMapping("/resultados")
public class ResponseController {

    /**
     * Servicio de resultados inyectado por Spring que contiene la lógica de negocio para registrar
     * entrenamientos y actualizar puntajes de los jugadores. Se inyecta mediante @Autowired sobre
     * el campo privado, permitiendo que Spring resuelva automáticamente la dependencia.
     */
    @Autowired
    private ResultadoService resultadoService;

    /**
     * Endpoint POST para registrar un nuevo resultado de entrenamiento. Recibe un ResultadoRequestDTO
     * en el cuerpo de la petición con los siguientes campos: numeroEntrenamiento (1, 2 o 3),
     * pasesEfectivos, potenciaTiro, velocidadJugador (las tres métricas de rendimiento), y users
     * (número de camiseta del jugador como String). Delega en ResultadoService.createResultado(),
     * que busca al jugador por su camiseta, valida que no tenga más de 3 entrenamientos, persiste
     * el resultado y actualiza el puntaje promedio del jugador. A diferencia de los endpoints de
     * autenticación, este método no utiliza @Valid porque el DTO ResultadoRequestDTO no tiene
     * anotaciones de validación, asumiendo que la validación de los datos se realiza en el servicio.
     *
     * @param request DTO con los datos del entrenamiento y el número de camiseta del jugador.
     * @return ResponseEntity con MessageResponseDTO indicando el resultado del registro.
     */
    @PostMapping
    public ResponseEntity<MessageResponseDTO> createResultado(@RequestBody ResultadoRequestDTO request) {
        return ResponseEntity.ok(resultadoService.createResultado(request));
    }
}
