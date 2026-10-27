<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.MedicoDTO, modelo.dto.OpcionDTO, modelo.repositorio.MedicoRepository" %>
<%-- MÉDICOS Y HORARIOS (RF-03): cada cuenta de rol Médico y si ya puede recibir citas. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked") List<MedicoDTO> medicos = (List<MedicoDTO>) request.getAttribute("medicos");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    @SuppressWarnings("unchecked") List<OpcionDTO> especialidades = (List<OpcionDTO>) request.getAttribute("especialidades");
    String urlLista = ctx + "/admin/medicos";
    String q = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
    Integer espSel = controlador.ControladorUtil.enteroONulo(request.getParameter("especialidad"));
    String estadoSel = request.getParameter("estado") == null ? "" : request.getParameter("estado");
    String regreso = "q=" + URLEncoder.encode(q, "UTF-8") + (espSel == null ? "" : "&especialidad=" + espSel)
                   + (estadoSel.isEmpty() ? "" : "&estado=" + URLEncoder.encode(estadoSel, "UTF-8"));
    String queLista = "Los médicos";
    String[][] atajos = {
        {"Todos los médicos", "q=", "fas fa-user-md"},
        {"Atienden ahora", "estado=ATIENDE", "fas fa-check-circle"},
        {"Sin configurar", "estado=SIN_CONFIGURAR", "fas fa-exclamation-circle"},
        {"Citas en pausa", "estado=PAUSA", "fas fa-pause-circle"}
    };
