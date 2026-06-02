package com.equipofutbol.equipofutbol_adso.service;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio central para la gestión de tokens JWT (JSON Web Tokens) en la aplicación, anotado con
 * @Service para que Spring lo detecte durante el escaneo de componentes y lo registre como un bean
 * en el contexto de la aplicación. Este servicio encapsula todas las operaciones criptográficas
 * relacionadas con JWT: generación de tokens con claims personalizados (userId, rolId), validación
 * de la firma HMAC-SHA, verificación de expiración, extracción individual de claims y refresco de
 * tokens expirados. La clave secreta y el tiempo de expiración se inyectan desde el archivo
 * application.yaml mediante @Value, lo que permite cambiar la configuración de seguridad sin
 * modificar el código fuente. La arquitectura del servicio sigue el patrón de método de plantilla:
 * los métodos específicos (extractUsername, extractUserId, extractRolId) delegan en el método
 * genérico extractClaims(), que a su vez utiliza el método privado getSigninKey() para obtener la
 * clave de verificación. Esto centraliza la lógica de parsing de JWT en un solo punto y reduce la
 * duplicación de código. El servicio es utilizado tanto por JwtValidationFilter (validación de
 * tokens en cada petición) como por AuthService (generación y refresco de tokens durante la
 * autenticación).
 */
@Service
public class JwtService {

    /**
     * Clave secreta codificada en Base64 que se utiliza para firmar y verificar los tokens JWT mediante
     * el algoritmo HMAC-SHA. Se inyecta desde la propiedad "security.jwt.secret-key" del archivo
     * application.yaml usando @Value, lo que permite cambiar la clave sin recompilar la aplicación.
     * La clave actual es una cadena Base64 de 256 bits (32 bytes) que cumple con el tamaño mínimo
     * recomendado por el algoritmo HMAC-SHA256. Esta clave debe mantenerse estrictamente confidencial:
     * si un atacante obtuviera la clave, podría falsificar tokens JWT y suplantar cualquier usuario
     * del sistema. Por esta razón, en producción debería almacenarse en variables de entorno o en un
     * servicio de gestión de secretos como Vault, no en el repositorio de código.
     */
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    /**
     * Tiempo de expiración del token JWT en milisegundos, inyectado desde la propiedad
     * "security.jwt.token-expiration" del application.yaml. Actualmente configurado en 600000 ms
     * (10 minutos). Cuando el token expira, el servidor lo rechaza automáticamente durante la
     * validación en JwtValidationFilter, y el cliente debe renovarlo usando el endpoint
     * GET /auth/refreshToken. Un tiempo de expiración corto reduce la ventana de exposición en caso
     * de que un token sea interceptado, pero obliga al cliente a refrescarlo con más frecuencia,
     * por lo que debe ajustarse según los requisitos de seguridad y usabilidad de la aplicación.
     */
    @Value("${security.jwt.token-expiration}")
    private Long tokenExpiration;

    /**
     * Genera una clave secreta HMAC-SHA a partir de la cadena Base64 almacenada en secretKey.
     * Internamente, Decoders.BASE64.decode() convierte la cadena Base64 en un arreglo de bytes, y
     * Keys.hmacShaKeyFor() crea una clave HMAC-SHA adecuada a partir de esos bytes. Si la clave
     * Base64 tiene menos de 256 bits, Keys.hmacShaKeyFor() lanzará una excepción, por lo que es
     * importante que la clave tenga el tamaño suficiente. Este método es privado y se utiliza
     * únicamente dentro del servicio para firmar tokens (generateToken) y verificarlos (isTokenValid,
     * extractClaims, refreshToken).
     *
     * @return SecretKey lista para usar en operaciones de firma y verificación JWT.
     */
    private SecretKey getSigninKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un nuevo token JWT firmado con claims personalizados y el subject. Construye el token
     * utilizando la API fluida de JJWT (Jwts.builder()): establece claims personalizados "userId" y
     * "rolId" mediante un Map para transportar información del usuario sin consultar la base de datos;
     * asigna el subject (username) como identificador estándar del propietario del token según RFC 7519;
     * fija la fecha de emisión (issuedAt) al momento actual; calcula la expiración como la hora actual
     * más tokenExpiration milisegundos; y firma el token con la clave HMAC-SHA obtenida de getSigninKey().
     * El resultado es un String compacto con formato JWT (header.payload.signature) listo para ser
     * enviado al cliente en la respuesta de login o refreshToken.
     *
     * @param userId   Identificador único del usuario en la base de datos, convertido a String.
     * @param rolId    Nombre del rol del usuario ("ADMINISTRATOR" o "JUGADOR").
     * @param userName Nombre de usuario que será el subject (sub) del token.
     * @return Token JWT firmado como String compacto.
     */
    public String generateToken(String userId, String rolId, String userName) {
        return Jwts.builder()
                .claims(Map.of("userId", userId, "rolId", rolId))
                .subject(userName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigninKey())
                .compact();
    }

