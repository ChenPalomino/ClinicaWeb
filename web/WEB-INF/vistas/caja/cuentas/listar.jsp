<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.ReportePagoDTO, modelo.dto.CuentaAtencionDTO, modelo.dto.PaginaDTO, modelo.dto.RangoFechasDTO, modelo.dto.FiltroBusquedaDTO, controlador.FormatoVista" %>
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
    base += rangoFechas.parametrosUrl();
    boolean soloPorCobrar = Boolean.TRUE.equals(request.getAttribute("soloPorCobrar"));
    @SuppressWarnings("unchecked") List<ReportePagoDTO> reportes = (List<ReportePagoDTO>) request.getAttribute("reportes");
    if (soloPorCobrar) { base = "estado=PENDIENTE"; }
%>

<% if (reportes != null && !reportes.isEmpty()) { %>
<div class="card card-outline card-info" id="pagos-informados">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-mobile-alt mr-2"></i>Pagos informados desde el portal
        <span class="badge badge-info ml-1"><%= reportes.size() %></span></h3></div>
    <div class="card-body p-0">
        <ul class="list-group list-group-flush">
        <% for (ReportePagoDTO r : reportes) { %>
            <li class="list-group-item reporte-pago">
                <div class="d-flex flex-wrap align-items-start" style="gap:.75rem">
                    <div class="flex-grow-1" style="min-width:220px">
                        <strong><%= EscapeHtml.texto(r.getNombrePaciente()) %></strong>
                        <span class="text-muted small">· DNI <%= EscapeHtml.texto(r.getDniPaciente()) %> · <a href="<%= urlLista %>?accion=ver&id=<%= r.getIdCuentaAtencion() %>"><%= EscapeHtml.texto(r.getNumeroCuenta()) %></a></span>
                        <div class="mt-1"><span class="badge badge-light border"><%= EscapeHtml.texto(r.getNombreBilletera()) %></span>
                            <% if (!r.getNumeroOperacion().isEmpty()) { %><span class="text-monospace ml-1">Op. <%= EscapeHtml.texto(r.getNumeroOperacion()) %></span><% } %>
                            <% if (!r.getCelular().isEmpty()) { %><span class="ml-1">Cel. <%= EscapeHtml.texto(r.getCelular()) %></span><% } %>
                            <% if (!r.getNombrePagador().isEmpty()) { %><span class="ml-1 text-muted">a nombre de <%= EscapeHtml.texto(r.getNombrePagador()) %></span><% } %></div>
                        <small class="text-muted">Informado <%= FormatoVista.fechaHora(r.getFechaCreacion()) %></small>
                    </div>
                    <div class="text-right">
                        <div class="h5 mb-0 monto">S/ <%= FormatoVista.soles(r.getMonto()) %></div>
                        <% if (!r.isMontoCoincide()) { %><small class="text-danger d-block">La cuenta suma S/ <%= FormatoVista.soles(r.getTotalCuenta()) %></small><% } %>
                    </div>
                </div>
                <div class="d-flex flex-wrap mt-2" style="gap:.5rem">
                    <form method="POST" action="<%= urlLista %>" data-un-envio>
                        <input type="hidden" name="accion" value="verificarReporte"><input type="hidden" name="idReporte" value="<%= r.getIdReportePago() %>">
                        <button type="submit" class="btn btn-success btn-sm" <%= r.isMontoCoincide() ? "" : "disabled" %>><i class="fas fa-check mr-1"></i>Lo vi en la app: confirmar y emitir boleta</button>
                    </form>
                    <details class="rechazo-reporte">
                        <summary class="btn btn-outline-danger btn-sm">No aparece el pago</summary>
                        <form method="POST" action="<%= urlLista %>" class="form-inline mt-2" data-un-envio>
                            <input type="hidden" name="accion" value="rechazarReporte"><input type="hidden" name="idReporte" value="<%= r.getIdReportePago() %>">
                            <label class="sr-only" for="motivo<%= r.getIdReportePago() %>">Motivo</label>
                            <input type="text" id="motivo<%= r.getIdReportePago() %>" name="motivo" class="form-control form-control-sm mr-2" minlength="5" maxlength="200" required
                                   value="No encontramos el pago en la app de la clínica">
                            <button type="submit" class="btn btn-danger btn-sm">Rechazar aviso</button>
                        </form>
                    </details>
                </div>
            </li>
        <% } %>
        </ul>
    </div>
    <div class="card-footer small text-muted"><i class="fas fa-info-circle mr-1"></i>Revisa el pago en el celular de la clínica (monto, hora y nombre) antes de confirmar.</div>
</div>
<% } %>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0">
            <i class="fas fa-cash-register mr-2"></i><%= soloPorCobrar ? "Por cobrar" : "Cuentas de atención" %>
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
                <% if (!soloPorCobrar) { %><a href="<%= urlLista %>" class="btn btn-link">Volver a «Por cobrar»</a><% } %>
            </div>
        </form>
        <% if (soloPorCobrar) { %>
        <p class="small text-muted mb-1 aviso-rango"><i class="fas fa-info-circle mr-1"></i>Se muestran solo las cuentas por cobrar.
            Para ver pagadas o anuladas, busca o usa: <a href="<%= urlLista %>?estado=PAGADA&desde=<%= java.time.LocalDate.now() %>&hasta=<%= java.time.LocalDate.now() %>">pagadas hoy</a>
            · <a href="<%= urlLista %>?estado=PAGADA">pagadas (30 días)</a> · <a href="<%= urlLista %>?estado=ANULADA">anuladas</a>.</p>
        <% } else { %>
        <% request.setAttribute("notaRango", "Las cuentas por cobrar se muestran siempre, aunque sean antiguas."); %>
        <%@ include file="/WEB-INF/vistas/comun/aviso-rango.jspf" %>
        <% } %>
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
            <% if (soloPorCobrar) { %>
                <p class="mb-1"><strong>No hay cuentas por cobrar.</strong></p>
                <p class="text-muted mb-0">Cuando recepción pulse «Cobrar» en una cita, o abras una cuenta nueva, aparecerá aquí.</p>
            <% } else if (rangoFechas.isPorDefecto() && !filtro.hayFiltros()) { %>
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
