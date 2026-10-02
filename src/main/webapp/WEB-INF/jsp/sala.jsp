<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cine Amazonas | Seleccionar asientos</title>
    <link href="https://fonts.googleapis.com/css2?family=Archivo+Black&family=Manrope:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cartelera.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sala.css">
</head>
<body>
<c:set var="seccion" value="cartelera"/>
<c:set var="destinoLogin" value="sala"/>
<%@ include file="cabecera.jspf" %>
<main class="seats-page">
    <a class="back-to-movies" href="${pageContext.request.contextPath}/cartelera">← Volver a cartelera</a>
    <p class="eyebrow">Elige tu lugar</p>
    <h1>Selecciona tus asientos</h1>
    <c:if test="${not empty mensaje}"><p class="seat-alert" role="alert"><c:out value="${mensaje}"/></p></c:if>
    <c:if test="${not empty funcion}">
        <div class="function-summary">
            <div><h2><c:out value="${funcion.pelicula}"/></h2><p><c:out value="${funcion.sala}"/> · <c:out value="${funcion.fechaTexto}"/> · <c:out value="${funcion.horaTexto}"/></p></div>
            <div class="seat-price">S/ <c:out value="${funcion.precio}"/><small>por asiento</small></div>
        </div>
        <form action="${pageContext.request.contextPath}/seleccionar" method="post" class="seat-form">
            <input type="hidden" name="csrf" value="${csrf}">
            <input type="hidden" name="funcion" value="${funcion.id}">
            <input type="hidden" name="compra" value="${compra}">
            <div class="seat-room">
                <div class="screen">PANTALLA</div>
                <div class="seatmap-scroll">
                    <div class="seat-grid" role="group" aria-label="Mapa de asientos de ${funcion.sala}">
                        <c:forEach var="fila" items="${filas}">
                            <div class="seat-row">
                                <span class="row-letter"><c:out value="${fila.letra}"/></span>
                                <c:forEach var="asiento" items="${fila.asientos}">
                                    <label class="seat${asiento.ocupado ? ' occupied' : ''}" style="grid-column:${asiento.columna > 5 ? asiento.columna + 2 : asiento.columna + 1}" title="${asiento.codigo} · ${asiento.ocupado ? 'Ocupado' : 'Libre'}">
                                        <input id="asiento-${asiento.codigo}" type="checkbox" name="asientos" value="${asiento.codigo}" ${asiento.ocupado ? 'disabled' : ''} ${asiento.seleccionado ? 'checked' : ''} aria-label="Asiento ${asiento.codigo}, ${asiento.ocupado ? 'ocupado' : 'libre'}">
                                        <span><c:out value="${asiento.columna}"/></span>
                                    </label>
                                </c:forEach>
                            </div>
                        </c:forEach>
                    </div>
                </div>
                <div class="seat-legend"><span><i class="legend-free"></i>Libre</span><span><i class="legend-occupied"></i>Ocupado</span><span><i class="legend-selected"></i>Seleccionado</span></div>
            </div>
            <div class="purchase-bar">
                <p><strong><c:out value="${libres}"/> disponibles</strong><span>Marca uno o más asientos y continúa. Todavía no se registrará la compra.</span><c:if test="${sessionScope.rol ne 'USER'}"><span>En el siguiente paso eliges iniciar sesión o seguir como invitado.</span></c:if></p>
                <button class="confirm-purchase" type="submit">Continuar</button>
            </div>
        </form>
    </c:if>
</main>
<footer><strong>Cine Amazonas</strong><span>Iquitos</span><span>© 2026</span></footer>
</body>
</html>