    /**
     * Valida si un token JWT es válido verificando su firma y su fecha de expiración. Intenta parsear
     * el token con la clave secreta: si Jwts.parser().verifyWith().build().parseSignedClaims() se
     * ejecuta sin lanzar excepciones, significa que la firma es correcta y el token no ha expirado,
     * por lo que retorna true. Si ocurre una JwtException (que cubre token malformado, firma inválida,
     * token expirado), captura la excepción, imprime el error en la consola estándar para fines de
     * depuración y retorna false. También captura cualquier otra excepción inesperada como medida de
     * seguridad para evitar que el filtro JWT falle con un error 500. Este método es utilizado por
     * JwtValidationFilter.doFilterInternal() para decidir si la petición debe continuar hacia el
     * controlador o ser rechazada con HTTP 401.
     *
     * @param token Token JWT a validar.
     * @return true si el token tiene firma válida y no ha expirado, false en caso contrario.
     */
    public Boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(getSigninKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            System.err.println("Token is invalid: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ocurrio un error inesperado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Método genérico que extrae un valor específico del payload (claims) de un token JWT aplicando
     * una función resolver. Parsea el token, obtiene el objeto Claims del payload, y luego aplica la
     * función resolver para transformar los Claims en el tipo de retorno deseado. Este diseño basado
     * en Function<Claims, T> permite que un solo método sirva para extraer cualquier claim sin
     * necesidad de duplicar el código de parsing. Los métodos específicos (extractUsername,
     * extractUserId, extractRolId) llaman a este método cada uno con su propia función lambda,
     * siguiendo el patrón Template Method y manteniendo el código DRY (Don't Repeat Yourself).
     *
     * @param <T>      Tipo de dato del valor a extraer.
     * @param token    Token JWT del cual extraer los claims.
     * @param resolver Función que recibe los Claims y retorna el valor deseado del tipo T.
     * @return Valor extraído de los claims según el resolver proporcionado.
     */
    public <T> T extractClaims(String token, Function<Claims, T> resolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolver.apply(claims);
    }

    /**
     * Extrae el nombre de usuario (subject) del token JWT. El subject es un claim reservado y
     * estandarizado en RFC 7519 que identifica al principal del token. En esta aplicación, el subject
     * contiene el username del usuario autenticado, que se estableció durante la generación del token
     * mediante el método generateToken(). Internamente delega en extractClaims() con la función
     * Claims::getSubject, que es una referencia al método getSubject() de la clase Claims.
     *
     * @param token Token JWT del cual extraer el username.
     * @return Nombre de usuario almacenado en el subject del token.
     */
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    /**
     * Extrae el ID del usuario (su clave primaria en la base de datos) desde el claim personalizado
     * "userId" del token. Este claim se estableció en generateToken() mediante el Map de claims y
     * permite a JwtValidationFilter identificar al usuario autenticado sin necesidad de consultar la
     * base de datos en cada petición, mejorando el rendimiento y reduciendo la carga sobre MySQL.
     *
     * @param token Token JWT del cual extraer el userId.
     * @return ID del usuario como String.
     */
    public String extractUserId(String token) {
        return extractClaims(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extrae el nombre del rol del usuario desde el claim personalizado "rolId" del token. Este claim
     * contiene el nombre del rol según el enum UserRole ("ADMINISTRATOR" o "JUGADOR") y es utilizado
     * por JwtValidationFilter para establecer el atributo "rolId" y "role" en la petición, que
     * posteriormente SecurityContext.getCurrentRole() consulta para validar permisos en servicios
     * como UserService.createUser().
     *
     * @param token Token JWT del cual extraer el rolId.
     * @return Nombre del rol como String ("ADMINISTRATOR" o "JUGADOR").
     */
    public String extractRolId(String token) {
        return extractClaims(token, claims -> claims.get("rolId", String.class));
    }

    /**
     * Alias de extractRolId() que proporciona un nombre de método semánticamente más claro para su
     * uso en AuthService.refreshToken(). Internamente realiza exactamente la misma operación que
     * extractRolId(): extrae el claim "rolId" del token. Se mantiene como método separado para
     * mejorar la legibilidad del código en el contexto de autenticación, donde "rol" es un término
     * más natural que "rolId" para referirse al nombre del rol.
     *
     * @param token Token JWT del cual extraer el role.
     * @return Nombre del rol como String ("ADMINISTRATOR" o "JUGADOR").
     */
    public String extractRole(String token) {
        return extractClaims(token, claims -> claims.get("rolId", String.class));
    }

    /**
     * Método que actualmente funciona como alias de extractUsername(), pero que está diseñado para
     * futuras extensiones donde se agregue un claim de email al token. En la implementación actual,
     * retorna el subject del token (que es el username), pero si en el futuro se decide incluir un
     * claim "email" en generateToken(), este método se modificaría para extraer ese claim específico
     * sin afectar a los llamantes que ya utilizan extractUsername().
     *
     * @param token Token JWT del cual extraer el email.
     * @return Actualmente el username (subject del token).
     */
    public String extractEmail(String token) {
        return extractUsername(token);
    }

    /**
     * Refresca un token JWT generando uno nuevo a partir de los claims del token existente. Parsea el
     * token actual para extraer userId, rolId y subject del payload. Si el token está expirado, captura
     * la excepción ExpiredJwtException, ya que JJWT permite acceder a los claims incluso después de
     * la expiración para casos como este donde se necesita renovar el token. Si el token es inválido
     * por otras razones (firma incorrecta, token malformado), lanza una excepción JwtException que se
     * traduce a un error genérico. En caso de éxito, genera un nuevo token con los mismos claims pero
     * con nueva fecha de emisión (issuedAt) y nueva fecha de expiración, efectivamente extendiendo la
     * sesión del usuario sin que tenga que volver a introducir sus credenciales.
     *
     * @param token Token JWT a refrescar (puede estar expirado pero no debe tener firma inválida).
     * @return Nuevo token JWT con fecha de expiración renovada.
     * @throws Exception Si el token tiene firma inválida, está malformado u ocurre un error inesperado.
     */
    public String refreshToken(String token) throws Exception {
        Claims claims;

        try {
            claims = Jwts.parser()
                    .verifyWith(getSigninKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new Exception("Token is expired " + e.getMessage());
        } catch (JwtException e) {
            throw new Exception("Token is invalid " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Server error " + e.getMessage());
        }

        return generateToken(claims.get("userId", String.class), claims.get("rolId", String.class), claims.getSubject());
    }
}
