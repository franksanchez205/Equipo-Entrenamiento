package com.equipofutbol.equipofutbol_adso.service;

import com.equipofutbol.equipofutbol_adso.dto.MessageResponseDTO;
import com.equipofutbol.equipofutbol_adso.dto.ResultadoRequestDTO;
import com.equipofutbol.equipofutbol_adso.entity.Resultados;
import com.equipofutbol.equipofutbol_adso.entity.Users;
import com.equipofutbol.equipofutbol_adso.repository.UserRepository;
import com.equipofutbol.equipofutbol_adso.repository.ResultadoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona el registro de resultados de entrenamiento para cada jugador del equipo.
 * Está anotado con @Service para que Spring lo detecte durante el escaneo de componentes y lo registre
 * como un bean en el contexto de la aplicación. Inyecta dos repositorios mediante @Autowired:
 * ResultadoRepository para persistir los resultados de entrenamiento y consultar el conteo de
 * entrenamientos existentes, y EmployeesRepository para buscar al jugador por su número de camiseta
 * y actualizar su puntaje total después de cada registro.
 *
 * La lógica central del servicio se encuentra en createResultado(), que recibe los datos del
 * entrenamiento, valida que el jugador no haya excedido el límite de 3 entrenamientos, persiste
 * el resultado y actualiza el puntaje promedio del jugador. La actualización del puntaje se realiza
 * mediante el método privado actualizarPuntajeTotal(), que recalcula el promedio de todos los
 * entrenamientos del jugador usando la fórmula ponderada de pases, velocidad y potencia.
 */
@Service
public class ResultadoService {

    /**
     * Repositorio de resultados inyectado por Spring. Se utiliza para persistir cada nuevo resultado
     * de entrenamiento (save) y para contar cuántos entrenamientos tiene registrados un jugador
     * (countNumeroEntrenamiento), validando así el límite máximo de 3 entrenamientos por jugador.
     */
    @Autowired
    private ResultadoRepository resultadoRepository;

    /**
     * Repositorio de usuarios inyectado por Spring. Se utiliza para buscar al jugador por su número
     * de camiseta (findByNumeroCamiseta) antes de registrar el resultado, y para persistir la
     * actualización del puntajeTotal del jugador después de cada nuevo entrenamiento (save).
     */
    @Autowired
    private UserRepository employeesRepository;

    /**
     * Crea un nuevo resultado de entrenamiento para un jugador específico. El flujo completo es:
     * 1. Busca al jugador por su número de camiseta usando employeesRepository.findByNumeroCamiseta(),
     *    convirtiendo el campo "users" del DTO (String) a Integer con Integer.parseInt(). Si no
     *    existe un jugador con esa camiseta, lanza RuntimeException.
     * 2. Cuenta los entrenamientos ya registrados para ese jugador mediante
     *    resultadoRepository.countNumeroEntrenamiento(). Si el jugador ya tiene 3 entrenamientos,
     *    lanza RuntimeException con el mensaje "El jugador ya tiene 3 entrenamientos registrados".
     * 3. Crea una nueva entidad Resultados con los datos del DTO y la asocia al jugador.
     * 4. Persiste el resultado mediante resultadoRepository.save().
     * 5. Actualiza el puntaje total del jugador llamando a actualizarPuntajeTotal(), que recalcula
     *    el promedio de todos sus entrenamientos.
     * 6. Retorna un MessageResponseDTO confirmando el registro exitoso.
     *
     * @param request DTO con número de entrenamiento, métricas (pases, potencia, velocidad) y
     *                número de camiseta del jugador.
     * @return MessageResponseDTO con el mensaje "Resultado registrado exitosamente".
     */
    public MessageResponseDTO createResultado(ResultadoRequestDTO request) {
        Users user = employeesRepository.findByNumeroCamiseta(Integer.parseInt(request.getUsers()))
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado con camiseta: " + request.getUsers()));

        int count = resultadoRepository.countNumeroEntrenamiento(user.getId());
        if (count >= 3) {
            throw new RuntimeException("El jugador ya tiene 3 entrenamientos registrados");
        }

        Resultados resultado = new Resultados();
        resultado.setNumeroEntrenamiento(request.getNumeroEntrenamiento());
        resultado.setPasesEfectivos(request.getPasesEfectivos());
        resultado.setPotenciaTiro(request.getPotenciaTiro());
        resultado.setVelocidadJugador(request.getVelocidadJugador());
        resultado.setUsers(user);

        resultadoRepository.save(resultado);

        actualizarPuntajeTotal(user);

        return new MessageResponseDTO("Resultado registrado exitosamente");
    }

    /**
     * Actualiza el puntaje total promedio del jugador recalculándolo a partir de todos sus resultados
     * de entrenamiento registrados. El cálculo sigue estos pasos:
     * 1. Obtiene la lista de resultados del jugador desde user.getListResultados().
     * 2. Si la lista es null o está vacía, establece el puntaje total en 0.0.
     * 3. Si hay resultados, itera sobre cada uno calculando el puntaje individual con la fórmula:
     *    (pasesEfectivos * 0.5) + (velocidadJugador * 0.3) + (potenciaTiro * 0.2), suma todos los
     *    puntajes y divide entre la cantidad de resultados para obtener el promedio.
     * 4. Actualiza el campo puntajeTotal del usuario y persiste el cambio con employeesRepository.save().
     * Este método se ejecuta después de cada nuevo registro de entrenamiento para mantener el puntaje
     * del jugador siempre actualizado, de modo que el algoritmo de selección del top 5 refleje los
     * datos más recientes.
     *
     * @param user Entidad Users del jugador cuyo puntaje se va a recalcular.
     */
    private void actualizarPuntajeTotal(Users user) {
        List<Resultados> resultados = user.getListResultados();
        if (resultados == null || resultados.isEmpty()) {
            user.setPuntajeTotal(0.0);
        } else {
            double sum = 0;
            for (Resultados r : resultados) {
                double puntaje = (r.getPasesEfectivos() * 0.5)
                        + (r.getVelocidadJugador() * 0.3)
                        + (r.getPotenciaTiro() * 0.2);
                sum += puntaje;
            }
            user.setPuntajeTotal(sum / resultados.size());
        }
        employeesRepository.save(user);
    }
}
