package com.equipofutbol.equipofutbol_adso.controller;

import com.equipofutbol.equipofutbol_adso.dto.JwtResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.LoginRequestDTO;
import com.equipofutbol.equipofutbol_adso.dto.MessageResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.RegisterRequestDTO;
import com.equipofutbol.equipofutbol_adso.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que expone los endpoints de autenticación bajo la ruta base "/auth". Está anotado
 * con @RestController para indicar que cada método retorna directamente objetos Java que Spring
 * serializa a JSON (en lugar de vistas), y con @RequestMapping("/auth") para establecer el prefijo
 * común de todas las rutas del controlador. Inyecta AuthService mediante @Autowired para delegar
 * toda la lógica de negocio de autenticación en la capa de servicio.
 *
 * Expone tres endpoints: POST /register para el registro de nuevos usuarios (accesible sin
 * autenticación), POST /login para el inicio de sesión que retorna un token JWT, y GET /refreshToken
 * para renovar tokens expirados (requiere token JWT en el encabezado). Los dos primeros utilizan
 * @Valid en sus parámetros para que Spring active las validaciones de Jakarta Validation definidas
 * en los DTOs (como @NotBlank en username y password), devolviendo automáticamente errores 400 si
 * los datos no cumplen las restricciones, sin necesidad de validaciones manuales en el controlador.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * Servicio de autenticación inyectado por Spring que contiene la lógica de negocio para registro,
     * login y refresco de tokens. Se inyecta mediante @Autowired sobre el campo, lo que permite que
     * Spring resuelva automáticamente la dependencia buscando un bean de tipo AuthService en su contexto.
     */
    @Autowired
    private AuthService authService;

    /**
     * Endpoint POST para registrar un nuevo usuario en el sistema. Recibe un RegisterRequestDTO con
     * username, password y rol (0=ADMINISTRATOR, 1=JUGADOR). El parámetro está anotado con @Valid para
     * que Spring active las validaciones de Jakarta Validation (@NotBlank en username y password,
     * @NotNull en rol). Si las validaciones fallan, Spring devuelve automáticamente un error 400 sin
     * llegar a ejecutar el método del controlador. En caso exitoso, delega en AuthService.register()
     * y retorna un MessageResponseDTO con estado HTTP 200 OK.
     *
     * @param request DTO con username, password y rol del nuevo usuario.
     * @return ResponseEntity con MessageResponseDTO indicando el resultado del registro.
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Endpoint POST para iniciar sesión con las credenciales del usuario. Recibe un LoginRequestDTO
     * con username y password. Al igual que el registro, utiliza @Valid para validar que los campos
     * no estén vacíos. Delega en AuthService.login(), que verifica las credenciales y, si son correctas,
     * genera y retorna un token JWT dentro de un JwtResponseDTO. Si las credenciales son inválidas o
     * la cuenta está desactivada, AuthService lanza RuntimeException que es manejada por
     * GlobalExceptionHandler.
     *
     * @param request DTO con username y password del usuario.
     * @return ResponseEntity con JwtResponseDTO conteniendo el token JWT, rol y nombre de usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Endpoint GET para renovar un token JWT expirado sin necesidad de que el usuario vuelva a
     * introducir sus credenciales. Extrae el token del encabezado "Authorization" de la petición HTTP,
     * verificando que tenga el formato "Bearer <token>". Si el encabezado falta o no tiene el formato
     * correcto, retorna HTTP 400 con el mensaje "Token no proporcionado". Si el token está presente,
     * delega en AuthService.refreshToken(), que a su vez utiliza JwtService.refreshToken() para generar
     * un nuevo token con los mismos claims pero nueva fecha de expiración. Las excepciones se manejan
     * en dos categorías: RuntimeException (token expirado o usuario no encontrado) retorna HTTP 401,
     * y cualquier otra excepción retorna HTTP 400. En caso de éxito, retorna un JwtResponseDTO con el
     * nuevo token, el rol y el nombre del usuario.
     *
     * @param request Objeto HttpServletRequest para acceder al encabezado Authorization de la petición.
     * @return ResponseEntity con JwtResponseDTO (éxito) o MessageResponseDTO (error).
     */
    @GetMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String autheader = request.getHeader("Authorization");
        if (autheader == null || !autheader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponseDTO("Token no proporcionado"));
        }

        String token = autheader.replace("Bearer ", "");

        try {
            JwtResponseDTO response = authService.refreshToken(token);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponseDTO("Token expired"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponseDTO("Error al refrescar el token"));
        }
    }
}

