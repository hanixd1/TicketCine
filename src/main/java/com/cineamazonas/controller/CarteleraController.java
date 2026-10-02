package com.cineamazonas.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CarteleraController {

    private final JdbcTemplate bd;
    private final DataSource conexiones;

    public CarteleraController(JdbcTemplate bd, DataSource conexiones) {
        this.bd = bd;
        this.conexiones = conexiones;
    }

    @GetMapping("/cartelera")
    public String mostrarCartelera(Model modelo) {
        List<Map<String, Object>> peliculas = bd.queryForList(
                "SELECT p.*, g.nombre AS genero FROM peliculas p "
                + "JOIN generos g ON g.id = p.genero_id WHERE p.activo = TRUE AND g.activo = TRUE ORDER BY p.id");
        List<Map<String, Object>> funciones = bd.queryForList(
                "SELECT f.*, s.nombre AS sala, FORMATDATETIME(f.hora, 'h:mm a', 'en') AS horaTexto "
                + "FROM funciones f JOIN salas s ON s.id = f.sala_id "
                + "WHERE f.activo = TRUE AND s.activo = TRUE AND f.fecha = CURRENT_DATE ORDER BY f.hora");
        modelo.addAttribute("peliculas", peliculas);
        modelo.addAttribute("funciones", funciones);
        return "cartelera";
    }

    @GetMapping("/sala")
    public String sala(@RequestParam int funcion, HttpSession sesion, HttpServletResponse respuesta, Model modelo) {
        sesion.setAttribute("funcionPendiente", funcion);
        if (sesion.getAttribute("csrf") == null) {
            sesion.setAttribute("csrf", UUID.randomUUID().toString());
        }
        if (sesion.getAttribute("invitadoId") == null) {
            sesion.setAttribute("invitadoId", UUID.randomUUID().toString());
        }
        String compra = UUID.randomUUID().toString();
        sesion.setAttribute("compraToken", compra);
        sesion.setAttribute("funcionCompra", funcion);
        sesion.removeAttribute("asientosPendientes");
        sesion.removeAttribute("compraInvitado");
        return cargarSala(funcion, compra, new ArrayList<>(), sesion, respuesta, modelo);
    }

    @PostMapping("/seleccionar")
    @SuppressWarnings("unchecked")
    public String seleccionar(@RequestParam int funcion,
                               @RequestParam(defaultValue = "") String compra,
                               @RequestParam(defaultValue = "") String csrf,
                               @RequestParam(required = false) List<String> asientos,
                               HttpSession sesion, HttpServletResponse respuesta, Model modelo) {
        if (!formularioValido(funcion, compra, csrf, sesion)) {
            respuesta.setStatus(403);
            modelo.addAttribute("mensaje", "El formulario venció. Vuelve a abrir la sala.");
            return cargarSala(funcion, compra, new ArrayList<>(), sesion, respuesta, modelo);
        }
        if (asientos == null || asientos.isEmpty()) {
            respuesta.setStatus(400);
            modelo.addAttribute("mensaje", "Selecciona al menos un asiento para continuar.");
            return cargarSala(funcion, compra, new ArrayList<>(), sesion, respuesta, modelo);
        }
        cargarSala(funcion, compra, asientos, sesion, respuesta, modelo);
        Map<String, Object> datos = (Map<String, Object>) modelo.getAttribute("funcion");
        if (datos == null) {
            return "sala";
        }
        try {
            validarAsientos(((Number) datos.get("aforo")).intValue(), asientos);
            Set<String> ocupados = (Set<String>) modelo.getAttribute("ocupados");
            for (int i = 0; i < asientos.size(); i++) {
                if (ocupados.contains(asientos.get(i))) {
                    throw new IllegalArgumentException("Un asiento seleccionado ya está ocupado. Elige otro.");
                }
            }
        } catch (IllegalArgumentException error) {
            respuesta.setStatus(409);
            modelo.addAttribute("mensaje", error.getMessage());
            return "sala";
        }
        sesion.setAttribute("asientosPendientes", new ArrayList<>(asientos));
        sesion.removeAttribute("compraInvitado");
        if ("USER".equals(sesion.getAttribute("rol"))) {
            return "redirect:/confirmar-compra";
        }
        return "redirect:/login?destino=compra";
    }

    @GetMapping("/confirmar-compra")
    @SuppressWarnings("unchecked")
    public String confirmar(HttpSession sesion, HttpServletResponse respuesta, Model modelo) {
        List<String> asientos = (List<String>) sesion.getAttribute("asientosPendientes");
        if (asientos == null || asientos.isEmpty()) {
            return "redirect:/cartelera";
        }
        boolean invitado = Boolean.TRUE.equals(sesion.getAttribute("compraInvitado"));
        if (!invitado && !"USER".equals(sesion.getAttribute("rol"))) {
            return "redirect:/login?destino=compra";
        }
        int funcion = (Integer) sesion.getAttribute("funcionCompra");
        String compra = (String) sesion.getAttribute("compraToken");
        cargarSala(funcion, compra, asientos, sesion, respuesta, modelo);
        Map<String, Object> datos = (Map<String, Object>) modelo.getAttribute("funcion");
        if (datos == null) {
            return "sala";
        }
        BigDecimal precio = (BigDecimal) datos.get("precio");
        modelo.addAttribute("asientos", asientos);
        modelo.addAttribute("total", precio.multiply(BigDecimal.valueOf(asientos.size())));
        modelo.addAttribute("invitado", invitado);
        return "confirmar-compra";
    }

    @PostMapping("/comprar")
    @SuppressWarnings("unchecked")
    public String comprar(@RequestParam int funcion,
                          @RequestParam(defaultValue = "") String compra,
                          @RequestParam(defaultValue = "") String csrf,
                          HttpSession sesion, HttpServletResponse respuesta, Model modelo) {
        if (csrf.isEmpty() || !csrf.equals(sesion.getAttribute("csrf"))) {
            respuesta.setStatus(403);
            modelo.addAttribute("mensaje", "El formulario venció. Vuelve a abrir la sala.");
            return cargarSala(funcion, compra, new ArrayList<>(), sesion, respuesta, modelo);
        }
        boolean comoInvitado = Boolean.TRUE.equals(sesion.getAttribute("compraInvitado"));
        if (!comoInvitado && !"USER".equals(sesion.getAttribute("rol"))) {
            return "redirect:/login?destino=compra";
        }
        long usuario = 0;
        if (!comoInvitado) {
            usuario = ((Number) sesion.getAttribute("usuarioId")).longValue();
        }
        String invitado = (String) sesion.getAttribute("invitadoId");
        String destino = "redirect:/compra?codigo=" + compra;
        if (usuario > 0) {
            destino = "redirect:/mis-tickets?compra=" + compra;
        }
        if (compraExistente(compra, usuario, invitado, funcion)) {
            return destino;
        }
        if (!formularioValido(funcion, compra, csrf, sesion)) {
            respuesta.setStatus(403);
            modelo.addAttribute("mensaje", "Este formulario ya no es válido. Vuelve a abrir la sala.");
            return cargarSala(funcion, compra, new ArrayList<>(), sesion, respuesta, modelo);
        }
        List<String> asientos = (List<String>) sesion.getAttribute("asientosPendientes");
        if (asientos == null || asientos.isEmpty()) {
            respuesta.setStatus(400);
            modelo.addAttribute("mensaje", "Selecciona al menos un asiento para comprar.");
            return cargarSala(funcion, compra, new ArrayList<>(), sesion, respuesta, modelo);
        }
        try {
            guardarCompra(compra, usuario, invitado, funcion, asientos);
        } catch (IllegalArgumentException error) {
            respuesta.setStatus(409);
            modelo.addAttribute("mensaje", error.getMessage());
            return cargarSala(funcion, compra, asientos, sesion, respuesta, modelo);
        } catch (SQLException error) {
            if ("23505".equals(error.getSQLState())) {
                if (compraExistente(compra, usuario, invitado, funcion)) {
                    return destino;
                }
                respuesta.setStatus(409);
                modelo.addAttribute("mensaje", "Un asiento acaba de ocuparse. Elige entre los disponibles.");
                return cargarSala(funcion, compra, asientos, sesion, respuesta, modelo);
            }
            throw new IllegalStateException("No se pudo guardar la compra", error);
        }
        sesion.removeAttribute("asientosPendientes");
        return destino;
    }

    private boolean formularioValido(int funcion, String compra, String csrf, HttpSession sesion) {
        return !csrf.isEmpty() && csrf.equals(sesion.getAttribute("csrf"))
                && !compra.isEmpty() && compra.equals(sesion.getAttribute("compraToken"))
                && Integer.valueOf(funcion).equals(sesion.getAttribute("funcionCompra"));
    }

    private Set<String> validarAsientos(int aforo, List<String> asientos) {
        Set<String> elegidos = new HashSet<>();
        for (int i = 0; i < asientos.size(); i++) {
            String asiento = asientos.get(i);
            if (!asiento.matches("[A-Z](?:[1-9]|10)")) {
                throw new IllegalArgumentException("Selecciona únicamente asientos de esta sala.");
            }
            int numero = (asiento.charAt(0) - 'A') * 10 + Integer.parseInt(asiento.substring(1));
            if (numero > aforo || !elegidos.add(asiento)) {
                throw new IllegalArgumentException("La selección contiene un asiento inválido o repetido.");
            }
        }
        return elegidos;
    }

    private boolean compraExistente(String compra, long usuario, String invitado, int funcion) {
        int cantidad = bd.queryForObject(
                "SELECT COUNT(*) FROM compras WHERE id = ? AND COALESCE(usuario_id, 0) = ? "
                + "AND (? > 0 OR invitado = ?) AND funcion_id = ?",
                Integer.class, compra, usuario, usuario, invitado, funcion);
        return cantidad > 0;
    }

    @GetMapping("/compra")
    public String comprobante(@RequestParam(defaultValue = "") String codigo, HttpSession sesion,
                               HttpServletResponse respuesta, Model modelo) {
        List<Map<String, Object>> compras = bd.queryForList(
                "SELECT c.*, p.nombre AS pelicula, s.nombre AS sala, "
                + "FORMATDATETIME(f.fecha, 'dd/MM/yyyy') AS fechaTexto, "
                + "FORMATDATETIME(f.hora, 'h:mm a', 'en') AS horaTexto "
                + "FROM compras c JOIN funciones f ON f.id = c.funcion_id "
                + "JOIN peliculas p ON p.id = f.pelicula_id JOIN salas s ON s.id = f.sala_id "
                + "WHERE c.id = ? AND c.usuario_id IS NULL AND c.invitado = ?",
                codigo, sesion.getAttribute("invitadoId"));
        if (compras.isEmpty()) {
            respuesta.setStatus(404);
            modelo.addAttribute("mensaje", "Este comprobante no está disponible en tu sesión.");
        } else {
            modelo.addAttribute("compraConfirmada", compras.get(0));
            modelo.addAttribute("tickets", bd.queryForList(
                    "SELECT id, asiento, precio FROM tickets WHERE compra_id = ? ORDER BY turno", codigo));
        }
        return "compra";
    }

    private String cargarSala(int funcion, String compra, List<String> seleccionados, HttpSession sesion,
                              HttpServletResponse respuesta, Model modelo) {
        List<Map<String, Object>> funciones = bd.queryForList(
                "SELECT f.*, p.nombre AS pelicula, s.nombre AS sala, s.aforo, "
                + "FORMATDATETIME(f.fecha, 'dd/MM/yyyy') AS fechaTexto, "
                + "FORMATDATETIME(f.hora, 'h:mm a', 'en') AS horaTexto "
                + "FROM funciones f JOIN peliculas p ON p.id = f.pelicula_id "
                + "JOIN generos g ON g.id = p.genero_id JOIN salas s ON s.id = f.sala_id "
                + "WHERE f.id = ? AND f.activo AND p.activo AND g.activo AND s.activo AND f.fecha >= CURRENT_DATE", funcion);
        if (funciones.isEmpty()) {
            respuesta.setStatus(404);
            modelo.addAttribute("mensaje", "Esta función no está disponible para comprar.");
            return "sala";
        }
        Map<String, Object> datos = funciones.get(0);
        int aforo = ((Number) datos.get("aforo")).intValue();
        Set<String> ocupados = new HashSet<>();
        List<Map<String, Object>> tickets = bd.queryForList(
                "SELECT asiento FROM tickets WHERE funcion_id = ? AND activo = TRUE AND asiento IS NOT NULL", funcion);
        for (int i = 0; i < tickets.size(); i++) {
            ocupados.add((String) tickets.get(i).get("asiento"));
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        for (int inicio = 0; inicio < aforo; inicio += 10) {
            String letra = String.valueOf((char) ('A' + inicio / 10));
            List<Map<String, Object>> lugares = new ArrayList<>();
            for (int columna = 1; columna <= 10 && inicio + columna <= aforo; columna++) {
                String codigo = letra + columna;
                Map<String, Object> lugar = new LinkedHashMap<>();
                lugar.put("codigo", codigo);
                lugar.put("columna", columna);
                lugar.put("ocupado", ocupados.contains(codigo));
                lugar.put("seleccionado", seleccionados.contains(codigo) && !ocupados.contains(codigo));
                lugares.add(lugar);
            }
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("letra", letra);
            fila.put("asientos", lugares);
            filas.add(fila);
        }
        int entradas = bd.queryForObject("SELECT COUNT(*) FROM tickets WHERE funcion_id = ? AND activo = TRUE",
                Integer.class, funcion);
        modelo.addAttribute("funcion", datos);
        modelo.addAttribute("filas", filas);
        modelo.addAttribute("ocupados", ocupados);
        modelo.addAttribute("libres", Math.max(0, aforo - entradas));
        modelo.addAttribute("compra", compra);
        modelo.addAttribute("csrf", sesion.getAttribute("csrf"));
        return "sala";
    }

    private void guardarCompra(String compra, long usuario, String invitado, int funcion,
                                List<String> asientos) throws SQLException {
        try (Connection conexion = conexiones.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                int aforo;
                BigDecimal precio;
                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT f.*, p.activo AS peliculaActiva, g.activo AS generoActivo, s.activo AS salaActiva, s.aforo "
                        + "FROM funciones f JOIN peliculas p ON p.id = f.pelicula_id "
                        + "JOIN generos g ON g.id = p.genero_id JOIN salas s ON s.id = f.sala_id WHERE f.id = ? FOR UPDATE")) {
                    consulta.setInt(1, funcion);
                    try (ResultSet resultado = consulta.executeQuery()) {
                        if (!resultado.next() || !resultado.getBoolean("activo")
                                || !resultado.getBoolean("peliculaActiva") || !resultado.getBoolean("generoActivo")
                                || !resultado.getBoolean("salaActiva")
                                || resultado.getDate("fecha").toLocalDate().isBefore(LocalDate.now())) {
                            throw new IllegalArgumentException("La función ya no está disponible.");
                        }
                        aforo = resultado.getInt("aforo");
                        precio = resultado.getBigDecimal("precio");
                    }
                }
                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT usuario_id, invitado, funcion_id FROM compras WHERE id = ?")) {
                    consulta.setString(1, compra);
                    try (ResultSet resultado = consulta.executeQuery()) {
                        if (resultado.next()) {
                            if (resultado.getLong("usuario_id") != usuario || resultado.getInt("funcion_id") != funcion
                                    || (usuario == 0 && !invitado.equals(resultado.getString("invitado")))) {
                                throw new IllegalArgumentException("La compra no corresponde a tu sesión.");
                            }
                            conexion.rollback();
                            return;
                        }
                    }
                }
                Set<String> elegidos = validarAsientos(aforo, asientos);
                int ultimo = 0;
                int ocupacion = 0;
                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT turno, asiento, activo FROM tickets WHERE funcion_id = ?")) {
                    consulta.setInt(1, funcion);
                    try (ResultSet resultado = consulta.executeQuery()) {
                        while (resultado.next()) {
                            ultimo = Math.max(ultimo, resultado.getInt("turno"));
                            if (resultado.getBoolean("activo")) {
                                ocupacion++;
                                if (elegidos.contains(resultado.getString("asiento"))) {
                                    throw new IllegalArgumentException("Un asiento seleccionado ya está ocupado. Elige otro.");
                                }
                            }
                        }
                    }
                }
                if (ocupacion + asientos.size() > aforo) {
                    throw new IllegalArgumentException("No hay suficientes asientos disponibles para esta selección.");
                }
                BigDecimal total = precio.multiply(BigDecimal.valueOf(asientos.size()));
                try (PreparedStatement insercion = conexion.prepareStatement(
                        "INSERT INTO compras (id, usuario_id, funcion_id, cantidad, total, invitado) VALUES (?, ?, ?, ?, ?, ?)")) {
                    insercion.setString(1, compra);
                    if (usuario > 0) {
                        insercion.setLong(2, usuario);
                    } else {
                        insercion.setNull(2, Types.BIGINT);
                    }
                    insercion.setInt(3, funcion);
                    insercion.setInt(4, asientos.size());
                    insercion.setBigDecimal(5, total);
                    if (usuario > 0) {
                        insercion.setNull(6, Types.VARCHAR);
                    } else {
                        insercion.setString(6, invitado);
                    }
                    insercion.executeUpdate();
                }
                try (PreparedStatement insercion = conexion.prepareStatement(
                        "INSERT INTO tickets (usuario_id, funcion_id, turno, precio, fecha_compra, asiento, compra_id) "
                        + "VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?)")) {
                    for (int i = 0; i < asientos.size(); i++) {
                        if (usuario > 0) {
                            insercion.setLong(1, usuario);
                        } else {
                            insercion.setNull(1, Types.BIGINT);
                        }
                        insercion.setInt(2, funcion);
                        insercion.setInt(3, ultimo + i + 1);
                        insercion.setBigDecimal(4, precio);
                        insercion.setString(5, asientos.get(i));
                        insercion.setString(6, compra);
                        insercion.executeUpdate();
                    }
                }
                conexion.commit();
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                throw error;
            }
        }
    }
}
