<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.*, modelo.dto.CitaDTO, modelo.entidades.Rol, modelo.citas.FechaTexto, controlador.FormatoVista, controlador.citas.AccionesCita" %>
<%--
    DETALLE DE UNA CITA: cuándo y dónde, el paciente (con sus alertas), lo que pasó con la cita
    (línea de tiempo) y SOLO las acciones que ese rol puede hacer ahora (AccionesCita + ReglasCita).
--%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    CitaDTO c = (CitaDTO) request.getAttribute("cita");
    AccionesCita ac = (AccionesCita) request.getAttribute("acciones");
    LocalDate hoy = (LocalDate) request.getAttribute("hoy");
    String rutaCitas = (String) request.getAttribute("rutaCitas");
    boolean esPaciente = usuarioSesion.getRolId() == Rol.PACIENTE;
    boolean esPersonal = usuarioSesion.getRolId() == Rol.ADMINISTRADOR || usuarioSesion.getRolId() == Rol.RECEPCIONISTA;
    String volver = esPaciente ? rutaCitas : rutaCitas + "?vista=dia&fecha=" + c.getFecha();
    String resumenCita = c.getNombrePaciente() + " · " + FechaTexto.relativa(c.getFecha(), hoy) + " a las "
                       + FechaTexto.hora(c.getHoraInicio()) + " · " + c.getEspecialidad();
%>
<p class="mb-2"><a href="<%= volver %>"><i class="fas fa-arrow-left mr-1"></i><%= esPaciente ? "Volver a mis citas" : "Volver a la agenda de ese día" %></a></p>

