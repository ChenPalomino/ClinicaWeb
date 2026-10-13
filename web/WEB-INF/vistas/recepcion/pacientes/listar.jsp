<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.PacienteDTO, modelo.dto.PaginaDTO, modelo.dto.FiltroBusquedaDTO, modelo.dto.OpcionDTO, controlador.FormatoVista" %>
<%-- Listado de PACIENTES (Recepción y Administrador). RF-02: buscar por DNI o apellidos. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<PacienteDTO> pagina = (PaginaDTO<PacienteDTO>) request.getAttribute("pagina");
    FiltroBusquedaDTO filtro = (FiltroBusquedaDTO) request.getAttribute("filtro");
    @SuppressWarnings("unchecked")
    List<OpcionDTO> coberturas = (List<OpcionDTO>) request.getAttribute("coberturas");
    boolean errorDatos = request.getAttribute("errorDatos") != null;

    String urlLista = ctx + "/recepcion/pacientes";
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    Integer coberturaSel = filtro == null ? null : filtro.getGrupo();
    String estadoSel = filtro == null || filtro.getEstado() == null ? "" : filtro.getEstado();
    String base = "q=" + URLEncoder.encode(q, "UTF-8") + (coberturaSel != null ? "&cobertura=" + coberturaSel : "")
                + (estadoSel.isEmpty() ? "" : "&estado=" + estadoSel);
    String regreso = base + (pagina != null ? "&pagina=" + pagina.getPagina() : "");
    boolean buscaDni = q.matches("\\d{8}");
