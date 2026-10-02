package com.cineamazonas.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NavegacionController {

    private final JdbcTemplate bd;

    public NavegacionController(JdbcTemplate bd) {
        this.bd = bd;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/cartelera";
    }

    @GetMapping("/mis-tickets")
    public String misTickets(@RequestParam(defaultValue = "") String compra, HttpSession sesion, Model modelo) {
        if (!"USER".equals(sesion.getAttribute("rol"))) {
            return "redirect:/login?destino=tickets";
        }
        List<Map<String, Object>> tickets = bd.queryForList(
                "SELECT t.*, p.nombre AS pelicula, s.nombre AS sala, "
                + "FORMATDATETIME(f.fecha, 'dd/MM/yyyy') AS fechaTexto, "
                + "FORMATDATETIME(f.hora, 'h:mm a', 'en') AS horaTexto "
                + "FROM tickets t JOIN funciones f ON f.id = t.funcion_id "
                + "JOIN peliculas p ON p.id = f.pelicula_id JOIN salas s ON s.id = f.sala_id "
                + "WHERE t.usuario_id = ? ORDER BY t.id", sesion.getAttribute("usuarioId"));
        for (int i = 0; i < tickets.size(); i++) {
            Map<String, Object> ticket = tickets.get(i);
            long numero = ((Number) ticket.get("id")).longValue();
            ticket.put("numero", String.format("%03d", numero));
            if (Boolean.TRUE.equals(ticket.get("activo"))) {
                ticket.put("estado", "Vigente");
            } else {
                ticket.put("estado", "Anulado");
            }
        }
        modelo.addAttribute("tickets", tickets);
        List<Map<String, Object>> compras = bd.queryForList(
                "SELECT cantidad, total FROM compras WHERE id = ? AND usuario_id = ?",
                compra, sesion.getAttribute("usuarioId"));
        if (!compras.isEmpty()) {
            modelo.addAttribute("compraConfirmada", compras.get(0));
        }
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
