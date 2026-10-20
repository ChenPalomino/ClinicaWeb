<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.MedicoDTO" %>
<%-- MÉDICOS Y HORARIOS (RF-03): cada cuenta de rol Médico y si ya puede recibir citas. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked") List<MedicoDTO> medicos = (List<MedicoDTO>) request.getAttribute("medicos");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    long pendientes = medicos == null ? 0 : medicos.stream().filter(m -> !m.isConfigurado()).count();
%>
<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-user-md mr-2"></i>Médicos y horarios
            <% if (pendientes > 0) { %><span class="badge badge-warning ml-1"><%= pendientes %> sin configurar</span><% } %></h3>
        <a href="<%= ctx %>/admin/usuarios?accion=nuevo" class="btn btn-outline-primary btn-sm"><i class="fas fa-user-plus mr-1"></i>Crear cuenta de médico</a>
    </div>
    <div class="card-body border-bottom py-2 small text-muted">
        <i class="fas fa-info-circle mr-1"></i>Para agregar un médico: créale una cuenta con el rol <strong>Médico</strong> en Usuarios y luego
        registra aquí su especialidad, consultorio y horario. Solo los médicos <span class="badge badge-success">Atiende</span> aparecen al reservar.
    </div>
    <div class="card-body p-0">
    <% if (errorDatos || medicos == null) { %>
        <div class="estado-vacio"><i class="fas fa-plug text-warning"></i><p class="mb-1"><strong>No pudimos cargar los médicos.</strong></p></div>
    <% } else if (medicos.isEmpty()) { %>
        <div class="estado-vacio"><i class="fas fa-user-md text-muted"></i>
            <p class="mb-1"><strong>Aún no hay cuentas de médicos.</strong></p>
            <a href="<%= ctx %>/admin/usuarios?accion=nuevo">Crear la primera</a></div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="thead-light"><tr>
                <th scope="col">Médico</th><th scope="col">Especialidad</th><th scope="col" class="d-none d-lg-table-cell">Horario</th>
                <th scope="col">Estado</th><th scope="col" class="text-right"><span class="sr-only">Acciones</span></th></tr></thead>
            <tbody>
            <% for (MedicoDTO m : medicos) {
                   String nombre = EscapeHtml.texto(m.getNombreConTitulo()); %>
                <tr class="<%= m.isAtiende() ? "" : "fila-inactiva" %>">
                    <td><strong><%= nombre %></strong><div class="small text-muted">@<%= EscapeHtml.texto(m.getNombreUsuario()) %>
                        <%= m.isConfigurado() ? " · CMP " + EscapeHtml.texto(m.getCmp()) : "" %></div></td>
                    <td><%= m.isConfigurado() ? EscapeHtml.texto(m.getEspecialidad()) + "<div class=\"small text-muted\">" + EscapeHtml.texto(m.getConsultorio()) + " · " + m.getDuracionCita() + " min</div>" : "<span class=\"text-muted\">—</span>" %></td>
                    <td class="d-none d-lg-table-cell small"><%= EscapeHtml.texto(m.getResumenHorario()) %></td>
                    <td>
                        <% if (!m.isConfigurado()) { %><span class="badge badge-warning">Sin configurar</span>
                        <% } else if (!m.isUsuarioActivo()) { %><span class="badge badge-secondary" title="Su cuenta está desactivada">Cuenta inactiva</span>
                        <% } else if (!m.isActivo()) { %><span class="badge badge-secondary">Citas en pausa</span>
                        <% } else if (m.getHorarios().isEmpty()) { %><span class="badge badge-warning">Sin horario</span>
                        <% } else { %><span class="badge badge-success">Atiende</span><% } %>
                    </td>
                    <td class="text-right text-nowrap">
                        <a href="<%= ctx %>/admin/medicos?accion=editar&u=<%= m.getIdUsuario() %>" class="btn btn-sm <%= m.isConfigurado() ? "btn-outline-primary" : "btn-primary" %>">
                            <i class="fas fa-<%= m.isConfigurado() ? "pen" : "plus" %> mr-1"></i><%= m.isConfigurado() ? "Editar horario" : "Configurar" %></a>
                        <% if (m.isConfigurado()) { %>
                        <button type="button" class="btn btn-sm <%= m.isActivo() ? "btn-outline-secondary" : "btn-outline-success" %>"
                                data-confirmar="<%= m.isActivo() ? "pausar" : "reanudar" %>" data-id="<%= m.getIdUsuario() %>" data-nombre="<%= nombre %>"
                                data-color="<%= m.isActivo() ? "btn-secondary" : "btn-success" %>" data-titulo="<%= m.isActivo() ? "Pausar citas" : "Reanudar citas" %>"
                                data-boton="<%= m.isActivo() ? "Sí, pausar" : "Sí, reanudar" %>"
                                data-mensaje="<%= m.isActivo() ? "¿Pausar las citas de {usuario}? Ya no aparecerá para nuevas reservas (vacaciones, licencia...). Sus citas ya reservadas se mantienen." : "¿{usuario} vuelve a recibir citas?" %>"
                                title="<%= m.isActivo() ? "Pausar (vacaciones, licencia)" : "Reanudar" %>"><i class="fas fa-<%= m.isActivo() ? "pause" : "play" %>"></i></button>
                        <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    <% } %>
    </div>
</div>
<div id="datosConfirmar" data-action="<%= ctx %>/admin/medicos" data-regreso="" hidden></div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
