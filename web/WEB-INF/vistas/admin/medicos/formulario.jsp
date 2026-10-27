<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.LocalTime, modelo.dto.*" %>
<%-- RF-03: datos profesionales y HORARIO SEMANAL de un médico (hasta 2 bloques por día: mañana y tarde). --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%!
    private String h(LocalTime t) { return t == null ? "" : t.toString(); }
%>
<%
    MedicoDTO m = (MedicoDTO) request.getAttribute("medico");
    @SuppressWarnings("unchecked") List<OpcionDTO> especialidades = (List<OpcionDTO>) request.getAttribute("especialidades");
    boolean sugerido = !m.isConfigurado() && m.getHorarios().isEmpty() && errores.isEmpty();
%>
<form method="POST" action="<%= ctx %>/admin/medicos" data-un-envio novalidate>
<input type="hidden" name="accion" value="guardar">
<input type="hidden" name="u" value="<%= m.getIdUsuario() %>">
<div class="row">
    <div class="col-lg-8">
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-user-md mr-2"></i><%= EscapeHtml.texto(m.getNombreConTitulo()) %>
                <small class="text-muted ml-1">@<%= EscapeHtml.texto(m.getNombreUsuario()) %></small></h3></div>
            <div class="card-body">
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="idEspecialidad">Especialidad <span class="text-danger" aria-hidden="true">*</span></label>
                        <select id="idEspecialidad" name="idEspecialidad" class="form-control <%= invalido(errores, "idEspecialidad") %>" required>
                            <option value="">— Elige —</option>
                            <% for (OpcionDTO e : especialidades) { %>
                            <option value="<%= e.getId() %>" <%= sel(e.getId() == m.getIdEspecialidad()) %>><%= EscapeHtml.texto(e.getNombre()) %></option>
                            <% } %>
                        </select>
                        <%= error(errores, "idEspecialidad") %>
                        <small class="form-text text-muted">¿No está en la lista? <a href="<%= ctx %>/admin/especialidades?accion=nuevo">Créala aquí</a> (los cambios no guardados de este formulario se perderán).</small>
                    </div>
                    <div class="form-group col-md-6">
                        <label for="cmp">N.º de colegiatura (CMP) <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" id="cmp" name="cmp" maxlength="6" inputmode="numeric" data-solo-numeros required
                               class="form-control <%= invalido(errores, "cmp") %>" value="<%= EscapeHtml.texto(m.getCmp()) %>" placeholder="Ej. 054321">
                        <%= error(errores, "cmp") %>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="consultorio">Consultorio <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" id="consultorio" name="consultorio" maxlength="30" required
                               class="form-control <%= invalido(errores, "consultorio") %>" value="<%= EscapeHtml.texto(m.getConsultorio()) %>" placeholder="Ej. Consultorio 101">
                        <%= error(errores, "consultorio") %>
                    </div>
                    <div class="form-group col-md-6">
                        <label for="duracionCita">Duración de cada cita <span class="text-danger" aria-hidden="true">*</span></label>
                        <select id="duracionCita" name="duracionCita" class="form-control <%= invalido(errores, "duracionCita") %>">
                            <% for (int d : MedicoDTO.DURACIONES) { %><option value="<%= d %>" <%= sel(d == m.getDuracionCita()) %>><%= d %> minutos</option><% } %>
                        </select>
                        <%= error(errores, "duracionCita") %>
                    </div>
                </div>
            </div>
        </div>

        <div class="card card-outline card-primary">
            <div class="card-header d-flex flex-wrap align-items-center">
                <h3 class="card-title mr-auto"><i class="fas fa-clock mr-2"></i>Horario de atención semanal</h3>
                <button type="button" class="btn btn-link btn-sm" id="copiarLunes">Copiar el lunes a martes–viernes</button>
            </div>
            <div class="card-body p-0">
                <% if (errores.containsKey("horario")) { %>
                <div class="alert alert-danger m-3 mb-0"><i class="fas fa-exclamation-triangle mr-1"></i><%= EscapeHtml.texto(errores.get("horario")) %></div>
                <% } %>
                <% if (sugerido) { %>
                <p class="small text-info px-3 pt-3 mb-0"><i class="fas fa-lightbulb mr-1"></i>Te dejamos un horario sugerido: ajústalo al del médico antes de guardar.</p>
                <% } %>
                <div class="table-responsive">
                <table class="table mb-0 tabla-horario" id="tablaHorario">
                    <thead><tr><th scope="col">Día</th><th scope="col">Turno 1 (mañana)</th><th scope="col">Turno 2 (tarde, opcional)</th></tr></thead>
                    <tbody>
                    <% for (int d = 1; d <= 7; d++) {
                           List<HorarioDTO> b = m.horariosDelDia(d);
                           boolean on = !b.isEmpty();
                           String i1 = b.size() > 0 ? h(b.get(0).getHoraInicio()) : "", f1 = b.size() > 0 ? h(b.get(0).getHoraFin()) : "";
                           String i2 = b.size() > 1 ? h(b.get(1).getHoraInicio()) : "", f2 = b.size() > 1 ? h(b.get(1).getHoraFin()) : "";
                           if (sugerido && d <= 5) { on = true; i1 = "08:00"; f1 = "13:00"; i2 = "15:00"; f2 = "19:00"; } %>
                        <tr data-dia="<%= d %>">
                            <th scope="row">
                                <div class="custom-control custom-switch">
                                    <input type="checkbox" class="custom-control-input" id="d<%= d %>_on" name="d<%= d %>_on" <%= marcado(on) %>>
                                    <label class="custom-control-label" for="d<%= d %>_on"><%= HorarioDTO.nombreDia(d) %></label>
                                </div>
                            </th>
                            <td class="text-nowrap">
                                <input type="time" name="d<%= d %>_i1" value="<%= i1 %>" class="form-control form-control-sm d-inline-block w-auto" aria-label="<%= HorarioDTO.nombreDia(d) %>: inicio del turno 1"> –
                                <input type="time" name="d<%= d %>_f1" value="<%= f1 %>" class="form-control form-control-sm d-inline-block w-auto" aria-label="<%= HorarioDTO.nombreDia(d) %>: fin del turno 1">
                            </td>
                            <td class="text-nowrap">
                                <input type="time" name="d<%= d %>_i2" value="<%= i2 %>" class="form-control form-control-sm d-inline-block w-auto" aria-label="<%= HorarioDTO.nombreDia(d) %>: inicio del turno 2"> –
                                <input type="time" name="d<%= d %>_f2" value="<%= f2 %>" class="form-control form-control-sm d-inline-block w-auto" aria-label="<%= HorarioDTO.nombreDia(d) %>: fin del turno 2">
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
                </div>
                <p class="small text-muted p-3 mb-0">Activa los días que atiende. Deja vacío el turno 2 si solo atiende en la mañana.
                    El sistema divide cada turno en citas de la duración elegida (ej. 08:00–13:00 con 30 min = 10 citas).</p>
            </div>
            <div class="card-footer d-flex">
                <a href="<%= ctx %>/admin/medicos" class="btn btn-outline-secondary mr-auto">Volver</a>
                <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> Guardar</button>
            </div>
        </div>
    </div>
    <div class="col-lg-4">
        <%@ include file="/WEB-INF/vistas/comun/historial.jspf" %>
    </div>
</div>
</form>
<script src="<%= ctx %>/js/citas.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
