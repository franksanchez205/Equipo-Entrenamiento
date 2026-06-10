package com.equipofutbol.equipofutbol_adso.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.equipofutbol.equipofutbol_adso.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de validación de tokens JWT que se ejecuta una vez por cada petición
 * entrante gracias a que extiende OncePerRequestFilter.
 * Al marcar la clase con @Component, Spring la detecta durante el escaneo de
 * componentes y la registra como un bean en el contexto. Luego, FilterConfig
 * la envuelve en un FilterRegistrationBean para definir su orden y las URL a
 * las que se aplica. Este filtro intercepta cada solicitud HTTP antes de que
 * llegue al controlador, extrae el token JWT del encabezado Authorization,
 * lo valida contra la clave secreta y, si es válido, extrae el username, userId
 * y rolId y los establece como atributos de la petición para que estén
 * disponibles en los controladores y en SecurityContext. Si no hay token o es
 * inválido, responde con 401 sin permitir que la solicitud continúe. Las rutas
 * de autenticación, Swagger, resultados y usuarios se omiten mediante
 * shouldNotFilter() para que sean accesibles sin token.
 */
@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    /**
     * Servicio de JWT inyectado automáticamente por Spring mediante @Autowired.
     * Se utiliza para validar la firma del token, verificar su expiración y
     * extraer los claims (username, userId, rolId) del payload. Al declarar el
     * campo con @Autowired, Spring busca un bean de tipo JwtService en su
     * contexto y lo asigna aquí, permitiendo que el filtro delegue toda la
     * lógica criptográfica al servicio especializado sin acoplarse a los
     * detalles de implementación de JJWT.
     */
    @Autowired
    private JwtService jwtService;

    /**
     * Método principal del filtro, ejecutado en cada petición HTTP que no sea
     * excluida por shouldNotFilter(). Obtiene el encabezado "Authorization" de
     * la petición y verifica que tenga el formato "Bearer <token>". Si el
     * encabezado falta o no comienza con "Bearer ", responde con 401 y un JSON
     * indicando que falta el encabezado. Si el encabezado es válido, extrae el
     * token, lo valida con JwtService.isTokenValid() y, si es válido, extrae
     * username, userId y rolId y los almacena como atributos de la petición
     * para que los controladores y SecurityContext puedan acceder a ellos.
     * Luego continúa la cadena de filtros con filterChain.doFilter(). Si el
     * token es inválido o expirado, responde con 401. Si ocurre una excepción
     * inesperada durante el proceso, también responde con 401.
     * 
     * @param request     Objeto HttpServletRequest de la petición entrante.
     * @param response    Objeto HttpServletResponse para escribir la respuesta.
     * @param filterChain Cadena de filtros para continuar el procesamiento.
     * @throws IOException Si ocurre un error al escribir en la respuesta.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        String autHeader = request.getHeader("Authorization");

        if (autHeader != null && autHeader.startsWith("Bearer ")) {
            String token = autHeader.replace("Bearer ", "");
            try {
                if (jwtService.isTokenValid(token)) {
                    String username = jwtService.extractUsername(token);
                    String userId = jwtService.extractUserId(token);
                    String rolId = jwtService.extractRolId(token);

                    request.setAttribute("username", username);
                    request.setAttribute("userId", userId);
                    request.setAttribute("rolId", rolId);
                    request.setAttribute("role", rolId);
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determina si una solicitud debe omitir la validación del token JWT.
     * Retorna true para las rutas que no requieren autenticación: /auth
     * (registro y login), /swagger y /v3/api-docs (documentación de la API),
     * /resultados (registro de entrenamientos, que es público) y /user
     * (creación y consulta de jugadores). Esto permite que los usuarios
     * puedan registrarse, iniciar sesión y que los endpoints de consulta
     * sean accesibles sin necesidad de un token JWT.
     * 
     * @param request La solicitud HTTP a evaluar.
     * @return true si la solicitud debe omitir la validación, false en caso
     *         contrario.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || path.equals("/api/v1")
                || path.equals("/api/v1/")
                || path.equals("/api/v1/index.html")
                || path.equals("/api/v1/styles.css")
                || path.equals("/api/v1/app.js")
                || path.equals("/api/v1/favicon.ico")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/swagger-ui")
                || path.startsWith("/api/v1/v3/api-docs")
                || path.startsWith("/api/v1/swagger-config");
    }
}
