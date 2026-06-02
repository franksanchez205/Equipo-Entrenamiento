package com.equipofutbol.equipofutbol_adso.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.equipofutbol.equipofutbol_adso.dto.JwtResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.LoginRequestDTO;
import com.equipofutbol.equipofutbol_adso.dto.MessageResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.RegisterRequestDTO;
import com.equipofutbol.equipofutbol_adso.entity.Users;
import com.equipofutbol.equipofutbol_adso.enums.UserRole;
import com.equipofutbol.equipofutbol_adso.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Servicio de autenticación que gestiona tres operaciones fundamentales: registro de nuevos usuarios,
 * inicio de sesión con generación de token JWT, y renovación de tokens expirados. Está anotado con
 * @Service para que Spring lo detecte durante el escaneo de componentes y lo registre como un bean
 * en el contexto de la aplicación. Inyecta tres dependencias mediante @Autowired en el constructor:
 * EmployeesRepository para acceder a los datos de los usuarios en la base de datos, PasswordEncoder
 * (configurado como BCrypt en AppConfig) para cifrar contraseñas nuevas y verificar las existentes,
 * y JwtService para generar y refrescar tokens JWT. La inyección por constructor es la práctica
 * recomendada por Spring porque hace que las dependencias sean explícitas, facilita las pruebas
 * unitarias (se pueden pasar mocks en el constructor) y permite que los campos sean finales,
 * garantizando que no puedan ser reasignados después de la construcción. Cada método del servicio
 * encapsula una operación de autenticación completa con sus propias validaciones de negocio:
 * unicidad del username en registro, verificación de credenciales y estado activo en login, y
 * confirmación de que el usuario sigue existiendo en refreshToken.
 */
@Service
public class AuthService {

    private final UserRepository employeesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Constructor con inyección de dependencias. Spring utiliza @Autowired para resolver automáticamente
     * los tres parámetros buscando beans del tipo correspondiente en su contexto de aplicación:
     * EmployeesRepository (detectado por @Repository), PasswordEncoder (definido como @Bean en
     * AppConfig) y JwtService (detectado por @Service). Al ser campos finales, se asignan una sola
     * vez durante la construcción y permanecen inmutables durante todo el ciclo de vida del bean,
     * lo que es una buena práctica de inmutabilidad y seguridad en hilos.
     *
     * @param employeesRepository Repositorio de usuarios para acceso a base de datos.
     * @param passwordEncoder     Codificador BCrypt para cifrado y verificación de contraseñas.
     * @param jwtService          Servicio JWT para generación y refresco de tokens.
     */
    @Autowired
    public AuthService(UserRepository employeesRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.employeesRepository = employeesRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registra un nuevo usuario en el sistema con los datos proporcionados en el DTO de registro.
     * El método ejecuta las siguientes validaciones y pasos en orden:
     * 1. Verifica que el nombre de usuario no exista previamente en la base de datos consultando
     *    EmployeesRepository.findByUsername(). Si ya existe, lanza una RuntimeException con un mensaje
     *    descriptivo que incluye el nombre de usuario conflictivo.
     * 2. Convierte el valor numérico del rol (recibido como Long: 0 o 1) a su correspondiente enum
     *    UserRole usando UserRole.values()[intValue]. Este enfoque aprovecha que el orden de los
     *    valores del enum coincide con los índices esperados (0 = ADMINISTRATOR, 1 = JUGADOR).
     *    Si el índice está fuera del rango (por ejemplo, 2 o -1), se captura la excepción
     *    ArrayIndexOutOfBoundsException y se lanza una RuntimeException con un mensaje claro.
     * 3. Crea una nueva instancia de Users, asigna el username, la contraseña cifrada con BCrypt
     *    mediante passwordEncoder.encode(), el rol convertido, el estado activo en true, y la
     *    fecha de creación actual con LocalDateTime.now().
     * 4. Persiste el usuario mediante employeesRepository.save().
     * 5. Retorna un MessageResponseDTO indicando que el registro fue exitoso.
     *
     * @param request DTO con username, password y rol del nuevo usuario (rol: 0=ADMIN, 1=JUGADOR).
     * @return MessageResponseDTO con el mensaje "Empleado registrado exitosamente".
     */
    public MessageResponseDTO register(RegisterRequestDTO request) {
        if (employeesRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("El nombre de usuario ya esta registrado: " + request.getUsername());
        }

        UserRole role;
        try {
            int roleIndex = request.getRol().intValue();
            role = UserRole.values()[roleIndex];
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            throw new RuntimeException("Rol invalido. Debe ser: 0 (ADMINISTRATOR), 1 (JUGADOR)");
        }

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        employeesRepository.save(user);

        return new MessageResponseDTO("Empleado registrado exitosamente");
    }

    /**
     * Autentica un usuario con sus credenciales y genera un token JWT que el cliente utilizará para
     * autenticar las peticiones subsiguientes. El flujo de validación es el siguiente:
     * 1. Busca al usuario por username en la base de datos mediante findByUsername().
     *    Si no existe, lanza RuntimeException con el mensaje "Nombre de usuario no registrado".
     * 2. Verifica la contraseña usando passwordEncoder.matches(), que compara el texto plano
     *    enviado por el cliente con el hash BCrypt almacenado en la base de datos. Si no coinciden,
     *    lanza RuntimeException con "Contrasena incorrecta".
     * 3. Comprueba que la cuenta del usuario esté activa (campo active = true). Si el administrador
     *    desactivó la cuenta, lanza una excepción indicando que debe contactar al administrador.
     * 4. Si todas las validaciones son exitosas, genera un token JWT mediante jwtService.generateToken()
     *    pasando el ID del usuario (convertido a String), el nombre del rol (obtenido del enum) y
     *    el username. Retorna un JwtResponseDTO con el token, el rol y el nombre del usuario.
     *
     * @param request DTO con username y password del usuario.
     * @return JwtResponseDTO conteniendo el token JWT, el nombre del rol y el username.
     */
    public JwtResponseDTO login(LoginRequestDTO request) {
        Optional<Users> userOpt = employeesRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Nombre de usuario no registrado: " + request.getUsername());
        }

        Users user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contrasena incorrecta");
        }

