package com.cineamazonas.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final JdbcTemplate bd;

    public AdminController(JdbcTemplate bd) {
        this.bd = bd;
    }

    @GetMapping("/admin")
    public String panel(HttpSession sesion, HttpServletResponse respuesta, Model modelo) {
        if (sesion.getAttribute("usuarioId") == null || !"ADMIN".equals(sesion.getAttribute("rol"))) {
            return "redirect:/admin/login";
        }
        modelo.addAttribute("peliculas", bd.queryForList(
                "SELECT p.*, g.nombre AS genero FROM peliculas p JOIN generos g ON g.id = p.genero_id ORDER BY p.id"));
        modelo.addAttribute("generos", bd.queryForList(
                "SELECT g.*, (SELECT COUNT(*) FROM peliculas p WHERE p.genero_id = g.id) AS cantidad "
                + "FROM generos g ORDER BY g.nombre"));
        modelo.addAttribute("salas", bd.queryForList("SELECT * FROM salas ORDER BY id"));
        modelo.addAttribute("funciones", bd.queryForList(
                "SELECT f.*, p.nombre AS pelicula, s.nombre AS sala, s.aforo, "
                + "FORMATDATETIME(f.hora, 'h:mm a', 'en') AS horaTexto, "
                + "(SELECT COUNT(*) FROM tickets t WHERE t.funcion_id = f.id AND t.activo = TRUE) AS ocupacion "
                + "FROM funciones f JOIN peliculas p ON p.id = f.pelicula_id JOIN salas s ON s.id = f.sala_id "
                + "WHERE f.fecha = CURRENT_DATE ORDER BY f.hora"));
        modelo.addAttribute("usuarios", bd.queryForList("SELECT nombre, correo, rol, activo FROM usuarios ORDER BY id"));
        return "admin";
    }
}
