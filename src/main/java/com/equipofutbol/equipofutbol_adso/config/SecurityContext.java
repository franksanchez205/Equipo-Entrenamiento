package com.equipofutbol.equipofutbol_adso.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Componente de utilidad que permite acceder al rol del usuario autenticado desde cualquier capa de la
 * aplicación sin necesidad de recibir explícitamente el objeto HttpServletRequest como parámetro. Está
 * anotado con @Component, lo que hace que Spring lo detecte durante el escaneo de componentes y lo
 * registre como un bean singleton en el contexto de la aplicación. Su funcionamiento se basa en
 * RequestContextHolder, una clase de Spring que mantiene los atributos de la petición HTTP actual
 * asociados al hilo de ejecución mediante un ThreadLocal. Esto significa que cualquier servicio
 * (como UserService.createUser()) puede inyectar SecurityContext con @Autowired y llamar a
 * getCurrentRole() para obtener el rol del usuario autenticado sin tener que recibir la petición HTTP
 * como parámetro del método, desacoplando así la lógica de negocio de los detalles del protocolo HTTP.
 * Internamente, el método se apoya en que JwtValidationFilter estableció previamente el atributo "role"
 * en la petición durante la validación del token, creando un puente entre el filtro y los servicios.
 */
@Component
public class SecurityContext {

    /**
     * Recupera el rol del usuario autenticado desde los atributos de la petición HTTP actual. Utiliza
     * RequestContextHolder.currentRequestAttributes() para obtener el objeto RequestAttributes del
     * hilo en ejecución, y luego extrae el atributo "role" que JwtValidationFilter almacenó durante
     * la validación del token JWT. Si el atributo existe, lo convierte a String y lo retorna; si no
     * existe (por ejemplo, porque la ruta fue omitida por shouldNotFilter() o porque el atributo no
     * se estableció), retorna null en lugar de lanzar una excepción, delegando en el llamante la
     * decisión de cómo manejar la ausencia de autenticación. Este comportamiento null-safe es
     * importante porque UserService.validateAdminRole() compara el resultado con "ADMINISTRATOR"
     * usando equals(), que maneja correctamente el caso null retornando false y lanzando una
     * SecurityAuthorizationException.
     *
     * @return El nombre del rol ("ADMINISTRATOR" o "JUGADOR") si existe un token válido, null si no
     *         hay rol disponible en la petición actual.
     */
    public String getCurrentRole() {
        Object role = RequestContextHolder.currentRequestAttributes()
                .getAttribute("role", RequestAttributes.SCOPE_REQUEST);

        if (role != null) {
            return role.toString();
        } else {
            return null;
        }
    }
}