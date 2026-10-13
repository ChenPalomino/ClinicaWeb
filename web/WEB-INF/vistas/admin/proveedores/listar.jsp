<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.net.URLEncoder, modelo.dto.ProveedorDTO, modelo.dto.PaginaDTO, modelo.dto.FiltroBusquedaDTO" %>
<%-- Listado de PROVEEDORES (solo Administrador). --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<ProveedorDTO> pagina = (PaginaDTO<ProveedorDTO>) request.getAttribute("pagina");
    FiltroBusquedaDTO filtro = (FiltroBusquedaDTO) request.getAttribute("filtro");
    boolean errorDatos = request.getAttribute("errorDatos") != null;

    String urlLista = ctx + "/admin/proveedores";
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    String estadoSel = filtro == null || filtro.getEstado() == null ? "" : filtro.getEstado();
    String base = "q=" + URLEncoder.encode(q, "UTF-8") + (estadoSel.isEmpty() ? "" : "&estado=" + estadoSel);
    String regreso = base + (pagina != null ? "&pagina=" + pagina.getPagina() : "");
%>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0">
            <i class="fas fa-truck mr-2"></i>Proveedores
            <% if (pagina != null) { %><span class="badge badge-light border ml-1"><%= pagina.getTotal() %></span><% } %>
        </h3>
        <a href="<%= urlLista %>?accion=nuevo" class="btn btn-primary btn-sm"><i class="fas fa-plus mr-1"></i> Nuevo proveedor</a>
    </div>

    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-6 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60"
                           placeholder="RUC, razón social, nombre comercial o rubro" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-3 mb-2">
                <label for="estado" class="small text-muted mb-1">Estado</label>
                <select id="estado" name="estado" class="form-control">
                    <option value="">Todos</option>
                    <option value="ACTIVO" <%= "ACTIVO".equals(estadoSel) ? "selected" : "" %>>Activos</option>
                    <option value="INACTIVO" <%= "INACTIVO".equals(estadoSel) ? "selected" : "" %>>Desactivados</option>
                </select>
            </div>
            <div class="col-6 col-md-3 mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Buscar</button>
                <% if (filtro != null && filtro.hayFiltros()) { %><a href="<%= urlLista %>" class="btn btn-link">Limpiar</a><% } %>
            </div>
        </form>
    </div>

    <div class="card-body p-0">
    <% if (errorDatos || pagina == null) { %>
        <div class="estado-vacio">
            <i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar los proveedores.</strong></p>
            <p class="text-muted">Revisa la conexión con la base de datos y recarga la página.</p>
        </div>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio">
            <i class="fas fa-truck text-muted"></i>
            <% if (filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>Ningún proveedor coincide con la búsqueda.</strong></p><a href="<%= urlLista %>">Quitar filtros</a>
            <% } else { %>
                <p class="mb-1"><strong>Aún no hay proveedores.</strong></p><a href="<%= urlLista %>?accion=nuevo">Registrar el primero</a>
            <% } %>
        </div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Proveedor</th>
                    <th scope="col" class="d-none d-sm-table-cell">RUC</th>
                    <th scope="col" class="d-none d-md-table-cell">Rubro</th>
                    <th scope="col" class="d-none d-lg-table-cell">Contacto</th>
                    <th scope="col" class="d-none d-md-table-cell text-center">Ítems</th>
                    <th scope="col">Estado</th>
                    <th scope="col" class="text-right">Acciones</th>
                </tr>
            </thead>
            <tbody>
            <% for (ProveedorDTO p : pagina.getElementos()) {
                   String nombre = EscapeHtml.texto(p.getNombreMostrar()); %>
                <tr class="<%= p.isActivo() ? "" : "fila-inactiva" %>">
                    <td>
                        <div class="font-weight-bold"><%= nombre %></div>
                        <small class="text-muted"><%= EscapeHtml.texto(p.getRazonSocial()) %></small>
                    </td>
                    <td class="d-none d-sm-table-cell"><%= EscapeHtml.texto(p.getRuc()) %></td>
                    <td class="d-none d-md-table-cell"><span class="badge badge-light border"><%= EscapeHtml.texto(p.getRubro()) %></span></td>
                    <td class="d-none d-lg-table-cell small">
                        <% if (!p.getContactoNombre().isEmpty()) { %><div><i class="fas fa-user text-muted mr-1"></i><%= EscapeHtml.texto(p.getContactoNombre()) %></div><% } %>
                        <% if (!p.getTelefono().isEmpty()) { %><div><i class="fas fa-phone text-muted mr-1"></i><%= EscapeHtml.texto(p.getTelefono()) %></div><% } %>
                        <% if (!p.getCorreo().isEmpty()) { %><div><i class="fas fa-envelope text-muted mr-1"></i><%= EscapeHtml.texto(p.getCorreo()) %></div><% } %>
                    </td>
                    <td class="d-none d-md-table-cell text-center">
                        <span class="badge badge-pill badge-light border" title="Servicios y productos activos que provee"><%= p.getProductos() %></span>
                    </td>
                    <td><% if (p.isActivo()) { %><span class="badge badge-success"><i class="fas fa-check mr-1"></i>Activo</span>
                        <% } else { %><span class="badge badge-secondary"><i class="fas fa-ban mr-1"></i>Desactivado</span><% } %></td>
                    <td class="text-right text-nowrap">
                        <a href="<%= urlLista %>?accion=editar&id=<%= p.getIdProveedor() %>" class="btn btn-sm btn-outline-primary" title="Editar" aria-label="Editar <%= nombre %>"><i class="fas fa-pen"></i></a>
                        <% if (p.isActivo()) { %>
                        <button type="button" class="btn btn-sm btn-outline-danger" title="Desactivar" aria-label="Desactivar <%= nombre %>"
                                data-confirmar="desactivar" data-id="<%= p.getIdProveedor() %>" data-nombre="<%= nombre %>" data-color="btn-danger"
                                data-titulo="Desactivar proveedor" data-boton="Sí, desactivar"
                                data-mensaje="¿Desactivar a {usuario}? Ya no aparecerá al elegir proveedor<%= p.getProductos() > 0 ? " (tiene " + p.getProductos() + " ítems asociados, que no se modifican)" : "" %>.">
                            <i class="fas fa-ban"></i></button>
                        <% } else { %>
                        <button type="button" class="btn btn-sm btn-outline-success" title="Reactivar" aria-label="Reactivar <%= nombre %>"
                                data-confirmar="reactivar" data-id="<%= p.getIdProveedor() %>" data-nombre="<%= nombre %>" data-color="btn-success"
                                data-titulo="Reactivar proveedor" data-boton="Sí, reactivar" data-mensaje="¿Reactivar a {usuario}?">
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
    <%@ include file="/WEB-INF/vistas/comun/paginacion.jspf" %>
</div>

<div id="datosConfirmar" data-action="<%= urlLista %>" data-regreso="<%= EscapeHtml.texto(regreso) %>" hidden></div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
