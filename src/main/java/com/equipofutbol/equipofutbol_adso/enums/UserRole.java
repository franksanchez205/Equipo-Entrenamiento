package com.equipofutbol.equipofutbol_adso.enums;

/**
 * Enumeración que define los roles de usuario dentro del sistema de gestión del equipo de fútbol.
 * Se utiliza como tipo seguro en la entidad Users, mapeada con @Enumerated(EnumType.STRING) para
 * que JPA almacene el nombre del rol como varchar en la base de datos en lugar de un ordinal numérico,
 * lo que hace que el esquema sea más legible y resiliente ante cambios en el orden de los valores.
 * La convención adoptada asigna el índice 0 a ADMINISTRATOR y el 1 a JUGADOR, coincidiendo con el
 * valor numérico que el frontend o los clientes HTTP deben enviar en los DTOs de registro
 * (RegisterRequestDTO.rol). Esta correspondencia se explota en AuthService.register() mediante
 * UserRole.values()[intValue], lo que evita tener que escribir un mapper manual y mantiene la
 * lógica de conversión centralizada en un solo punto.
 */
public enum UserRole {
    ADMINISTRATOR,
    JUGADOR
}
