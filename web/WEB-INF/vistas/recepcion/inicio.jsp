<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.*, modelo.dto.*, modelo.citas.FechaTexto, controlador.citas.AccionesCita" %>
<%-- Inicio de RECEPCIÓN (admisión y caja): citas de HOY de todos los médicos, con llegada y cobro a un clic, y su caja. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>
<%
    @SuppressWarnings("unchecked") List<CitaDTO> citasHoy = (List<CitaDTO>) request.getAttribute("citasHoy");
    ResumenCitasDTO r = (ResumenCitasDTO) request.getAttribute("resumenHoy");
    LocalDateTime ahora = (LocalDateTime) request.getAttribute("ahora");
    LocalDate hoy = (LocalDate) request.getAttribute("hoy");
    ResumenCajaDTO cajaHoy = (ResumenCajaDTO) request.getAttribute("cajaHoy");
    Integer porCobrar = (Integer) request.getAttribute("porCobrar");
    String agenda = ctx + "/recepcion/citas";
%>
<div class="row">
    <div class="col-6 col-lg-3 animar">
        <a href="<%= agenda %>" class="info-box text-reset" title="Ir a la agenda de hoy">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-calendar-day"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Citas de hoy</span>
                <span class="info-box-number"><%= r == null ? "—" : String.valueOf(r.getProgramadas()) %></span>
                <small class="text-muted"><%= r == null ? "" : r.getAtendidas() + " atendidas" %></small>
            </div>
        </a>
    </div>
    <div class="col-6 col-lg-3 animar">
        <a href="<%= agenda %>?estado=RESERVADA" class="info-box text-reset">
            <span class="info-box-icon bg-primary elevation-1"><i class="fas fa-door-open"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Por llegar</span>
                <span class="info-box-number"><%= r == null ? "—" : String.valueOf(r.getReservadas()) %></span>
                <small class="text-muted">Registra su llegada</small>
            </div>
        </a>
    </div>
    <div class="col-6 col-lg-3 animar">
        <a href="<%= agenda %>?estado=CONFIRMADA" class="info-box text-reset">
            <span class="info-box-icon bg-warning elevation-1"><i class="fas fa-user-clock"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">En sala de espera</span>
                <span class="info-box-number"><%= r == null ? "—" : String.valueOf(r.getConfirmadas()) %></span>
                <small class="text-muted">Esperan al médico</small>
            </div>
        </a>
    </div>
    <div class="col-6 col-lg-3 animar">
        <a href="<%= ctx %>/caja/cuentas<%= porCobrar != null && porCobrar > 0 ? "?estado=PENDIENTE" : "" %>" class="info-box text-reset" title="Ir a cobros">
            <span class="info-box-icon bg-success elevation-1"><i class="fas fa-cash-register"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Mis cobros de hoy</span>
                <% if (cajaHoy == null) { %>
                <span class="info-box-number text-muted">&mdash;</span>
                <small class="text-muted">No se pudo leer la caja</small>
                <% } else { %>
                <span class="info-box-number monto">S/ <%= controlador.FormatoVista.soles(cajaHoy.getTotalGeneral()) %></span>
                <small class="text-muted"><%= cajaHoy.getCantidadPagos() %> <%= cajaHoy.getCantidadPagos() == 1 ? "cobro" : "cobros" %>
                    <% if (porCobrar != null && porCobrar > 0) { %> · <strong class="text-warning"><%= porCobrar %> por cobrar</strong><% } %>
                    <% if (cajaHoy.isCerrada()) { %> · caja cerrada<% } %></small>
                <% } %>
            </div>
        </a>
    </div>
</div>
<div class="card animar">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-clipboard-list mr-2"></i>Agenda de hoy · <%= FechaTexto.largaTitulo(hoy) %></h3>
        <a href="<%= agenda %>" class="btn btn-outline-primary btn-sm mr-2">Ver agenda</a>
        <a href="<%= agenda %>?accion=reservar" class="btn btn-primary btn-sm"><i class="fas fa-calendar-plus mr-1"></i> Nueva cita</a>
    </div>
    <% if (request.getAttribute("errorCitas") != null) { %>
    <div class="card-body text-muted">No pudimos cargar las citas de hoy. Recarga la página en unos minutos.</div>
    <% } else if (citasHoy == null || citasHoy.isEmpty()) { %>
    <div class="card-body estado-vacio text-center py-4">
        <i class="fas fa-calendar-check fa-2x text-muted mb-2" aria-hidden="true"></i>
        <p class="mb-1"><strong>No hay citas programadas para hoy.</strong></p>
        <p class="text-muted mb-0">Cuando reserves una, aparecerá aquí para registrar la llegada del paciente y su cobro.</p>
    </div>
    <% } else { %>
    <div class="table-responsive">
    <table class="table table-hover mb-0 tabla-agenda">
        <thead class="thead-light"><tr><th scope="col">Hora</th><th scope="col">Paciente</th><th scope="col" class="d-none d-md-table-cell">Médico</th>
            <th scope="col">Estado</th><th scope="col" class="text-right"><span class="sr-only">Acciones</span></th></tr></thead>
        <tbody>
        <% for (CitaDTO c : citasHoy) {
               if (c.isCancelada()) continue;
               AccionesCita ac = new AccionesCita(c, usuarioSesion, ahora, 24); %>
            <tr class="<%= !c.isActiva() ? "cita-pasada" : "" %>">
                <td class="hora"><%= FechaTexto.hora(c.getHoraInicio()) %></td>
                <td><a href="<%= agenda %>?accion=ver&id=<%= c.getIdCita() %>" class="font-weight-bold"><%= EscapeHtml.texto(c.getNombrePaciente()) %></a>
                    <small class="d-block text-muted">DNI <%= EscapeHtml.texto(c.getDniPaciente()) %></small></td>
                <td class="d-none d-md-table-cell"><%= EscapeHtml.texto("Dr(a). " + c.getNombreMedico()) %><small class="d-block text-muted"><%= EscapeHtml.texto(c.getEspecialidad()) %></small></td>
                <td><span class="badge <%= c.getClaseEstado() %>"><%= c.isConfirmada() ? "En sala de espera" : c.getEstadoLegible() %></span>
                    <% if (c.isCuentaPagada()) { %><small class="d-block text-success">Pagada</small><% } %></td>
                <td class="text-right text-nowrap">
                    <% if (ac.isLlegada()) { %>
                    <form method="POST" action="<%= agenda %>" class="d-inline" data-un-envio>
                        <input type="hidden" name="accion" value="llegada"><input type="hidden" name="id" value="<%= c.getIdCita() %>">
                        <input type="hidden" name="regreso" value="">
                        <button type="submit" class="btn btn-warning btn-sm"><i class="fas fa-user-check mr-1"></i>Llegó</button>
                    </form>
                    <% } %>
                    <% if (ac.isCobrar()) { %>
                    <form method="POST" action="<%= agenda %>" class="d-inline" data-un-envio>
                        <input type="hidden" name="accion" value="cobrar"><input type="hidden" name="id" value="<%= c.getIdCita() %>">
                        <button type="submit" class="btn btn-outline-success btn-sm"><i class="fas fa-cash-register mr-1"></i>Cobrar</button>
                    </form>
                    <% } %>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>
<script src="<%= ctx %>/js/citas.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