<div class="row">
    <div class="col-lg-8">
        <div class="card cita-destacada">
            <div class="card-header d-flex flex-wrap align-items-center">
                <h3 class="card-title mr-auto"><i class="fas fa-calendar-check mr-2"></i>Cita <span class="text-monospace"><%= EscapeHtml.texto(c.getCodigo()) %></span></h3>
                <span class="badge <%= c.getClaseEstado() %> p-2" style="font-size:.9rem"><i class="<%= c.getIconoEstado() %> mr-1"></i><%= c.isConfirmada() ? "En sala de espera" : c.getEstadoLegible() %></span>
            </div>
            <div class="card-body">
                <div class="cita-tarjeta mb-3">
                    <div class="fecha-bloque" aria-hidden="true">
                        <span class="mes"><%= FechaTexto.mesCorto(c.getFecha()) %></span>
                        <span class="dia"><%= c.getFecha().getDayOfMonth() %></span>
                        <span class="sem"><%= FechaTexto.diaCorto(c.getFecha()) %></span>
                    </div>
                    <div>
                        <div class="h5 mb-1"><%= FechaTexto.relativaCompleta(c.getFecha(), hoy) %></div>
                        <div class="h6 mb-2"><i class="far fa-clock mr-1 text-muted"></i><%= FechaTexto.hora(c.getHoraInicio()) %> – <%= FechaTexto.hora(c.getHoraFin()) %></div>
                        <div><i class="fas fa-user-md mr-1 text-muted"></i>Dr(a). <%= EscapeHtml.texto(c.getNombreMedico()) %> · <%= EscapeHtml.texto(c.getEspecialidad()) %></div>
                        <div><i class="fas fa-door-open mr-1 text-muted"></i><%= EscapeHtml.texto(c.getConsultorio()) %></div>
                        <%@ include file="/WEB-INF/vistas/comun/precio-cita.jspf" %>
                    </div>
                </div>
                <% if (!c.getMotivo().isBlank()) { %>
                <p class="mb-2"><span class="text-muted">Motivo de la consulta:</span> <%= EscapeHtml.texto(c.getMotivo()) %></p>
                <% } %>
                <% if (c.isCancelada()) { %>
                <div class="alert alert-light border mb-2"><i class="fas fa-ban text-danger mr-1"></i>Cancelada<%= c.getMotivoCancelacion().isBlank() ? "" : ": " + EscapeHtml.texto(c.getMotivoCancelacion()) %></div>
                <% } %>
                <% if (esPaciente && c.isReservada()) { %>
                <div class="alert alert-info mb-2"><i class="fas fa-info-circle mr-1"></i>Llega 15 minutos antes con tu DNI y acércate a recepción.</div>
                <% } %>
                <% if (ac.getBloqueoPaciente() != null) { %>
                <div class="alert alert-warning mb-2"><i class="fas fa-phone mr-1"></i><%= EscapeHtml.texto(ac.getBloqueoPaciente()) %></div>
                <% } %>

                <% if (ac.isAlguna()) { %>
                <div class="d-flex flex-wrap" style="gap:.5rem">
                    <% if (ac.isLlegada()) { %>
                    <form method="POST" action="<%= rutaCitas %>" data-un-envio>
                        <input type="hidden" name="accion" value="llegada"><input type="hidden" name="id" value="<%= c.getIdCita() %>">
                        <button type="submit" class="btn btn-warning"><i class="fas fa-user-check mr-1"></i>Registrar llegada</button>
                    </form>
                    <% } %>
                    <% if (ac.isAtender()) { %>
                    <button type="button" class="btn btn-success" data-atencion="atendida" data-id="<%= c.getIdCita() %>" data-resumen="<%= EscapeHtml.texto(resumenCita) %>">
                        <i class="fas fa-check mr-1"></i>Marcar atendida</button>
                    <% } %>
                    <% if (ac.isNoAsistio()) { %>
                    <button type="button" class="btn btn-outline-secondary" data-atencion="noasistio" data-id="<%= c.getIdCita() %>" data-resumen="<%= EscapeHtml.texto(resumenCita) %>">
                        <i class="fas fa-user-times mr-1"></i>No asistió</button>
                    <% } %>
                    <% if (ac.isCobrar()) { %>
                    <form method="POST" action="<%= rutaCitas %>" data-un-envio>
                        <input type="hidden" name="accion" value="cobrar"><input type="hidden" name="id" value="<%= c.getIdCita() %>">
                        <button type="submit" class="btn btn-outline-success"><i class="fas fa-cash-register mr-1"></i>Cobrar en caja</button>
                    </form>
                    <% } else if (ac.isVerCuenta()) { %>
                    <a href="<%= ctx %>/caja/cuentas?accion=ver&id=<%= c.getIdCuentaAtencion() %>" class="btn btn-outline-secondary">
                        <i class="fas fa-receipt mr-1"></i>Cuenta <%= EscapeHtml.texto(c.getNumeroCuenta()) %> (<%= c.isCuentaPagada() ? "pagada" : "por cobrar" %>)</a>
                    <% } %>
                    <% if (ac.isReprogramar()) { %>
                    <a href="<%= rutaCitas %>?accion=reprogramar&id=<%= c.getIdCita() %>" class="btn btn-outline-primary"><i class="fas fa-calendar-alt mr-1"></i><%= esPaciente ? "Cambiar fecha u hora" : "Reprogramar" %></a>
                    <% } %>
                    <% if (ac.isCancelar()) { %>
                    <button type="button" class="btn btn-outline-danger" data-cancelar-cita data-id="<%= c.getIdCita() %>" data-resumen="<%= EscapeHtml.texto(resumenCita) %>">
                        <i class="fas fa-times mr-1"></i>Cancelar cita</button>
                    <% } %>
                </div>
                <% } %>
            </div>
        </div>

        <% if (!esPaciente) { %>
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-user-injured mr-2"></i>Paciente</h3></div>
            <div class="card-body">
                <div class="h6 mb-1"><%= EscapeHtml.texto(c.getNombrePaciente()) %></div>
                <div class="text-muted mb-2">DNI <%= EscapeHtml.texto(c.getDniPaciente()) %> · <%= EscapeHtml.texto(c.getNumeroHistoria()) %>
                    <% if (c.getEdadPaciente() != null) { %> · <%= c.getEdadPaciente() %> años<% } %> · <%= EscapeHtml.texto(c.getCoberturaPaciente()) %></div>
                <% if (!c.getObservacionesPaciente().isBlank()) { %>
                <div class="alert alert-warning mb-2"><i class="fas fa-exclamation-triangle mr-1"></i><strong>Alerta:</strong> <%= EscapeHtml.texto(c.getObservacionesPaciente()) %></div>
                <% } %>
                <% if (esPersonal) { %>
                <div><i class="fas fa-envelope mr-1 text-muted"></i><%= c.getCorreoPaciente().isBlank() ? "<span class=\"text-muted\">Sin correo registrado</span>" : EscapeHtml.texto(c.getCorreoPaciente()) %></div>
                <div><i class="fas fa-mobile-alt mr-1 text-muted"></i><%= c.getCelularPaciente().isBlank() ? "<span class=\"text-muted\">Sin celular registrado</span>" : EscapeHtml.texto(c.getCelularPaciente()) %></div>
                <% } %>
            </div>
        </div>
        <% } %>

        <% if (ac.isVerNota() && (c.isAtendida() || !c.getNotaAtencion().isBlank())) { %>
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-notes-medical mr-2"></i>Nota de la atención</h3></div>
            <div class="card-body"><%= c.getNotaAtencion().isBlank() ? "<span class=\"text-muted\">Sin nota.</span>" : EscapeHtml.texto(c.getNotaAtencion()) %></div>
        </div>
        <% } %>
    </div>

    <div class="col-lg-4">
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-history mr-2"></i>Lo que pasó con esta cita</h3></div>
            <div class="card-body">
                <ul class="linea-tiempo">
                    <li><strong>Reservada</strong><br><small class="text-muted"><%= FormatoVista.fechaHora(c.getFechaCreacion()) %>
                        · <%= c.isDelPortal() ? "desde el portal del paciente" : "en recepción" %></small></li>
                    <% if (c.getReprogramaciones() > 0) { %>
                    <li><strong>Reprogramada <%= c.getReprogramaciones() %> <%= c.getReprogramaciones() == 1 ? "vez" : "veces" %></strong></li>
                    <% } %>
                    <% if (c.getFechaLlegada() != null) { %>
                    <li><strong>Llegó a la clínica</strong><br><small class="text-muted"><%= FormatoVista.fechaHora(c.getFechaLlegada()) %></small></li>
                    <% } %>
                    <% if (c.isAtendida()) { %>
                    <li class="verde"><strong>Atendida</strong><br><small class="text-muted"><%= FormatoVista.fechaHora(c.getFechaAtencion()) %></small></li>
                    <% } else if (c.isNoAsistio()) { %>
                    <li class="rojo"><strong>No asistió</strong><br><small class="text-muted"><%= FormatoVista.fechaHora(c.getFechaAtencion()) %></small></li>
                    <% } else if (c.isCancelada()) { %>
                    <li class="rojo"><strong>Cancelada</strong><br><small class="text-muted"><%= FormatoVista.fechaHora(c.getFechaCancelacion()) %>
                        <% if (!esPaciente) { %> · por @<%= EscapeHtml.texto(c.getCanceladaPor()) %><% } %></small></li>
                    <% } else { %>
                    <li><span class="text-muted">Pendiente: <%= c.isConfirmada() ? "esperando su turno con el médico" : "el paciente aún no llega" %></span></li>
                    <% } %>
                    <% if (c.isCuentaPagada() && !esPaciente) { %>
                    <li class="verde"><strong>Pagada en caja</strong><br><small class="text-muted">Cuenta <%= EscapeHtml.texto(c.getNumeroCuenta()) %></small></li>
                    <% } %>
                </ul>
            </div>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/vistas/comun/citas-modales.jspf" %>
<script src="<%= ctx %>/js/citas.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
