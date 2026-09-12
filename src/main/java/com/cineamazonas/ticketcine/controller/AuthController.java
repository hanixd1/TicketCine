package com.cineamazonas.ticketcine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sirve la interfaz de acceso (login / registro) del cliente.
 * La lógica real de autenticación (validar credenciales, crear el
 * usuario, generar la sesión) se conecta más adelante a un Service
 * + Repository sobre la entidad Usuario.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        // Muestra la pantalla de ingreso y creación de cuenta.
        return "login";
    }
}
