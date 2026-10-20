<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.UsuarioDTO, modelo.dto.PaginaDTO, modelo.dto.RangoFechasDTO, modelo.dto.FiltroUsuarioDTO, modelo.entidades.Rol, controlador.FormatoVista" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<UsuarioDTO> pagina = (PaginaDTO<UsuarioDTO>) request.getAttribute("pagina");
    FiltroUsuarioDTO filtro = (FiltroUsuarioDTO) request.getAttribute("filtro");
    @SuppressWarnings("unchecked")
    List<Rol> roles = (List<Rol>) request.getAttribute("roles");
    boolean errorDatos = request.getAttribute("errorDatos") != null;

    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    Integer rolSel = filtro == null ? null : filtro.getIdRol();
    String estadoSel = filtro == null || filtro.getEstado() == null ? "" : filtro.getEstado();

    // Parámetros actuales, para volver a la misma búsqueda después de una acción o al cambiar de página
    String base = "q=" + URLEncoder.encode(q, "UTF-8")
                + (rolSel != null ? "&rol=" + rolSel : "")
                + (filtro != null && filtro.isSoloPersonal() ? "&rol=personal" : "")
                + (estadoSel.isEmpty() ? "" : "&estado=" + estadoSel);
    RangoFechasDTO rangoFechas = filtro == null ? RangoFechasDTO.SIN_RANGO : filtro.getRango();
    base += rangoFechas.parametrosUrl();   // conserva las fechas elegidas al paginar o volver
    String regreso = base + (pagina != null ? "&pagina=" + pagina.getPagina() : "");
%>

