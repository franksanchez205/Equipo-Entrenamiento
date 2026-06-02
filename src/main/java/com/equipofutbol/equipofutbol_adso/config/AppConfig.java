package com.equipofutbol.equipofutbol_adso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Clase de configuración global de la aplicación marcada con @Configuration, lo que le indica a Spring
 * que esta clase contiene definiciones de beans que deben ser gestionados por el contenedor de inversión
 * de control (IoC). A diferencia de las clases anotadas con @Component que Spring detecta automáticamente
 * mediante el escaneo de componentes, los métodos anotados con @Bean dentro de una clase @Configuration
 * permiten definir beans de forma explícita cuando se necesita control sobre el proceso de instanciación
 * o cuando la clase del bean no es una clase propia del proyecto (como BCryptPasswordEncoder, que
 * pertenece a Spring Security). Centralizar estos beans en AppConfig evita tener que dispersar la
 * configuración por toda la aplicación y facilita la localización de todos los componentes configurables.
 */
@Configuration
public class AppConfig {

    /**
     * Define un bean de tipo PasswordEncoder implementado con BCryptPasswordEncoder, que es el
     * algoritmo de hash de contraseñas recomendado por Spring Security por su resistencia a ataques
     * de fuerza bruta y rainbow tables. BCrypt incorpora automáticamente un salt aleatorio en cada
     * hash, lo que significa que dos usuarios con la misma contraseña tendrán hashes completamente
     * diferentes. Además, el algoritmo es deliberadamente lento (configurable mediante el parámetro
     * strength, que por defecto es 10 rondas), lo que dificulta los ataques de diccionario incluso
     * si un atacante obtiene la base de datos de hashes. Este bean se inyecta automáticamente mediante
     * @Autowired en AuthService, donde se utiliza tanto para cifrar contraseñas en el registro como
     * para verificarlas en el inicio de sesión mediante el método matches().
     *
     * @return Instancia singleton de BCryptPasswordEncoder gestionada por el contenedor Spring.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
