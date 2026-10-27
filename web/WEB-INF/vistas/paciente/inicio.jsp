<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.*, modelo.dto.CitaDTO, modelo.citas.FechaTexto" %>
<%-- Inicio del PACIENTE (portal): su próxima cita en grande (o una invitación clara a reservar) y accesos directos. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>
<%
    @SuppressWarnings("unchecked") List<CitaDTO> proximas = (List<CitaDTO>) request.getAttribute("proximas");
    LocalDate hoy = (LocalDate) request.getAttribute("hoy");
    Long atendidas = (Long) request.getAttribute("atendidas");
    Integer horasAttr = (Integer) request.getAttribute("horasCancelacion");
    int horas = horasAttr == null ? 24 : horasAttr;
    CitaDTO siguiente = proximas == null || proximas.isEmpty() ? null : proximas.get(0);
%>
<div class="row">
    <div class="col-md-4 animar">
        <a href="<%= ctx %>/paciente/citas" class="info-box text-reset" title="Ver mis citas">
            <span class="info-box-icon bg-primary elevation-1"><i class="fas fa-calendar-check"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Próxima cita</span>
                <% if (siguiente == null) { %>
                <span class="info-box-number text-muted">Ninguna</span>
                <small class="text-muted">Reserva cuando la necesites</small>
                <% } else { %>
                <span class="info-box-number"><%= FechaTexto.relativa(siguiente.getFecha(), hoy) %></span>
                <small class="text-muted"><%= FechaTexto.hora(siguiente.getHoraInicio()) %> · <%= EscapeHtml.texto(siguiente.getEspecialidad()) %></small>
                <% } %>
            </div>
        </a>
    </div>
    <div class="col-md-4 animar">
        <a href="<%= ctx %>/paciente/citas" class="info-box text-reset">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-history"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Consultas atendidas</span>
                <span class="info-box-number"><%= atendidas == null ? "—" : String.valueOf(atendidas) %></span>
                <small class="text-muted">Tu historial de citas</small>
            </div>
        </a>
    </div>
    <div class="col-md-4 animar">
        <a href="<%= ctx %>/paciente/pagos" class="info-box text-reset">
            <span class="info-box-icon bg-success elevation-1"><i class="fas fa-receipt"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Pagos y comprobantes</span>
                <span class="info-box-number"><i class="fas fa-arrow-right small"></i></span>
                <small class="text-muted">Boletas y facturas</small>
            </div>
        </a>
    </div>
</div>

<div class="card animar">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-calendar-alt mr-2"></i>Mis próximas citas</h3>
        <a href="<%= ctx %>/paciente/citas?accion=reservar" class="btn btn-primary btn-sm"><i class="fas fa-calendar-plus mr-1"></i> Reservar cita</a>
    </div>
    <% if (request.getAttribute("errorCitas") != null) { %>
    <div class="card-body text-muted">No pudimos cargar tus citas en este momento. Recarga la página en unos minutos.</div>
    <% } else if (siguiente == null) { %>
    <div class="card-body estado-vacio text-center py-4">
        <i class="far fa-calendar-plus fa-2x text-primary mb-2" aria-hidden="true"></i>
        <p class="mb-1"><strong>No tienes citas pendientes.</strong></p>
        <p class="text-muted mb-3">Reserva en tres pasos: especialidad, día y hora. Te enviaremos la confirmación al correo de
            <a href="<%= ctx %>/perfil">Mi perfil</a>.</p>
        <a href="<%= ctx %>/paciente/citas?accion=reservar" class="btn btn-primary"><i class="fas fa-calendar-plus mr-1"></i> Reservar una cita</a>
    </div>
    <% } else { %>
    <ul class="list-group list-group-flush">
        <% boolean primera = true; for (CitaDTO c : proximas) { %>
        <li class="list-group-item">
            <a href="<%= ctx %>/paciente/citas?accion=ver&id=<%= c.getIdCita() %>" class="cita-tarjeta text-reset">
                <div class="fecha-bloque" aria-hidden="true">
                    <span class="mes"><%= FechaTexto.mesCorto(c.getFecha()) %></span>
                    <span class="dia"><%= c.getFecha().getDayOfMonth() %></span>
                    <span class="sem"><%= FechaTexto.diaCorto(c.getFecha()) %></span>
                </div>
                <div>
                    <% if (primera) { %><span class="badge badge-primary">La próxima</span><% } %>
                    <div class="font-weight-bold"><%= FechaTexto.relativaCompleta(c.getFecha(), hoy) %> · <%= FechaTexto.hora(c.getHoraInicio()) %></div>
                    <div><%= EscapeHtml.texto(c.getEspecialidad()) %> con Dr(a). <%= EscapeHtml.texto(c.getNombreMedico()) %></div>
                    <small class="text-muted"><i class="fas fa-door-open mr-1"></i><%= EscapeHtml.texto(c.getConsultorio()) %> · <%= EscapeHtml.texto(c.getCodigo()) %></small>
                    <%@ include file="/WEB-INF/vistas/comun/precio-cita.jspf" %>
                </div>
            </a>
        </li>
        <% primera = false; } %>
    </ul>
    <div class="card-footer small text-muted">
        <i class="fas fa-info-circle mr-1"></i>Llega 15 minutos antes con tu DNI. Puedes cancelar o cambiar una cita hasta <%= horas %> horas antes en
        <a href="<%= ctx %>/paciente/citas">Mis citas</a>.
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
