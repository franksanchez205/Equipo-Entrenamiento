package com.equipofutbol.equipofutbol_adso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación Spring Boot para la gestión del equipo de fútbol. Está anotada con
 * @SpringBootApplication, que es una anotación compuesta que combina @Configuration (permite registrar
 * beans adicionales mediante @Bean), @EnableAutoConfiguration (configura automáticamente Spring Boot
 * basándose en las dependencias del classpath, como la configuración de JPA si encuentra Hibernate y
 * MySQL en el classpath) y @ComponentScan (escanea el paquete base y todos sus subpaquetes en busca de
 * componentes anotados con @Component, @Service, @Repository, @Controller, etc.). El método main()
 * invoca SpringApplication.run(), que arranca el contexto de Spring, inicia el contenedor de servlets
 * embebido (Tomcat en el puerto 9090 según application.yaml), ejecuta los scripts de inicialización
 * de la base de datos y deja la aplicación lista para recibir peticiones HTTP en el context-path /api/v1/.
 *
 * Desde aquí se inicia toda la cadena de inicialización: se cargan las configuraciones de application.yaml,
 * se registran los beans definidos en AppConfig (PasswordEncoder), se registra el filtro JWT definido
 * en FilterConfig, se detectan las entidades JPA (Users, Resultados) y se crean las tablas en MySQL
 * (con ddl-auto: create-drop, que las crea al iniciar y las elimina al detener), y se exponen los
 * controladores REST para recibir peticiones HTTP en los endpoints definidos.
 */
@SpringBootApplication
public class EquipofutbolAdsoApplication {

    /**
     * Método de entrada de la aplicación Java. La JVM invoca este método al ejecutar el JAR de la
     * aplicación. SpringApplication.run() configura el contexto de Spring de acuerdo con la clase
     * anotada con @SpringBootApplication, arranca el servidor embebido y mantiene la aplicación en
     * ejecución hasta que reciba una señal de terminación.
     *
     * @param args Argumentos de línea de comandos pasados al iniciar la aplicación.
     */
    public static void main(String[] args) {
        SpringApplication.run(EquipofutbolAdsoApplication.class, args);
    }
}