%>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0">
            <i class="fas fa-user-injured mr-2"></i>Pacientes
            <% if (pagina != null) { %><span class="badge badge-light border ml-1"><%= pagina.getTotal() %></span><% } %>
        </h3>
        <a href="<%= urlLista %>?accion=nuevo" class="btn btn-primary btn-sm"><i class="fas fa-plus mr-1"></i> Nuevo paciente</a>
    </div>

    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-5 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" autofocus
                           placeholder="DNI, apellidos o N.º de historia" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-2 mb-2">
                <label for="cobertura" class="small text-muted mb-1">Cobertura</label>
                <select id="cobertura" name="cobertura" class="form-control">
                    <option value="">Todas</option>
                    <% if (coberturas != null) for (OpcionDTO c : coberturas) { %>
                    <option value="<%= c.getId() %>" <%= coberturaSel != null && coberturaSel == c.getId() ? "selected" : "" %>><%= EscapeHtml.texto(c.getNombre()) %></option>
                    <% } %>
                </select>
            </div>
            <div class="col-6 col-md-2 mb-2">
                <label for="estado" class="small text-muted mb-1">Estado</label>
                <select id="estado" name="estado" class="form-control">
                    <option value="">Todos</option>
                    <option value="ACTIVO" <%= "ACTIVO".equals(estadoSel) ? "selected" : "" %>>Activos</option>
                    <option value="INACTIVO" <%= "INACTIVO".equals(estadoSel) ? "selected" : "" %>>Desactivados</option>
                </select>
            </div>
            <div class="col-md-3 mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Buscar</button>
                <% if (filtro != null && filtro.hayFiltros()) { %><a href="<%= urlLista %>" class="btn btn-link">Limpiar</a><% } %>
            </div>
        </form>
    </div>

    <div class="card-body p-0">
    <% if (errorDatos || pagina == null) { %>
        <div class="estado-vacio">
            <i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar los pacientes.</strong></p>
            <p class="text-muted">Revisa la conexión con la base de datos y recarga la página.</p>
        </div>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio">
            <i class="fas fa-user-injured text-muted"></i>
            <% if (filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>Ningún paciente coincide con «<%= EscapeHtml.texto(q) %>».</strong></p>
                <% if (buscaDni) { %>
                <a href="<%= urlLista %>?accion=nuevo&dni=<%= q %>" class="btn btn-primary btn-sm mt-2"><i class="fas fa-plus mr-1"></i> Registrar al paciente con DNI <%= q %></a>
                <% } else { %><a href="<%= urlLista %>">Quitar filtros</a><% } %>
            <% } else { %>
                <p class="mb-1"><strong>Aún no hay pacientes registrados.</strong></p>
                <a href="<%= urlLista %>?accion=nuevo">Registrar el primero</a>
            <% } %>
        </div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover table-usuarios mb-0">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Paciente</th>
                    <th scope="col" class="d-none d-sm-table-cell">DNI</th>
                    <th scope="col" class="d-none d-md-table-cell">Edad</th>
                    <th scope="col" class="d-none d-sm-table-cell">Cobertura</th>
                    <th scope="col" class="d-none d-lg-table-cell">Contacto</th>
                    <th scope="col">Estado</th>
                    <th scope="col" class="text-right">Acciones</th>
                </tr>
            </thead>
            <tbody>
            <% for (PacienteDTO p : pagina.getElementos()) {
                   String nombre = EscapeHtml.texto(p.getNombreCompleto()); %>
                <tr class="<%= p.isActivo() ? "" : "fila-inactiva" %>">
                    <td>
                        <div class="d-flex align-items-center">
                            <span class="avatar-iniciales avatar-tabla mr-2" aria-hidden="true"><%= EscapeHtml.texto(p.getIniciales()) %></span>
                            <div>
                                <div class="font-weight-bold"><%= nombre %>
                                    <% if (!p.getObservaciones().isEmpty()) { %><i class="fas fa-exclamation-circle text-warning ml-1" data-toggle="tooltip" title="<%= EscapeHtml.texto(p.getObservaciones()) %>" aria-label="Tiene observaciones"></i><% } %>
                                </div>
                                <small class="text-muted">Historia <%= EscapeHtml.texto(p.getNumeroHistoria()) %>
                                    <% if (p.isTieneCuenta()) { %> · <span title="Puede entrar al portal del paciente"><i class="fas fa-globe"></i> Portal</span><% } %></small>
                            </div>
                        </div>
                    </td>
                    <td class="d-none d-sm-table-cell"><%= EscapeHtml.texto(p.getDni()) %></td>
                    <td class="d-none d-md-table-cell"><%= p.getEdad() == null ? "—" : p.getEdad() + " años" %></td>
                    <td class="d-none d-sm-table-cell">
                        <span class="badge <%= "PARTICULAR".equals(p.getTipoCobertura()) ? "badge-light border" : "badge-info" %>"><%= EscapeHtml.texto(p.getNombreCobertura()) %></span>
                    </td>
                    <td class="d-none d-lg-table-cell small">
                        <% if (!p.getCelular().isEmpty()) { %><div><i class="fas fa-mobile-alt text-muted mr-1"></i><%= EscapeHtml.texto(p.getCelular()) %></div><% } %>
                        <% if (!p.getCorreo().isEmpty()) { %><div class="text-truncate" style="max-width:220px"><i class="fas fa-envelope text-muted mr-1"></i><%= EscapeHtml.texto(p.getCorreo()) %></div><% } %>
                    </td>
                    <td><% if (p.isActivo()) { %><span class="badge badge-success"><i class="fas fa-check mr-1"></i>Activo</span>
                        <% } else { %><span class="badge badge-secondary"><i class="fas fa-ban mr-1"></i>Desactivado</span><% } %></td>
                    <td class="text-right text-nowrap">
                        <a href="<%= urlLista %>?accion=editar&id=<%= p.getIdPaciente() %>" class="btn btn-sm btn-outline-primary" title="Editar" aria-label="Editar a <%= nombre %>"><i class="fas fa-pen"></i></a>
                        <% if (p.isActivo()) { %>
                        <button type="button" class="btn btn-sm btn-outline-danger" title="Desactivar" aria-label="Desactivar a <%= nombre %>"
                                data-confirmar="desactivar" data-id="<%= p.getIdPaciente() %>" data-nombre="<%= nombre %>" data-color="btn-danger"
                                data-titulo="Desactivar paciente" data-boton="Sí, desactivar"
                                data-mensaje="¿Desactivar a {usuario}? No se borra: su historia y sus atenciones se conservan.">
                            <i class="fas fa-user-slash"></i></button>
                        <% } else { %>
                        <button type="button" class="btn btn-sm btn-outline-success" title="Reactivar" aria-label="Reactivar a <%= nombre %>"
                                data-confirmar="reactivar" data-id="<%= p.getIdPaciente() %>" data-nombre="<%= nombre %>" data-color="btn-success"
                                data-titulo="Reactivar paciente" data-boton="Sí, reactivar" data-mensaje="¿Reactivar a {usuario}?">
                            <i class="fas fa-user-check"></i></button>
                        <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    <% } %>
    </div>
    <%@ include file="/WEB-INF/vistas/comun/paginacion.jspf" %>
</div>

<div id="datosConfirmar" data-action="<%= urlLista %>" data-regreso="<%= EscapeHtml.texto(regreso) %>" hidden></div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
