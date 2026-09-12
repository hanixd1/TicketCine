package com.cineamazonas.ticketcine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacionController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/cartelera";
    }

    @GetMapping("/mis-tickets")
    public String misTickets() {
        return "mis-tickets";
    }

    @GetMapping("/publicidad")
    public String publicidad() {
        return "publicidad";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }
}
