<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.net.URLEncoder, modelo.dto.CuentaAtencionDTO, modelo.dto.PaginaDTO, modelo.dto.RangoFechasDTO, modelo.dto.FiltroBusquedaDTO, controlador.FormatoVista" %>
<%-- COBROS: listado de cuentas de atención (RF-11). Las pendientes de cobro salen primero. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    private String etiquetaEstado(CuentaAtencionDTO c) {
        if (c.isPendiente()) return "<span class=\"badge badge-warning\"><i class=\"fas fa-hourglass-half mr-1\"></i>Por cobrar</span>";
        if (c.isPagada()) return "<span class=\"badge badge-success\"><i class=\"fas fa-check mr-1\"></i>Pagada</span>";
        return "<span class=\"badge badge-secondary\"><i class=\"fas fa-ban mr-1\"></i>Anulada</span>";
    }
%>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<CuentaAtencionDTO> pagina = (PaginaDTO<CuentaAtencionDTO>) request.getAttribute("pagina");
    FiltroBusquedaDTO filtro = (FiltroBusquedaDTO) request.getAttribute("filtro");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    Integer pendientes = (Integer) request.getAttribute("pendientes");
    String urlLista = ctx + "/caja/cuentas";
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    String estadoSel = filtro == null || filtro.getEstado() == null ? "" : filtro.getEstado();
    String base = "q=" + URLEncoder.encode(q, "UTF-8") + (estadoSel.isEmpty() ? "" : "&estado=" + estadoSel);
    RangoFechasDTO rangoFechas = filtro == null ? RangoFechasDTO.SIN_RANGO : filtro.getRango();
    base += rangoFechas.parametrosUrl();   // conserva las fechas elegidas al paginar o volver
%>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0">
            <i class="fas fa-cash-register mr-2"></i>Cuentas de atención
            <% if (pendientes != null && pendientes > 0) { %>
            <a href="<%= urlLista %>?estado=PENDIENTE" class="badge badge-warning ml-1" title="Ver solo las que faltan cobrar"><%= pendientes %> por cobrar</a>
            <% } %>
        </h3>
        <a href="<%= ctx %>/caja/cierre" class="btn btn-outline-secondary btn-sm mr-2"><i class="fas fa-lock mr-1"></i> Cierre de caja</a>
        <a href="<%= urlLista %>?accion=nueva" class="btn btn-primary btn-sm"><i class="fas fa-plus mr-1"></i> Nueva cuenta</a>
    </div>

    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-12 col-lg-4 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" autofocus
                           placeholder="Paciente, DNI, N.º de cuenta (CA-…) o de comprobante (B001-…)" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-3 col-lg-2 mb-2">
                <label for="estado" class="small text-muted mb-1">Estado</label>
                <select id="estado" name="estado" class="form-control">
                    <option value="">Todas</option>
                    <option value="PENDIENTE" <%= "PENDIENTE".equals(estadoSel) ? "selected" : "" %>>Por cobrar</option>
                    <option value="PAGADA" <%= "PAGADA".equals(estadoSel) ? "selected" : "" %>>Pagadas</option>
                    <option value="ANULADA" <%= "ANULADA".equals(estadoSel) ? "selected" : "" %>>Anuladas</option>
                </select>
            </div>
            <%@ include file="/WEB-INF/vistas/comun/rango-fechas.jspf" %>
            <div class="col-md-12 col-lg mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Buscar</button>
                <% if (filtro != null && filtro.hayFiltros()) { %><a href="<%= urlLista %>" class="btn btn-link">Limpiar</a><% } %>
            </div>
        </form>
        <% request.setAttribute("notaRango", "Las cuentas por cobrar se muestran siempre, aunque sean antiguas."); %>
        <%@ include file="/WEB-INF/vistas/comun/aviso-rango.jspf" %>
    </div>

    <div class="card-body p-0">
    <% if (errorDatos || pagina == null) { %>
        <div class="estado-vacio">
            <i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar las cuentas.</strong></p>
            <p class="text-muted">Revisa la conexión con la base de datos y recarga la página.</p>
        </div>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio">
            <i class="fas fa-receipt text-muted"></i>
            <% if (rangoFechas.isPorDefecto() && !filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>No hay registros en los últimos <%= RangoFechasDTO.DIAS_POR_DEFECTO %> días.</strong></p>
                <p class="text-muted mb-0">Busca por nombre o DNI, o elige otras fechas arriba.</p>
            <% } else if (filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>Ninguna cuenta coincide con la búsqueda.</strong></p>
                <a href="<%= urlLista %>">Quitar filtros</a>
            <% } else { %>
                <p class="mb-1"><strong>Aún no hay cuentas de atención.</strong></p>
                <a href="<%= urlLista %>?accion=nueva">Abrir la primera</a>
            <% } %>
        </div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0 tabla-cuentas">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Cuenta</th>
                    <th scope="col">Paciente</th>
                    <th scope="col" class="d-none d-md-table-cell">Cobertura</th>
                    <th scope="col" class="text-right">Total</th>
                    <th scope="col">Estado</th>
                    <th scope="col" class="d-none d-lg-table-cell">Comprobante</th>
                    <th scope="col" class="text-right"><span class="sr-only">Acciones</span></th>
                </tr>
            </thead>
            <tbody>
            <% for (CuentaAtencionDTO c : pagina.getElementos()) { %>
                <tr class="<%= c.isAnulada() ? "fila-inactiva" : "" %>">
                    <td>
                        <a href="<%= urlLista %>?accion=ver&id=<%= c.getIdCuentaAtencion() %>" class="font-weight-bold"><%= EscapeHtml.texto(c.getNumeroCuenta()) %></a>
                        <div><small class="text-muted"><%= FormatoVista.fechaHora(c.getFechaCreacion()) %> · <%= c.getCantidadItems() %> <%= c.getCantidadItems() == 1 ? "ítem" : "ítems" %></small></div>
                    </td>
                    <td>
                        <div><%= EscapeHtml.texto(c.getNombrePaciente()) %></div>
                        <small class="text-muted">DNI <%= EscapeHtml.texto(c.getDniPaciente()) %></small>
                    </td>
                    <td class="d-none d-md-table-cell"><span class="badge <%= "PARTICULAR".equals(c.getTipoCobertura()) ? "badge-light border" : "badge-info" %>"><%= EscapeHtml.texto(c.getNombreCobertura()) %></span></td>
                    <td class="text-right monto">S/ <%= FormatoVista.soles(c.getTotal()) %></td>
                    <td><%= etiquetaEstado(c) %></td>
                    <td class="d-none d-lg-table-cell">
                        <% if (c.getComprobante() != null) { %>
                        <a href="<%= ctx %>/caja/comprobante?id=<%= c.getIdComprobante() %>" class="text-monospace"><%= c.getComprobante() %></a>
                        <% } else { %><span class="text-muted">—</span><% } %>
                    </td>
                    <td class="text-right text-nowrap">
                        <a href="<%= urlLista %>?accion=ver&id=<%= c.getIdCuentaAtencion() %>" class="btn btn-sm <%= c.isPendiente() ? "btn-primary" : "btn-outline-secondary" %>">
                            <%= c.isPendiente() ? "<i class=\"fas fa-hand-holding-usd mr-1\"></i>Cobrar" : "Ver" %></a>
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
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
