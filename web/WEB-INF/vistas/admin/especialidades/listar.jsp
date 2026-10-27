<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.EspecialidadDTO, controlador.FormatoVista" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked") List<EspecialidadDTO> lista = (List<EspecialidadDTO>) request.getAttribute("especialidades");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    String urlLista = ctx + "/admin/especialidades";
    String q = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
    String estadoSel = request.getParameter("estado") == null ? "" : request.getParameter("estado");
    boolean hayFiltro = !q.isEmpty() || !estadoSel.isEmpty();
    String regreso = "q=" + URLEncoder.encode(q, "UTF-8") + (estadoSel.isEmpty() ? "" : "&estado=" + URLEncoder.encode(estadoSel, "UTF-8"));
    long sinPrecio = lista == null ? 0 : lista.stream().filter(x -> x.isActivo() && !x.isTieneConsulta()).count();
%>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0"><i class="fas fa-stethoscope mr-2"></i>Especialidades
            <% if (lista != null) { %><span class="badge badge-light border ml-1"><%= lista.size() %></span><% } %></h3>
        <a href="<%= urlLista %>?accion=nuevo" class="btn btn-primary btn-sm"><i class="fas fa-plus mr-1"></i> Nueva especialidad</a>
    </div>
    <div class="card-body border-bottom py-2 small text-muted">
        <i class="fas fa-info-circle mr-1"></i>Una especialidad aparece al reservar cuando tiene al menos un médico con horario.
        Su <strong>precio de consulta</strong> es el que ven los pacientes y el que se carga en caja al cobrar la cita.
    </div>
    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-6 col-lg-5 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" placeholder="Nombre de la especialidad" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-3 col-lg-2 mb-2">
                <label for="estado" class="small text-muted mb-1">Estado</label>
                <select id="estado" name="estado" class="form-control">
                    <option value="">Todas</option>
                    <option value="ACTIVO" <%= "ACTIVO".equals(estadoSel) ? "selected" : "" %>>Activas</option>
                    <option value="INACTIVO" <%= "INACTIVO".equals(estadoSel) ? "selected" : "" %>>Desactivadas</option>
                </select>
            </div>
            <div class="col-6 col-md-3 col-lg mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Filtrar</button>
                <% if (hayFiltro) { %><a href="<%= urlLista %>" class="btn btn-link">Limpiar</a><% } %>
            </div>
        </form>
        <% if (sinPrecio > 0) { %>
        <p class="small text-warning mb-1"><i class="fas fa-exclamation-triangle mr-1"></i><%= sinPrecio %> especialidad<%= sinPrecio == 1 ? "" : "es" %> activa<%= sinPrecio == 1 ? "" : "s" %> sin precio de consulta: los pacientes verán «precio por confirmar».</p>
        <% } %>
    </div>
    <div class="card-body p-0">
    <% if (errorDatos || lista == null) { %>
        <div class="estado-vacio"><i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar las especialidades.</strong></p>
            <p class="text-muted mb-0">¿Ejecutaste el script 13 en la base de datos? Revisa la conexión y recarga.</p></div>
    <% } else if (lista.isEmpty()) { %>
        <div class="estado-vacio"><i class="fas fa-stethoscope text-muted"></i>
            <% if (hayFiltro) { %><p class="mb-1"><strong>Ninguna especialidad coincide con la búsqueda.</strong></p><a href="<%= urlLista %>">Quitar filtros</a>
            <% } else { %><p class="mb-1"><strong>Aún no hay especialidades.</strong></p><a href="<%= urlLista %>?accion=nuevo">Crear la primera</a><% } %>
        </div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="thead-light"><tr>
                <th scope="col">Especialidad</th>
                <th scope="col" class="text-center">Médicos</th>
                <th scope="col" class="text-right">Precio de consulta</th>
                <th scope="col" class="d-none d-sm-table-cell">Estado</th>
                <th scope="col" class="text-right"><span class="sr-only">Acciones</span></th></tr></thead>
            <tbody>
            <% for (EspecialidadDTO esp : lista) {
                   String nombre = EscapeHtml.texto(esp.getNombre()); %>
                <tr class="<%= esp.isActivo() ? "" : "fila-inactiva" %>">
                    <td><strong><%= nombre %></strong>
                        <% if (!esp.getDescripcion().isEmpty()) { %><div class="small text-muted"><%= EscapeHtml.texto(esp.getDescripcion()) %></div><% } %></td>
                    <td class="text-center">
                        <% if (esp.getMedicosTotal() == 0) { %><span class="text-muted">—</span>
                        <% } else { %><a href="<%= ctx %>/admin/medicos?especialidad=<%= esp.getIdEspecialidad() %>" title="Ver sus médicos"><%= esp.getMedicosActivos() %><% if (esp.getMedicosTotal() > esp.getMedicosActivos()) { %><span class="text-muted"> / <%= esp.getMedicosTotal() %></span><% } %></a><% } %>
                    </td>
                    <td class="text-right text-nowrap">
                        <% if (esp.isTieneConsulta()) { %>S/ <%= FormatoVista.soles(esp.getPrecioConsulta()) %><div class="small text-muted"><%= EscapeHtml.texto(esp.getCodigoConsulta()) %></div>
                        <% } else { %><a href="<%= urlLista %>?accion=editar&id=<%= esp.getIdEspecialidad() %>" class="badge badge-warning">Sin precio · definir</a><% } %>
                    </td>
                    <td class="d-none d-sm-table-cell"><% if (esp.isActivo()) { %><span class="badge badge-success">Activa</span><% } else { %><span class="badge badge-secondary">Desactivada</span><% } %></td>
                    <td class="text-right text-nowrap">
                        <a href="<%= urlLista %>?accion=editar&id=<%= esp.getIdEspecialidad() %>" class="btn btn-sm btn-outline-primary" title="Editar" aria-label="Editar <%= nombre %>"><i class="fas fa-pen"></i></a>
                        <% if (esp.isActivo()) { %>
                        <button type="button" class="btn btn-sm btn-outline-danger" title="Desactivar" aria-label="Desactivar <%= nombre %>"
                                data-confirmar="desactivar" data-id="<%= esp.getIdEspecialidad() %>" data-nombre="<%= nombre %>" data-color="btn-danger"
                                data-titulo="Desactivar especialidad" data-boton="Sí, desactivar"
                                data-mensaje="¿Desactivar {usuario}? Ya no aparecerá al reservar ni al configurar médicos. Las citas pasadas se conservan.">
                            <i class="fas fa-ban"></i></button>
                        <% } else { %>
                        <button type="button" class="btn btn-sm btn-outline-success" title="Reactivar" aria-label="Reactivar <%= nombre %>"
                                data-confirmar="reactivar" data-id="<%= esp.getIdEspecialidad() %>" data-nombre="<%= nombre %>" data-color="btn-success"
                                data-titulo="Reactivar especialidad" data-boton="Sí, reactivar" data-mensaje="¿Reactivar {usuario}?">
                            <i class="fas fa-check"></i></button>
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
<div id="datosConfirmar" data-action="<%= urlLista %>" data-regreso="<%= EscapeHtml.texto(regreso) %>" hidden></div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
