package com.equipofutbol.equipofutbol_adso.config;

import com.equipofutbol.equipofutbol_adso.filter.JwtValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración que registra el filtro de validación JWT en la cadena de filtros de la
 * aplicación. Spring Boot gestiona los filtros de servlet HTTP mediante FilterRegistrationBean, que
 * permite definir el orden de ejecución y los patrones de URL a los que se aplica, todo sin necesidad
 * de modificar el web.xml ni usar @WebFilter. Al declarar un @Bean de tipo FilterRegistrationBean
 * dentro de una clase @Configuration, Spring registra el filtro JwtValidationFilter en el contenedor
 * de servlets embebido de Tomcat para que intercepte todas las peticiones entrantes antes de que
 * lleguen a los controladores. El orden 1 garantiza que este filtro se ejecute antes que cualquier
 * otro filtro personalizado, permitiendo que la validación del token JWT sea la primera barrera de
 * seguridad que atraviesa cada solicitud HTTP. Este enfoque evita tener que integrar Spring Security,
 * manteniendo el control explícito sobre qué rutas requieren autenticación y simplificando la
 * configuración de seguridad del proyecto.
 */
@Configuration
public class FilterConfig {

    /**
     * Crea y configura un FilterRegistrationBean que envuelve al JwtValidationFilter. El método recibe
     * el filtro como parámetro, y Spring lo inyecta automáticamente desde su contexto de aplicación
     * porque JwtValidationFilter está anotado con @Component. Se configura el patrón de URL "/*" para
     * que el filtro se aplique a todas las rutas, y el orden 1 para que sea el primer filtro en
     * ejecutarse. Aunque el filtro se registra para todas las rutas, internamente JwtValidationFilter
     * tiene un método shouldNotFilter() que omite la validación para rutas específicas como /auth,
     * /resultados y Swagger, por lo que no es necesario restringir los patrones de URL aquí.
     *
     * @param jwtValidationFilter Instancia del filtro de validación JWT inyectada por Spring.
     * @return FilterRegistrationBean configurado para aplicar el filtro a todas las rutas con orden 1.
     */
    @Bean
    FilterRegistrationBean<JwtValidationFilter> jwtFilter(JwtValidationFilter jwtValidationFilter) {
        FilterRegistrationBean<JwtValidationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(jwtValidationFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}