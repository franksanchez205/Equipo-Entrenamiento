package com.equipofutbol.equipofutbol_adso.dto;

import lombok.Data;

/**
 * DTO que encapsula la respuesta exitosa de los endpoints de autenticación: login y refreshToken.
 * Devuelve al cliente el token JWT que deberá utilizar para autenticar las peticiones posteriores,
 * junto con el rol del usuario (para que el frontend pueda mostrar u ocultar funcionalidades según
 * el perfil) y el nombre de usuario. Al incluir role y name en la misma respuesta, el cliente
 * evita tener que hacer una llamada adicional para obtener estos datos después del login. La anotación
 * @Data de Lombok genera los getters, y el constructor con todos los argumentos permite crear
 * instancias de forma inmutable y expresiva en los servicios que construyen la respuesta.
 */
@Data
public class JwtResponseDTO {

    /**
     * Token JWT firmado que el cliente debe incluir en el encabezado Authorization de las peticiones
     * subsiguientes bajo el formato "Bearer <token>". Contiene los claims userId, rolId y el subject
     * con el username, y tiene una validez configurada en application.yaml (10 minutos por defecto).
     */
    private String jwt;

    /**
     * Nombre del rol del usuario autenticado ("ADMINISTRATOR" o "JUGADOR"). Se extrae directamente del
     * enum UserRole y se incluye en la respuesta para que el frontend pueda adaptar la interfaz según
     * los permisos del usuario sin tener que parsear el token JWT del lado del cliente.
     */
    private String role;

    /**
     * Nombre de usuario autenticado. Se obtiene del campo username de la entidad Users y se incluye
     * como comodidad para que el frontend pueda mostrar el nombre del usuario logueado sin tener que
     * decodificar el token JWT ni hacer una consulta adicional a la API.
     */
    private String name;

    /**
     * Constructor con todos los argumentos que permite crear una instancia completa de la respuesta
     * en una sola línea. Se utiliza en AuthService.login() y AuthService.refreshToken() para construir
     * la respuesta inmediatamente después de generar o refrescar el token JWT.
     *
     * @param jwt  Token JWT firmado.
     * @param role Nombre del rol del usuario.
     * @param name Nombre de usuario.
     */
    public JwtResponseDTO(String jwt, String role, String name) {
        this.jwt = jwt;
        this.role = role;
        this.name = name;
    }
}
