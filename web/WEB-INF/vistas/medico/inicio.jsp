<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.*, modelo.dto.*, modelo.citas.FechaTexto" %>
<%-- Inicio del MÉDICO: sus pacientes de HOY en orden de hora, quién ya está en sala de espera y el siguiente. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>
<%
    MedicoDTO yo = (MedicoDTO) request.getAttribute("medicoSesion");
    @SuppressWarnings("unchecked") List<CitaDTO> citasHoy = (List<CitaDTO>) request.getAttribute("citasHoy");
    ResumenCitasDTO r = (ResumenCitasDTO) request.getAttribute("resumenHoy");
    LocalDateTime ahora = (LocalDateTime) request.getAttribute("ahora");
    CitaDTO siguiente = null;
    if (citasHoy != null) for (CitaDTO c : citasHoy) if (c.isActiva() && c.getFin().isAfter(ahora)) { siguiente = c; break; }
%>
<% if (yo == null || !yo.isConfigurado()) { %>
<div class="callout callout-warning animar">
    <h5><i class="fas fa-user-md mr-2"></i>Tu agenda aún no está lista</h5>
    <p class="mb-0">El administrador debe registrar tu especialidad, consultorio y horario en «Médicos y horarios». Mientras tanto,
        completa tu presentación en <a href="<%= ctx %>/perfil">Mi perfil</a>: los pacientes la verán al reservar.</p>
</div>
<% } else { %>
<div class="row">
    <div class="col-md-4 animar">
        <a href="<%= ctx %>/medico/agenda" class="info-box text-reset">
            <span class="info-box-icon bg-primary elevation-1"><i class="fas fa-stethoscope"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Pacientes de hoy</span>
                <span class="info-box-number"><%= r == null ? "—" : String.valueOf(r.getProgramadas()) %></span>
                <small class="text-muted"><%= r == null ? "" : r.getAtendidas() + " atendidos" %></small>
            </div>
        </a>
    </div>
    <div class="col-md-4 animar">
        <a href="<%= ctx %>/medico/agenda?estado=CONFIRMADA" class="info-box text-reset">
            <span class="info-box-icon bg-warning elevation-1"><i class="fas fa-user-clock"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">En sala de espera</span>
                <span class="info-box-number"><%= r == null ? "—" : String.valueOf(r.getConfirmadas()) %></span>
                <small class="text-muted">Ya llegaron a la clínica</small>
            </div>
        </a>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-clock"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Siguiente paciente</span>
                <% if (siguiente == null) { %>
                <span class="info-box-number text-muted">Ninguno</span><small class="text-muted">No quedan citas hoy</small>
                <% } else { %>
                <span class="info-box-number"><%= FechaTexto.hora(siguiente.getHoraInicio()) %></span>
                <small class="text-truncate"><%= EscapeHtml.texto(siguiente.getNombrePaciente()) %></small>
                <% } %>
            </div>
        </div>
    </div>
</div>
<div class="card animar">
    <div class="card-header d-flex align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-calendar-day mr-2"></i>Mi agenda de hoy</h3>
        <a href="<%= ctx %>/medico/agenda" class="btn btn-outline-primary btn-sm">Ver agenda completa</a>
    </div>
    <% if (citasHoy == null || citasHoy.isEmpty()) { %>
    <div class="card-body estado-vacio text-center py-4">
        <i class="fas fa-mug-hot fa-2x text-muted mb-2" aria-hidden="true"></i>
        <p class="mb-1"><strong>No tienes citas hoy.</strong></p>
        <p class="text-muted mb-0">Revisa tu <a href="<%= ctx %>/medico/agenda?vista=semana">semana</a> para ver las próximas.</p>
    </div>
    <% } else { %>
    <ul class="list-group list-group-flush">
        <% for (CitaDTO c : citasHoy) { boolean esSiguiente = siguiente != null && c.getIdCita() == siguiente.getIdCita(); %>
        <li class="list-group-item d-flex align-items-center <%= esSiguiente ? "cita-destacada" : "" %> <%= c.isActiva() ? "" : "text-muted" %>">
            <strong class="mr-3" style="min-width:86px"><%= FechaTexto.hora(c.getHoraInicio()) %></strong>
            <a href="<%= ctx %>/medico/agenda?accion=ver&id=<%= c.getIdCita() %>" class="mr-auto text-reset">
                <%= EscapeHtml.texto(c.getNombrePaciente()) %>
                <% if (!c.getObservacionesPaciente().isBlank()) { %><i class="fas fa-exclamation-triangle alerta-paciente ml-1" title="<%= EscapeHtml.texto(c.getObservacionesPaciente()) %>"></i><% } %>
                <% if (!c.getMotivo().isBlank()) { %><small class="d-block text-muted"><%= EscapeHtml.texto(c.getMotivo()) %></small><% } %>
            </a>
            <% if (esSiguiente) { %><span class="badge badge-primary mr-2">Siguiente</span><% } %>
            <span class="badge <%= c.getClaseEstado() %>"><%= c.isConfirmada() ? "En sala de espera" : c.getEstadoLegible() %></span>
        </li>
        <% } %>
    </ul>
    <% } %>
</div>
<% } %>
<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
