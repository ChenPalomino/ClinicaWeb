<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.ProductoServicioDTO, modelo.dto.PaginaDTO, modelo.dto.FiltroBusquedaDTO, modelo.dto.OpcionDTO, controlador.FormatoVista" %>
<%-- TARIFARIO: servicios médicos y productos con stock (solo Administrador). --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<ProductoServicioDTO> pagina = (PaginaDTO<ProductoServicioDTO>) request.getAttribute("pagina");
    FiltroBusquedaDTO filtro = (FiltroBusquedaDTO) request.getAttribute("filtro");
    @SuppressWarnings("unchecked")
    List<OpcionDTO> categorias = (List<OpcionDTO>) request.getAttribute("categorias");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    Integer stockBajo = (Integer) request.getAttribute("stockBajo");

    String urlLista = ctx + "/admin/servicios";
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    String tipoSel = filtro == null || filtro.getTipo() == null ? "" : filtro.getTipo();
    Integer categoriaSel = filtro == null ? null : filtro.getGrupo();
    String estadoSel = filtro == null || filtro.getEstado() == null ? "" : filtro.getEstado();
    String base = "q=" + URLEncoder.encode(q, "UTF-8") + (tipoSel.isEmpty() ? "" : "&tipo=" + tipoSel)
                + (categoriaSel != null ? "&categoria=" + categoriaSel : "") + (estadoSel.isEmpty() ? "" : "&estado=" + estadoSel);
    String regreso = base + (pagina != null ? "&pagina=" + pagina.getPagina() : "");
%>