<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto mb-2 mb-sm-0">
            <i class="fas fa-users-cog mr-2"></i>Usuarios del sistema
            <% if (pagina != null) { %><span class="badge badge-light border ml-1"><%= pagina.getTotal() %></span><% } %>
        </h3>
        <a href="<%= ctx %>/admin/usuarios?accion=nuevo" class="btn btn-primary btn-sm">
            <i class="fas fa-user-plus mr-1"></i> Nuevo usuario
        </a>
    </div>

    <%-- ---------- Búsqueda y filtros ---------- --%>
    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= ctx %>/admin/usuarios" class="form-row align-items-end" role="search">
            <div class="col-md-12 col-lg-4 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60"
                           placeholder="Nombre, usuario, DNI o correo" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-3 col-lg-2 mb-2">
                <label for="rol" class="small text-muted mb-1">Rol</label>
                <select id="rol" name="rol" class="form-control">
                    <option value="">Todos</option>
                    <option value="personal" <%= filtro != null && filtro.isSoloPersonal() ? "selected" : "" %>>Todo el personal (sin pacientes)</option>
                    <% if (roles != null) for (Rol r : roles) { %>
                    <option value="<%= r.getIdRol() %>" <%= rolSel != null && rolSel == r.getIdRol() ? "selected" : "" %>><%= EscapeHtml.texto(r.getNombre()) %></option>
                    <% } %>
                </select>
            </div>
            <div class="col-6 col-md-3 col-lg-2 mb-2">
                <label for="estado" class="small text-muted mb-1">Estado</label>
                <select id="estado" name="estado" class="form-control">
                    <option value="">Todos</option>
                    <option value="ACTIVO" <%= "ACTIVO".equals(estadoSel) ? "selected" : "" %>>Activos</option>
                    <option value="BLOQUEADO" <%= "BLOQUEADO".equals(estadoSel) ? "selected" : "" %>>Bloqueados</option>
                    <option value="INACTIVO" <%= "INACTIVO".equals(estadoSel) ? "selected" : "" %>>Desactivados</option>
                </select>
            </div>
            <%@ include file="/WEB-INF/vistas/comun/rango-fechas.jspf" %>
            <div class="col-md-12 col-lg mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Filtrar</button>
                <% if (filtro != null && filtro.hayFiltros()) { %>
                <a href="<%= ctx %>/admin/usuarios" class="btn btn-link">Limpiar</a>
                <% } %>
            </div>
        </form>
        <%@ include file="/WEB-INF/vistas/comun/aviso-rango.jspf" %>
    </div>

    <%-- ---------- Tabla ---------- --%>
    <div class="card-body p-0">
    <% if (errorDatos || pagina == null) { %>
        <div class="estado-vacio">
            <i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar los usuarios.</strong></p>
            <p class="text-muted">Revisa la conexión con la base de datos y recarga la página.</p>
        </div>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio">
            <i class="fas fa-user-slash text-muted"></i>
            <% if (rangoFechas.isPorDefecto() && !filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>No hay registros en los últimos <%= RangoFechasDTO.DIAS_POR_DEFECTO %> días.</strong></p>
                <p class="text-muted mb-0">Busca por nombre o DNI, o elige otras fechas arriba.</p>
            <% } else if (filtro.hayFiltros()) { %>
                <p class="mb-1"><strong>Ningún usuario coincide con la búsqueda.</strong></p>
                <a href="<%= ctx %>/admin/usuarios">Quitar filtros</a>
            <% } else { %>
                <p class="mb-1"><strong>Aún no hay usuarios.</strong></p>
                <a href="<%= ctx %>/admin/usuarios?accion=nuevo">Crear el primero</a>
            <% } %>
        </div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover table-usuarios mb-0">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Usuario</th>
                    <th scope="col" class="d-none d-sm-table-cell">DNI</th>
                    <th scope="col" class="d-none d-lg-table-cell">Correo</th>
                    <th scope="col" class="d-none d-sm-table-cell">Rol</th>
                    <th scope="col">Estado</th>
                    <th scope="col" class="d-none d-md-table-cell">Último acceso</th>
                    <th scope="col" class="text-right">Acciones</th>
                </tr>
            </thead>
            <tbody>
            <% for (UsuarioDTO u : pagina.getElementos()) {
                   boolean esYo = u.getIdUsuario() == usuarioSesion.getIdUsuario();
                   String etiqueta = EscapeHtml.texto(u.getNombreUsuario()); %>
                <tr class="<%= u.isActivo() ? "" : "fila-inactiva" %>">
                    <td>
                        <div class="d-flex align-items-center">
                            <%= avatar(ctx, u.getIdUsuario(), u.isTieneFoto(), u.getFotoVersion(), u.getIniciales(), "avatar-tabla mr-2") %>
                            <div>
                                <div class="font-weight-bold"><%= EscapeHtml.texto(u.getNombreCompleto()) %>
                                    <% if (esYo) { %><span class="badge badge-warning ml-1">Tú</span><% } %></div>
                                <small class="text-muted">@<%= etiqueta %></small>
                                <span class="badge <%= FormatoVista.colorRol(u.getIdRol()) %> d-sm-none ml-1"><%= EscapeHtml.texto(u.getNombreRol()) %></span>
                            </div>
                        </div>
                    </td>
                    <td class="d-none d-sm-table-cell"><%= EscapeHtml.texto(u.getDni().isEmpty() ? "—" : u.getDni()) %></td>
                    <td class="d-none d-lg-table-cell"><%= EscapeHtml.texto(u.getCorreo().isEmpty() ? "—" : u.getCorreo()) %></td>
                    <td class="d-none d-sm-table-cell"><span class="badge <%= FormatoVista.colorRol(u.getIdRol()) %>"><%= EscapeHtml.texto(u.getNombreRol()) %></span></td>
                    <td>
                        <% if (!u.isActivo()) { %>
                            <span class="badge badge-secondary"><i class="fas fa-ban mr-1"></i>Desactivado</span>
                        <% } else if (u.isBloqueado()) { %>
                            <span class="badge badge-danger" data-toggle="tooltip" title="<%= EscapeHtml.texto(FormatoVista.motivoBloqueo(u.getMotivoBloqueo())) %>">
                                <i class="fas fa-lock mr-1"></i>Bloqueado</span>
                        <% } else { %>
                            <span class="badge badge-success"><i class="fas fa-check mr-1"></i>Activo</span>
                        <% } %>
                    </td>
                    <td class="d-none d-md-table-cell small"><%= FormatoVista.fechaHora(u.getUltimoAcceso()) %></td>
                    <td class="text-right text-nowrap">
                        <a href="<%= ctx %>/admin/usuarios?accion=editar&id=<%= u.getIdUsuario() %>" class="btn btn-sm btn-outline-primary"
                           title="Editar" aria-label="Editar a <%= etiqueta %>"><i class="fas fa-pen"></i></a>
                        <% if (u.isBloqueado()) { %>
                        <button type="button" class="btn btn-sm btn-outline-success" title="Desbloquear" aria-label="Desbloquear a <%= etiqueta %>"
                                data-confirmar="desbloquear" data-id="<%= u.getIdUsuario() %>" data-usuario="<%= etiqueta %>" data-color="btn-success"
                                data-titulo="Desbloquear cuenta" data-boton="Sí, desbloquear"
                                data-mensaje="¿Desbloquear la cuenta de {usuario}? Podrá iniciar sesión de nuevo y sus intentos fallidos volverán a cero.">
                            <i class="fas fa-unlock"></i></button>
                        <% } else if (u.isActivo() && !esYo) { %>
                        <button type="button" class="btn btn-sm btn-outline-warning" title="Bloquear" aria-label="Bloquear a <%= etiqueta %>"
                                data-confirmar="bloquear" data-id="<%= u.getIdUsuario() %>" data-usuario="<%= etiqueta %>" data-color="btn-warning"
                                data-titulo="Bloquear cuenta" data-boton="Sí, bloquear"
                                data-mensaje="¿Bloquear la cuenta de {usuario}? No podrá iniciar sesión hasta que un administrador la desbloquee.">
                            <i class="fas fa-lock"></i></button>
                        <% } %>
                        <% if (u.isActivo() && !esYo) { %>
                        <button type="button" class="btn btn-sm btn-outline-danger" title="Desactivar" aria-label="Desactivar a <%= etiqueta %>"
                                data-confirmar="desactivar" data-id="<%= u.getIdUsuario() %>" data-usuario="<%= etiqueta %>" data-color="btn-danger"
                                data-titulo="Desactivar usuario" data-boton="Sí, desactivar"
                                data-mensaje="¿Desactivar a {usuario}? No se borra: su información se conserva y podrás reactivarlo cuando quieras.">
                            <i class="fas fa-user-slash"></i></button>
                        <% } else if (!u.isActivo()) { %>
                        <button type="button" class="btn btn-sm btn-outline-success" title="Reactivar" aria-label="Reactivar a <%= etiqueta %>"
                                data-confirmar="reactivar" data-id="<%= u.getIdUsuario() %>" data-usuario="<%= etiqueta %>" data-color="btn-success"
                                data-titulo="Reactivar usuario" data-boton="Sí, reactivar"
                                data-mensaje="¿Reactivar a {usuario}? Podrá volver a iniciar sesión.">
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

    <%-- ---------- Paginación ---------- --%>
    <% if (pagina != null && pagina.getTotal() > 0) { %>
    <div class="card-footer d-flex flex-wrap align-items-center">
        <small class="text-muted mr-auto">Mostrando <%= pagina.getDesde() %>–<%= pagina.getHasta() %> de <%= pagina.getTotal() %></small>
        <% if (pagina.getTotalPaginas() > 1) { %>
        <nav aria-label="Páginas">
            <ul class="pagination pagination-sm m-0">
                <li class="page-item <%= pagina.getPagina() <= 1 ? "disabled" : "" %>">
                    <a class="page-link" href="<%= ctx %>/admin/usuarios?<%= base %>&pagina=<%= pagina.getPagina() - 1 %>" aria-label="Anterior">&laquo;</a>
                </li>
                <% for (int i = 1; i <= pagina.getTotalPaginas(); i++) { %>
                <li class="page-item <%= i == pagina.getPagina() ? "active" : "" %>">
                    <a class="page-link" href="<%= ctx %>/admin/usuarios?<%= base %>&pagina=<%= i %>"><%= i %></a>
                </li>
                <% } %>
                <li class="page-item <%= pagina.getPagina() >= pagina.getTotalPaginas() ? "disabled" : "" %>">
                    <a class="page-link" href="<%= ctx %>/admin/usuarios?<%= base %>&pagina=<%= pagina.getPagina() + 1 %>" aria-label="Siguiente">&raquo;</a>
                </li>
            </ul>
        </nav>
        <% } %>
    </div>
    <% } %>
</div>

<%-- Datos para la ventana de confirmación (pie.jspf) --%>
<div id="datosConfirmar" data-action="<%= ctx %>/admin/usuarios" data-regreso="<%= EscapeHtml.texto(regreso) %>" hidden></div>

<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
