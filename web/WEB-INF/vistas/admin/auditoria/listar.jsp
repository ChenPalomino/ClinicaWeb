<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.net.URLEncoder, modelo.dto.AuditoriaDTO, modelo.dto.PaginaDTO, modelo.dto.FiltroAuditoriaDTO, controlador.FormatoVista" %>
<%-- AUDITORÍA (solo lectura): quién hizo qué y cuándo. RF-21, RNF-10, módulo 9 del curso. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%! private static final java.util.Set<String> ALERTAS = java.util.Set.of(
        "USUARIO_BLOQUEADO", "CUENTA_BLOQUEADA_INTENTOS", "INICIO_FALLIDO", "SESION_EXPULSADA", "PAGO_RECHAZADO",
        "CUENTA_ANULADA"); %>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<AuditoriaDTO> pagina = (PaginaDTO<AuditoriaDTO>) request.getAttribute("pagina");
    FiltroAuditoriaDTO filtro = (FiltroAuditoriaDTO) request.getAttribute("filtro");
    boolean errorDatos = request.getAttribute("errorDatos") != null;

    String urlLista = ctx + "/admin/auditoria";
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    String entidadSel = filtro == null || filtro.getEntidad() == null ? "" : filtro.getEntidad();
    String desde = filtro == null || filtro.getDesde() == null ? "" : filtro.getDesde().toString();
    String hasta = filtro == null || filtro.getHasta() == null ? "" : filtro.getHasta().toString();
    Integer idSel = filtro == null ? null : filtro.getIdEntidad();
    String base = "q=" + URLEncoder.encode(q, "UTF-8") + (entidadSel.isEmpty() ? "" : "&entidad=" + entidadSel)
                + (desde.isEmpty() ? "" : "&desde=" + desde) + (hasta.isEmpty() ? "" : "&hasta=" + hasta)
                + (idSel == null ? "" : "&id=" + idSel);
    String[][] entidades = {{"SESION", "Inicios de sesión"}, {"USUARIO", "Usuarios"}, {"PACIENTE", "Pacientes"},
                            {"PROVEEDOR", "Proveedores"}, {"TARIFARIO", "Tarifario"}, {"COBRANZA", "Caja y cobranza"},
                            {"CITA", "Citas"}, {"MEDICO", "Médicos y horarios"}, {"ESPECIALIDAD", "Especialidades"},
                            {"CONFIGURACION", "Datos de la clínica"}};
    boolean ultimos30 = Boolean.TRUE.equals(request.getAttribute("ultimos30"));
    if (ultimos30) {
        desde = "";
        base = "q=" + (entidadSel.isEmpty() ? "" : "&entidad=" + entidadSel);
    }
    String queLista = "Los movimientos";
    String hoyIso = java.time.LocalDate.now().toString();
    String[][] atajos = {
        {"Todo lo de hoy", "desde=" + hoyIso + "&hasta=" + hoyIso, "fas fa-calendar-day"},
        {"Inicios de sesión de hoy", "entidad=SESION&desde=" + hoyIso + "&hasta=" + hoyIso, "fas fa-sign-in-alt"},
        {"Caja de hoy", "entidad=COBRANZA&desde=" + hoyIso + "&hasta=" + hoyIso, "fas fa-cash-register"},
        {"Citas de la semana", "entidad=CITA&desde=" + java.time.LocalDate.now().minusDays(6) + "&hasta=" + hoyIso, "fas fa-calendar-check"},
        {"Usuarios (30 días)", "entidad=USUARIO", "fas fa-users-cog"}
    };
%>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-clipboard-list mr-2"></i>Registro de auditoría
            <% if (pagina != null) { %><span class="badge badge-light border ml-1"><%= pagina.getTotal() %></span><% } %></h3>
        <small class="text-muted"><i class="fas fa-lock mr-1"></i>Solo lectura: los registros no se pueden modificar ni borrar.</small>
    </div>
    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-4 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <input type="search" id="q" name="q" class="form-control" maxlength="60" placeholder="Usuario o detalle (ej. DNI, nombre)" value="<%= EscapeHtml.texto(q) %>">
            </div>
            <div class="col-6 col-md-2 mb-2">
                <label for="entidad" class="small text-muted mb-1">Sobre</label>
                <select id="entidad" name="entidad" class="form-control">
                    <option value="">Todo</option>
                    <% for (String[] e : entidades) { %><option value="<%= e[0] %>" <%= e[0].equals(entidadSel) ? "selected" : "" %>><%= e[1] %></option><% } %>
                </select>
            </div>
            <div class="col-6 col-md-2 mb-2">
                <label for="desde" class="small text-muted mb-1">Desde</label>
                <input type="date" id="desde" name="desde" class="form-control" value="<%= desde %>">
            </div>
            <div class="col-6 col-md-2 mb-2">
                <label for="hasta" class="small text-muted mb-1">Hasta</label>
                <input type="date" id="hasta" name="hasta" class="form-control" value="<%= hasta %>">
            </div>
            <div class="col-6 col-md-2 mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Filtrar</button>
                <% if (filtro != null) { %><a href="<%= urlLista %>" class="btn btn-link px-1">Limpiar</a><% } %>
            </div>
        </form>
        <% if (ultimos30) { %>
        <p class="small text-muted mb-0 aviso-rango"><i class="far fa-calendar-alt mr-1"></i>Se muestran los últimos <%= modelo.dto.RangoFechasDTO.DIAS_POR_DEFECTO %> días. Para ver anteriores, elige las fechas o busca un texto.</p>
        <% } %>
    </div>
    <div class="card-body p-0">
    <% if (errorDatos) { %>
        <div class="estado-vacio"><i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar la auditoría.</strong></p>
            <p class="text-muted">¿Ejecutaste el script sql/08? Revisa la conexión y recarga.</p></div>
    <% } else if (pagina == null) { %>
        <%@ include file="/WEB-INF/vistas/comun/inicio-filtros.jspf" %>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio"><i class="fas fa-clipboard-check text-muted"></i>
            <p class="mb-1"><strong>No hay movimientos <%= filtro.hayFiltros() ? "con esos filtros" : "registrados todavía" %>.</strong></p></div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-sm table-hover mb-0 tabla-auditoria">
            <thead class="thead-light">
                <tr><th scope="col">Fecha y hora</th><th scope="col">Usuario</th><th scope="col">Acción</th>
                    <th scope="col" class="d-none d-md-table-cell">Detalle</th><th scope="col" class="d-none d-lg-table-cell">IP</th></tr>
            </thead>
            <tbody>
            <% for (AuditoriaDTO a : pagina.getElementos()) {
                   boolean alerta = ALERTAS.contains(a.getAccion()) || a.getAccion().endsWith("DESACTIVADO"); %>
                <tr>
                    <td class="text-nowrap small"><%= FormatoVista.fechaHora(a.getFecha()) %></td>
                    <td>@<%= EscapeHtml.texto(a.getUsuario()) %></td>
                    <td><span class="badge <%= alerta ? "badge-warning" : "badge-light border" %>"><%= EscapeHtml.texto(a.getDescripcion()) %></span>
                        <div class="d-md-none small text-muted"><%= EscapeHtml.texto(a.getDetalle()) %></div></td>
                    <td class="d-none d-md-table-cell small text-break"><%= EscapeHtml.texto(a.getDetalle().isEmpty() ? "—" : a.getDetalle()) %></td>
                    <td class="d-none d-lg-table-cell small text-muted"><%= EscapeHtml.texto(a.getDireccionIp()) %></td>
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