<% if (stockBajo != null && stockBajo > 0) { %>
<div class="alert alert-warning d-flex align-items-center" role="status">
    <i class="fas fa-box-open fa-lg mr-3"></i>
    <div><strong><%= stockBajo %> producto<%= stockBajo == 1 ? "" : "s" %></strong> en o por debajo del stock mínimo. Están marcados en la tabla con <span class="badge badge-warning">Reponer</span>.</div>
</div>
<% } %>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0">
            <i class="fas fa-notes-medical mr-2"></i>Tarifario
            <% if (pagina != null) { %><span class="badge badge-light border ml-1"><%= pagina.getTotal() %></span><% } %>
        </h3>
        <div class="btn-group">
            <a href="<%= urlLista %>?accion=nuevo&tipo=SERVICIO" class="btn btn-primary btn-sm"><i class="fas fa-stethoscope mr-1"></i> Nuevo servicio</a>
            <a href="<%= urlLista %>?accion=nuevo&tipo=PRODUCTO" class="btn btn-outline-primary btn-sm"><i class="fas fa-pills mr-1"></i> Nuevo producto</a>
        </div>
    </div>

    <div class="card-body border-bottom pb-2">
        <%-- Pestañas rápidas por tipo (reconocer antes que recordar) --%>
        <ul class="nav nav-pills mb-3">
            <li class="nav-item"><a class="nav-link <%= tipoSel.isEmpty() ? "active" : "" %>" href="<%= urlLista %>">Todo</a></li>
            <li class="nav-item"><a class="nav-link <%= "SERVICIO".equals(tipoSel) ? "active" : "" %>" href="<%= urlLista %>?tipo=SERVICIO"><i class="fas fa-stethoscope mr-1"></i>Servicios</a></li>
            <li class="nav-item"><a class="nav-link <%= "PRODUCTO".equals(tipoSel) ? "active" : "" %>" href="<%= urlLista %>?tipo=PRODUCTO"><i class="fas fa-pills mr-1"></i>Productos</a></li>
        </ul>
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <input type="hidden" name="tipo" value="<%= tipoSel %>">
            <div class="col-md-5 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" placeholder="Código o nombre" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-3 mb-2">
                <label for="categoria" class="small text-muted mb-1">Categoría</label>
                <select id="categoria" name="categoria" class="form-control">
                    <option value="">Todas</option>
                    <% if (categorias != null) for (OpcionDTO c : categorias) {
                           if (!tipoSel.isEmpty() && !tipoSel.equals(c.getExtra())) continue; %>
                    <option value="<%= c.getId() %>" <%= categoriaSel != null && categoriaSel == c.getId() ? "selected" : "" %>><%= EscapeHtml.texto(c.getNombre()) %></option>
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
            <div class="col-md-2 mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Buscar</button>
            </div>
        </form>
    </div>

    <div class="card-body p-0">
    <% if (errorDatos || pagina == null) { %>
        <div class="estado-vacio">
            <i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar el tarifario.</strong></p>
            <p class="text-muted">Revisa la conexión con la base de datos y recarga la página.</p>
        </div>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio">
            <i class="fas fa-notes-medical text-muted"></i>
            <% if (filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>Nada coincide con la búsqueda.</strong></p><a href="<%= urlLista %>">Quitar filtros</a>
            <% } else { %>
                <p class="mb-1"><strong>El tarifario está vacío.</strong></p><a href="<%= urlLista %>?accion=nuevo&tipo=SERVICIO">Registrar el primer servicio</a>
            <% } %>
        </div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Código</th>
                    <th scope="col">Nombre</th>
                    <th scope="col" class="d-none d-md-table-cell">Categoría</th>
                    <th scope="col" class="text-right">Precio</th>
                    <th scope="col" class="d-none d-sm-table-cell text-right">Stock</th>
                    <th scope="col" class="d-none d-sm-table-cell">Estado</th>
                    <th scope="col" class="text-right">Acciones</th>
                </tr>
            </thead>
            <tbody>
            <% for (ProductoServicioDTO p : pagina.getElementos()) {
                   String nombre = EscapeHtml.texto(p.getNombre()); %>
                <tr class="<%= p.isActivo() ? "" : "fila-inactiva" %>">
                    <td class="text-nowrap"><code><%= EscapeHtml.texto(p.getCodigo()) %></code></td>
                    <td>
                        <div class="font-weight-bold"><i class="fas <%= p.isProducto() ? "fa-pills" : "fa-stethoscope" %> text-muted mr-1" title="<%= p.isProducto() ? "Producto" : "Servicio" %>"></i><%= nombre %></div>
                        <small class="text-muted"><%= EscapeHtml.texto(p.isProducto()
                                ? (p.getNombreMarca().isEmpty() ? "" : p.getNombreMarca() + " · ") + p.getUnidad()
                                : (p.getNombreEspecialidad().isEmpty() ? "Servicio" : p.getNombreEspecialidad())) %></small>
                    </td>
                    <td class="d-none d-md-table-cell"><span class="badge badge-light border"><%= EscapeHtml.texto(p.getNombreCategoria()) %></span></td>
                    <td class="text-right text-nowrap">S/ <%= FormatoVista.soles(p.getPrecio()) %></td>
                    <td class="d-none d-sm-table-cell text-right">
                        <% if (!p.isProducto()) { %><span class="text-muted">—</span>
                        <% } else { %><%= p.getStock() %>
                            <% if (p.isStockBajo()) { %><span class="badge badge-warning ml-1" title="Mínimo: <%= p.getStockMinimo() %>">Reponer</span><% } %>
                        <% } %>
                    </td>
                    <td class="d-none d-sm-table-cell"><% if (p.isActivo()) { %><span class="badge badge-success">Activo</span>
                        <% } else { %><span class="badge badge-secondary">Desactivado</span><% } %></td>
                    <td class="text-right text-nowrap">
                        <a href="<%= urlLista %>?accion=editar&id=<%= p.getIdProductoServicio() %>" class="btn btn-sm btn-outline-primary" title="Editar" aria-label="Editar <%= nombre %>"><i class="fas fa-pen"></i></a>
                        <% if (p.isActivo()) { %>
                        <button type="button" class="btn btn-sm btn-outline-danger" title="Desactivar" aria-label="Desactivar <%= nombre %>"
                                data-confirmar="desactivar" data-id="<%= p.getIdProductoServicio() %>" data-nombre="<%= nombre %>" data-color="btn-danger"
                                data-titulo="Desactivar del tarifario" data-boton="Sí, desactivar"
                                data-mensaje="¿Desactivar {usuario}? Ya no se podrá cobrar en caja; los cobros anteriores no cambian.">
                            <i class="fas fa-ban"></i></button>
                        <% } else { %>
                        <button type="button" class="btn btn-sm btn-outline-success" title="Reactivar" aria-label="Reactivar <%= nombre %>"
                                data-confirmar="reactivar" data-id="<%= p.getIdProductoServicio() %>" data-nombre="<%= nombre %>" data-color="btn-success"
                                data-titulo="Reactivar" data-boton="Sí, reactivar" data-mensaje="¿Reactivar {usuario}?">
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
