package com.cineamazonas.ticketcine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CarteleraController {

    @GetMapping("/cartelera")
    public String mostrarCartelera() {
        // Devuelve la vista principal con las películas y sus horarios.
        return "cartelera";
    }
}
