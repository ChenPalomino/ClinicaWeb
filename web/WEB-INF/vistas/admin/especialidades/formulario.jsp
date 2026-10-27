<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="modelo.dto.EspecialidadDTO, controlador.FormatoVista" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    EspecialidadDTO esp = (EspecialidadDTO) request.getAttribute("especialidadForm");
    String precio = (String) request.getAttribute("precioForm");
    boolean nuevo = esp.isNuevo();
    String urlLista = ctx + "/admin/especialidades";
%>
<form method="POST" action="<%= urlLista %>" id="formEspecialidad" data-form-crud novalidate>
    <input type="hidden" name="accion" value="guardar">
    <input type="hidden" name="id" value="<%= esp.getIdEspecialidad() %>">
    <div class="row">
        <div class="col-lg-8">
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-stethoscope mr-2"></i>Datos de la especialidad</h3></div>
                <div class="card-body">
                    <div class="form-group">
                        <label for="nombre">Nombre <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" class="form-control <%= invalido(errores, "nombre") %>" id="nombre" name="nombre" maxlength="60" required
                               value="<%= EscapeHtml.texto(esp.getNombre()) %>" placeholder="Ej. Oftalmología" <%= nuevo ? "autofocus" : "" %>>
                        <%= error(errores, "nombre") %>
                    </div>
                    <div class="form-group">
                        <label for="descripcion">Descripción <small class="text-muted">(opcional)</small></label>
                        <input type="text" class="form-control <%= invalido(errores, "descripcion") %>" id="descripcion" name="descripcion" maxlength="200"
                               value="<%= EscapeHtml.texto(esp.getDescripcion()) %>" placeholder="Ej. Salud de los ojos y la visión">
                        <%= error(errores, "descripcion") %>
                    </div>
                    <div class="form-group mb-0">
                        <label for="precio">Precio de la consulta <small class="text-muted">(con IGV)</small></label>
                        <div class="input-group" style="max-width: 240px">
                            <div class="input-group-prepend"><span class="input-group-text">S/</span></div>
                            <input type="text" class="form-control <%= invalido(errores, "precio") %>" id="precio" name="precio" inputmode="decimal" maxlength="9"
                                   value="<%= EscapeHtml.texto(precio) %>" placeholder="80.00">
                        </div>
                        <%= error(errores, "precio") %>
                        <small class="form-text text-muted">
                            <% if (esp.isTieneConsulta()) { %>Se actualiza el precio del servicio <strong><%= EscapeHtml.texto(esp.getCodigoConsulta()) %></strong> del tarifario.
                            <% } else { %>Si lo escribes, se crea el servicio «Consulta de …» en el tarifario. Es el precio que verán los pacientes al reservar.<% } %>
                        </small>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-4">
            <% if (!nuevo) { %>
            <div class="card">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-info-circle mr-2"></i>Información</h3></div>
                <div class="card-body">
                    <% if (esp.getFechaCreacion() != null) { %><p class="small text-muted">Registrada el <%= FormatoVista.fecha(esp.getFechaCreacion()) %><%= esp.getUsuarioRegistro().isEmpty() ? "" : " por " + EscapeHtml.texto(esp.getUsuarioRegistro()) %>.</p><% } %>
                    <p class="small"><strong><%= esp.getMedicosActivos() %></strong> médico(s) activo(s) con esta especialidad.
                        <% if (esp.getMedicosTotal() > 0) { %><a href="<%= ctx %>/admin/medicos?especialidad=<%= esp.getIdEspecialidad() %>">Ver</a><% } %></p>
                    <span class="d-block mb-1 font-weight-bold">Estado</span>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoActivo" name="estado" value="Activo" class="custom-control-input" <%= marcado(esp.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoActivo">Activa</label>
                    </div>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoInactivo" name="estado" value="Inactivo" class="custom-control-input" <%= marcado(!esp.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoInactivo">Desactivada</label>
                    </div>
                    <%= error(errores, "estado") %>
                </div>
            </div>
            <%@ include file="/WEB-INF/vistas/comun/historial.jspf" %>
            <% } else { %>
            <div class="callout callout-info small"><i class="fas fa-lightbulb mr-1"></i>Después de crearla, asígnala a un médico en
                «Médicos y horarios» y registra su horario: desde ese momento aparece para reservar citas.</div>
            <% } %>
        </div>
    </div>
    <div class="barra-acciones">
        <a href="<%= urlLista %>" class="btn btn-outline-secondary">Cancelar</a>
        <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> <%= nuevo ? "Crear especialidad" : "Guardar cambios" %></button>
    </div>
</form>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
