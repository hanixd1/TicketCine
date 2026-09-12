package com.cineamazonas.ticketcine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacionController {

    @GetMapping("/")
    public String inicio() {
        // Envía la ruta inicial a la cartelera, que es la vista principal.
        return "redirect:/cartelera";
    }

    @GetMapping("/mis-tickets")
    public String misTickets() {
        // Muestra la página estática con los tickets del usuario.
        return "mis-tickets";
    }

    @GetMapping("/publicidad")
    public String publicidad() {
        // Muestra las promociones actualmente publicadas.
        return "publicidad";
    }

    @GetMapping("/contacto")
    public String contacto() {
        // Muestra el formulario de contacto, sin procesarlo todavía.
        return "contacto";
    }
}