%>
<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-user-md mr-2"></i>Médicos y horarios
            <% if (medicos != null) { %><span class="badge badge-light border ml-1"><%= medicos.size() %></span><% } %></h3>
        <a href="<%= ctx %>/admin/usuarios?accion=nuevo" class="btn btn-outline-primary btn-sm"><i class="fas fa-user-plus mr-1"></i>Crear cuenta de médico</a>
    </div>
    <div class="card-body border-bottom py-2 small text-muted">
        <i class="fas fa-info-circle mr-1"></i>Para agregar un médico: créale una cuenta con el rol <strong>Médico</strong> en Usuarios y luego
        registra aquí su especialidad, consultorio y horario. Solo los médicos <span class="badge badge-success">Atiende</span> aparecen al reservar.
    </div>
    <div class="card-body border-bottom pb-2">
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-12 col-lg-4 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" placeholder="Nombre, usuario o CMP" value="<%= EscapeHtml.texto(q) %>">
                </div>
            </div>
            <div class="col-6 col-md-4 col-lg-3 mb-2">
                <label for="especialidad" class="small text-muted mb-1">Especialidad</label>
                <select id="especialidad" name="especialidad" class="form-control">
                    <option value="">Todas</option>
                    <% if (especialidades != null) for (OpcionDTO e : especialidades) { %>
                    <option value="<%= e.getId() %>" <%= espSel != null && espSel == e.getId() ? "selected" : "" %>><%= EscapeHtml.texto(e.getNombre()) %></option>
                    <% } %>
                </select>
            </div>
            <div class="col-6 col-md-4 col-lg-2 mb-2">
                <label for="estado" class="small text-muted mb-1">Estado</label>
                <select id="estado" name="estado" class="form-control">
                    <option value="">Todos</option>
                    <option value="ATIENDE" <%= "ATIENDE".equals(estadoSel) ? "selected" : "" %>>Atiende</option>
                    <option value="SIN_CONFIGURAR" <%= "SIN_CONFIGURAR".equals(estadoSel) ? "selected" : "" %>>Sin configurar</option>
                    <option value="PAUSA" <%= "PAUSA".equals(estadoSel) ? "selected" : "" %>>Citas en pausa</option>
                </select>
            </div>
            <div class="col-md-4 col-lg mb-2">
                <button type="submit" class="btn btn-secondary"><i class="fas fa-filter mr-1"></i> Filtrar</button>
                <% if (medicos != null) { %><a href="<%= urlLista %>" class="btn btn-link">Limpiar</a><% } %>
            </div>
        </form>
    </div>
    <div class="card-body p-0">
    <% if (errorDatos) { %>
        <div class="estado-vacio"><i class="fas fa-plug text-warning"></i><p class="mb-1"><strong>No pudimos cargar los médicos.</strong></p></div>
    <% } else if (medicos == null) { %>
        <%@ include file="/WEB-INF/vistas/comun/inicio-filtros.jspf" %>
    <% } else if (medicos.isEmpty()) { %>
        <div class="estado-vacio"><i class="fas fa-user-md text-muted"></i>
            <p class="mb-1"><strong>Ningún médico coincide con la búsqueda.</strong></p>
            <a href="<%= urlLista %>">Quitar filtros</a></div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="thead-light"><tr>
                <th scope="col">Médico</th><th scope="col">Especialidad</th><th scope="col" class="d-none d-lg-table-cell">Horario</th>
                <th scope="col">Estado</th><th scope="col" class="text-right"><span class="sr-only">Acciones</span></th></tr></thead>
            <tbody>
            <% for (MedicoDTO m : medicos) {
                   String nombre = EscapeHtml.texto(m.getNombreConTitulo()); %>
                <tr class="<%= m.isAtiende() ? "" : "fila-inactiva" %>">
                    <td><strong><%= nombre %></strong><div class="small text-muted">@<%= EscapeHtml.texto(m.getNombreUsuario()) %>
                        <%= m.isConfigurado() ? " · CMP " + EscapeHtml.texto(m.getCmp()) : "" %></div></td>
                    <td><%= m.isConfigurado() ? EscapeHtml.texto(m.getEspecialidad()) + "<div class=\"small text-muted\">" + EscapeHtml.texto(m.getConsultorio()) + " · " + m.getDuracionCita() + " min</div>" : "<span class=\"text-muted\">—</span>" %></td>
                    <td class="d-none d-lg-table-cell small"><%= EscapeHtml.texto(m.getResumenHorario()) %></td>
                    <td>
                        <% if (!m.isConfigurado()) { %><span class="badge badge-warning">Sin configurar</span>
                        <% } else if (!m.isUsuarioActivo()) { %><span class="badge badge-secondary" title="Su cuenta está desactivada">Cuenta inactiva</span>
                        <% } else if (!m.isActivo()) { %><span class="badge badge-secondary">Citas en pausa</span>
                        <% } else if (m.getHorarios().isEmpty()) { %><span class="badge badge-warning">Sin horario</span>
                        <% } else { %><span class="badge badge-success">Atiende</span><% } %>
                    </td>
                    <td class="text-right text-nowrap">
                        <a href="<%= ctx %>/admin/medicos?accion=editar&u=<%= m.getIdUsuario() %>" class="btn btn-sm <%= m.isConfigurado() ? "btn-outline-primary" : "btn-primary" %>">
                            <i class="fas fa-<%= m.isConfigurado() ? "pen" : "plus" %> mr-1"></i><%= m.isConfigurado() ? "Editar horario" : "Configurar" %></a>
                        <% if (m.isConfigurado()) { %>
                        <button type="button" class="btn btn-sm <%= m.isActivo() ? "btn-outline-secondary" : "btn-outline-success" %>"
                                data-confirmar="<%= m.isActivo() ? "pausar" : "reanudar" %>" data-id="<%= m.getIdUsuario() %>" data-nombre="<%= nombre %>"
                                data-color="<%= m.isActivo() ? "btn-secondary" : "btn-success" %>" data-titulo="<%= m.isActivo() ? "Pausar citas" : "Reanudar citas" %>"
                                data-boton="<%= m.isActivo() ? "Sí, pausar" : "Sí, reanudar" %>"
                                data-mensaje="<%= m.isActivo() ? "¿Pausar las citas de {usuario}? Ya no aparecerá para nuevas reservas (vacaciones, licencia...). Sus citas ya reservadas se mantienen." : "¿{usuario} vuelve a recibir citas?" %>"
                                title="<%= m.isActivo() ? "Pausar (vacaciones, licencia)" : "Reanudar" %>"><i class="fas fa-<%= m.isActivo() ? "pause" : "play" %>"></i></button>
                        <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    <% } %>
    </div>
    <% if (medicos != null && medicos.size() >= MedicoRepository.LIMITE_BUSQUEDA) { %>
    <div class="card-footer small text-muted">Se muestran los primeros <%= MedicoRepository.LIMITE_BUSQUEDA %>. Escribe un nombre o elige una especialidad para afinar.</div>
    <% } %>
</div>
<div id="datosConfirmar" data-action="<%= ctx %>/admin/medicos" data-regreso="<%= EscapeHtml.texto(regreso) %>" hidden></div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