        if (!user.getActive()) {
            throw new RuntimeException("El empleado no esta activo. Contacte al administrador");
        }

        String jwt = jwtService.generateToken(
            String.valueOf(user.getId()),
            user.getRole().name(),
            user.getUsername()
        );

        return new JwtResponseDTO(jwt, user.getRole().name(), user.getUsername());
    }

    /**
     * Refresca un token JWT generando uno nuevo a partir del token existente, sin necesidad de que el
     * usuario vuelva a introducir sus credenciales. El proceso funciona de la siguiente manera:
     * 1. Delega en jwtService.refreshToken() para que extraiga los claims del token actual (userId,
     *    rolId y subject) y genere un nuevo token con los mismos claims pero con nueva fecha de
     *    emisión y expiración. El token actual puede estar expirado pero debe tener la firma válida.
     * 2. Extrae el rol y el username del token original mediante jwtService.extractRole() y
     *    jwtService.extractUsername().
     * 3. Verifica que el usuario asociado al token todavía exista en la base de datos mediante
     *    employeesRepository.findByUsername(). Si el usuario fue eliminado después de obtener el
     *    token original, lanza una RuntimeException con "Usuario no encontrado", impidiendo la
     *    renovación del token para cuentas eliminadas.
     * 4. Si todo es correcto, retorna un JwtResponseDTO con el nuevo token, el rol y el username.
     *
     * @param token Token JWT actual (puede estar expirado pero con firma válida).
     * @return JwtResponseDTO con el nuevo token JWT, rol y nombre del usuario.
     * @throws Exception Si el token es inválido (firma incorrecta, malformado) o el usuario no existe.
     */
    public JwtResponseDTO refreshToken(String token) throws Exception {
        String newToken = jwtService.refreshToken(token);
        String role = jwtService.extractRole(token);
        String username = jwtService.extractUsername(token);

        Optional<Users> userOpt = employeesRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        return new JwtResponseDTO(newToken, role, userOpt.get().getUsername());
    }
}
