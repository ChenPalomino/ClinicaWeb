<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.*, modelo.dto.CitaDTO, modelo.citas.FechaTexto, modelo.citas.ReglasCita, controlador.citas.AccionesCita" %>
<%-- MIS CITAS (portal del paciente): próximas citas con sus acciones y el historial. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    String rutaCitas = (String) request.getAttribute("rutaCitas");
    boolean sinFicha = request.getAttribute("sinFicha") != null;
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    @SuppressWarnings("unchecked") List<CitaDTO> proximas = (List<CitaDTO>) request.getAttribute("proximas");
    @SuppressWarnings("unchecked") List<CitaDTO> historial = (List<CitaDTO>) request.getAttribute("historial");
    LocalDate hoy = (LocalDate) request.getAttribute("hoy");
    LocalDateTime ahora = (LocalDateTime) request.getAttribute("ahora");
    Integer horasAttr = (Integer) request.getAttribute("horasCancelacion");
    int horas = horasAttr == null ? 24 : horasAttr;
    boolean puedeReservarMas = Boolean.TRUE.equals(request.getAttribute("puedeReservarMas"));
%>
<% if (sinFicha) { %>
<div class="card"><div class="card-body estado-vacio">
    <i class="fas fa-id-card text-muted"></i>
    <p class="mb-1"><strong>Tu cuenta aún no tiene ficha de paciente.</strong></p>
    <p class="text-muted mb-0">Acércate a recepción con tu DNI para completarla y poder reservar citas.</p>
</div></div>
<% } else if (errorDatos || proximas == null) { %>
<div class="card"><div class="card-body estado-vacio">
    <i class="fas fa-plug text-warning"></i>
    <p class="mb-1"><strong>No pudimos cargar tus citas.</strong></p>
    <p class="text-muted mb-0">Recarga la página en unos minutos.</p>
</div></div>
<% } else { %>
<div class="d-flex flex-wrap align-items-center mb-3">
    <h2 class="h5 mb-0 mr-auto"><i class="fas fa-calendar-check mr-2 text-primary"></i>Mis próximas citas</h2>
    <% if (puedeReservarMas) { %>
    <a href="<%= rutaCitas %>?accion=reservar" class="btn btn-primary"><i class="fas fa-calendar-plus mr-1"></i> Reservar cita</a>
    <% } else { %>
    <span class="d-inline-block" tabindex="0" data-toggle="tooltip" title="Ya tienes <%= ReglasCita.MAXIMO_PENDIENTES_PORTAL %> citas pendientes. Cuando pase alguna (o canceles una) podrás reservar otra.">
        <button class="btn btn-primary" disabled style="pointer-events:none"><i class="fas fa-calendar-plus mr-1"></i> Reservar cita</button></span>
    <% } %>
</div>

<% if (proximas.isEmpty()) { %>
<div class="card"><div class="card-body estado-vacio">
    <i class="far fa-calendar-plus text-primary"></i>
    <p class="mb-1"><strong>No tienes citas pendientes.</strong></p>
    <p class="text-muted mb-3">Reserva en tres pasos: especialidad, día y hora. Te llegará la confirmación a tu correo.</p>
    <a href="<%= rutaCitas %>?accion=reservar" class="btn btn-primary"><i class="fas fa-calendar-plus mr-1"></i> Reservar mi primera cita</a>
</div></div>
<% } else { %>
    <% boolean primera = true;
       for (CitaDTO c : proximas) {
           AccionesCita ac = new AccionesCita(c, usuarioSesion, ahora, horas);
           String resumenCita = FechaTexto.relativa(c.getFecha(), hoy) + " a las " + FechaTexto.hora(c.getHoraInicio()) + " · " + c.getEspecialidad(); %>
<div class="card <%= primera ? "cita-destacada" : "" %>">
    <div class="card-body">
        <div class="cita-tarjeta">
            <div class="fecha-bloque" aria-hidden="true">
                <span class="mes"><%= FechaTexto.mesCorto(c.getFecha()) %></span>
                <span class="dia"><%= c.getFecha().getDayOfMonth() %></span>
                <span class="sem"><%= FechaTexto.diaCorto(c.getFecha()) %></span>
            </div>
            <div class="flex-grow-1">
                <% if (primera) { %><span class="badge badge-primary mb-1">Tu próxima cita</span><% } %>
                <div class="h6 mb-1"><%= FechaTexto.relativaCompleta(c.getFecha(), hoy) %> · <%= FechaTexto.hora(c.getHoraInicio()) %></div>
                <div><%= EscapeHtml.texto(c.getEspecialidad()) %> con Dr(a). <%= EscapeHtml.texto(c.getNombreMedico()) %></div>
                <div class="small text-muted"><i class="fas fa-door-open mr-1"></i><%= EscapeHtml.texto(c.getConsultorio()) %> · Código <%= EscapeHtml.texto(c.getCodigo()) %>
                    · <span class="badge <%= c.getClaseEstado() %>"><%= c.isConfirmada() ? "Ya registraste tu llegada" : c.getEstadoLegible() %></span></div>
                <% if (ac.getBloqueoPaciente() != null) { %>
                <div class="small text-warning mt-1"><i class="fas fa-phone mr-1"></i><%= EscapeHtml.texto(ac.getBloqueoPaciente()) %></div>
                <% } %>
            </div>
            <div class="text-nowrap d-none d-sm-block">
                <a href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>" class="btn btn-outline-secondary btn-sm">Ver</a>
            </div>
        </div>
        <% if (ac.isReprogramar() || ac.isCancelar()) { %>
        <div class="mt-3 d-flex flex-wrap" style="gap:.5rem">
            <% if (ac.isReprogramar()) { %>
            <a href="<%= rutaCitas %>?accion=reprogramar&id=<%= c.getIdCita() %>" class="btn btn-outline-primary btn-sm"><i class="fas fa-calendar-alt mr-1"></i>Cambiar fecha u hora</a>
            <% } %>
            <% if (ac.isCancelar()) { %>
            <button type="button" class="btn btn-outline-danger btn-sm" data-cancelar-cita data-id="<%= c.getIdCita() %>" data-resumen="<%= EscapeHtml.texto(resumenCita) %>">
                <i class="fas fa-times mr-1"></i>Cancelar</button>
            <% } %>
            <a href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>" class="btn btn-link btn-sm d-sm-none">Ver detalle</a>
        </div>
        <% } %>
    </div>
</div>
    <% primera = false; } %>
<p class="small text-muted"><i class="fas fa-info-circle mr-1"></i>Puedes cancelar o cambiar una cita desde aquí hasta <strong><%= horas %> horas antes</strong>.
    Después de ese plazo, llama a recepción.</p>
<% } %>

<div class="card mt-4">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-history mr-2"></i>Citas anteriores</h3></div>
    <div class="card-body p-0">
    <% if (historial == null || historial.isEmpty()) { %>
        <p class="text-muted p-3 mb-0">Aquí aparecerán tus citas pasadas y las canceladas.</p>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-sm table-hover mb-0">
            <thead class="thead-light"><tr><th scope="col">Fecha</th><th scope="col">Especialidad</th><th scope="col" class="d-none d-md-table-cell">Médico</th><th scope="col">Estado</th></tr></thead>
            <tbody>
            <% for (CitaDTO c : historial) { %>
                <tr>
                    <td><a href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>"><%= FechaTexto.dmy(c.getFecha()) %></a> <small class="text-muted"><%= FechaTexto.hora(c.getHoraInicio()) %></small></td>
                    <td><%= EscapeHtml.texto(c.getEspecialidad()) %></td>
                    <td class="d-none d-md-table-cell">Dr(a). <%= EscapeHtml.texto(c.getNombreMedico()) %></td>
                    <td><span class="badge <%= c.getClaseEstado() %>"><%= c.getEstadoLegible() %></span></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    <% } %>
    </div>
</div>
<% } %>
<%@ include file="/WEB-INF/vistas/comun/citas-modales.jspf" %>
<script src="<%= ctx %>/js/citas.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
