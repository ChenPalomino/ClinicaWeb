<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="modelo.dto.EstadisticasUsuariosDTO" %>
<%-- Inicio del ADMINISTRADOR: indicadores y gráficos de usuarios --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    EstadisticasUsuariosDTO est = (EstadisticasUsuariosDTO) request.getAttribute("estadisticas");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
%>

<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>

<% if (errorDatos) { %>
    <div class="alert alert-warning"><i class="fas fa-exclamation-circle mr-2"></i>No pudimos cargar los indicadores. Recarga la página en unos minutos.</div>
<% } %>

<% if (est != null) { %>
<%-- ================= Indicadores del Administrador ================= --%>
<div class="row">
    <div class="col-lg-3 col-6 animar">
        <div class="small-box bg-info">
            <div class="inner"><h3 data-contador="<%= est.getTotal() %>"><%= est.getTotal() %></h3><p>Usuarios registrados</p></div>
            <div class="icon"><i class="fas fa-users"></i></div>
            <a href="<%= ctx %>/admin/usuarios" class="small-box-footer">Ver todos <i class="fas fa-arrow-circle-right"></i></a>
        </div>
    </div>
    <div class="col-lg-3 col-6 animar">
        <div class="small-box bg-success">
            <div class="inner"><h3 data-contador="<%= est.getActivos() %>"><%= est.getActivos() %></h3><p>Activos</p></div>
            <div class="icon"><i class="fas fa-user-check"></i></div>
            <a href="<%= ctx %>/admin/usuarios?estado=ACTIVO" class="small-box-footer">Ver activos <i class="fas fa-arrow-circle-right"></i></a>
        </div>
    </div>
    <div class="col-lg-3 col-6 animar">
        <div class="small-box bg-danger">
            <div class="inner"><h3 data-contador="<%= est.getBloqueados() %>"><%= est.getBloqueados() %></h3><p>Bloqueados</p></div>
            <div class="icon"><i class="fas fa-user-lock"></i></div>
            <a href="<%= ctx %>/admin/usuarios?estado=BLOQUEADO" class="small-box-footer">Revisar bloqueados <i class="fas fa-arrow-circle-right"></i></a>
        </div>
    </div>
    <div class="col-lg-3 col-6 animar">
        <div class="small-box bg-secondary">
            <div class="inner"><h3 data-contador="<%= est.getInactivos() %>"><%= est.getInactivos() %></h3><p>Desactivados</p></div>
            <div class="icon"><i class="fas fa-user-slash"></i></div>
            <a href="<%= ctx %>/admin/usuarios?estado=INACTIVO" class="small-box-footer">Ver desactivados <i class="fas fa-arrow-circle-right"></i></a>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-lg-7 animar">
        <div class="card">
            <div class="card-header">
                <h3 class="card-title"><i class="fas fa-chart-bar mr-2"></i>Nuevos usuarios · últimos 14 días</h3>
                <span class="float-right text-muted small"><strong><%= est.getNuevosUltimos14Dias() %></strong> en total</span>
            </div>
            <div class="card-body">
                <div class="grafico"><canvas id="graficoDias" role="img" aria-label="Usuarios nuevos por día en los últimos 14 días"></canvas></div>
            </div>
        </div>
    </div>
    <div class="col-lg-5 animar">
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-user-tag mr-2"></i>Usuarios activos por rol</h3></div>
            <div class="card-body">
                <div class="grafico"><canvas id="graficoRoles" role="img" aria-label="Usuarios activos por rol"></canvas></div>
            </div>
        </div>
    </div>
</div>

<%-- Datos de los gráficos (los dibuja panel.js con Chart.js) y su tabla accesible --%>
<script type="application/json" id="datosGraficos">
{"dias": {"etiquetas": [<% for (int i = 0; i < est.getDiasEtiquetas().size(); i++) { %><%= i > 0 ? "," : "" %>"<%= est.getDiasEtiquetas().get(i) %>"<% } %>],
          "valores": <%= est.getDiasValores() %>},
 "roles": {"etiquetas": [<% for (int i = 0; i < est.getRolesEtiquetas().size(); i++) { %><%= i > 0 ? "," : "" %>"<%= est.getRolesEtiquetas().get(i).replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c") %>"<% } %>],
           "valores": <%= est.getRolesValores() %>}}
</script>

<div class="row">
    <div class="col-md-6 animar">
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-bolt mr-2"></i>Accesos rápidos</h3></div>
            <div class="card-body">
                <a href="<%= ctx %>/admin/usuarios?accion=nuevo" class="btn btn-primary mb-2"><i class="fas fa-user-plus mr-1"></i> Nuevo usuario</a>
                <a href="<%= ctx %>/admin/usuarios?estado=BLOQUEADO" class="btn btn-outline-danger mb-2"><i class="fas fa-unlock mr-1"></i> Desbloquear cuentas</a>
                <a href="<%= ctx %>/admin/usuarios?rol=personal" class="btn btn-outline-secondary mb-2"><i class="fas fa-user-md mr-1"></i> Ver personal</a>
            </div>
        </div>
    </div>
    <div class="col-md-6 animar">
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-users mr-2"></i>Composición</h3></div>
            <div class="card-body">
                <%
                    int total = Math.max(est.getTotal(), 1);
                    int pctPacientes = Math.round(est.getPacientes() * 100f / total);
                %>
                <a href="<%= ctx %>/admin/usuarios?rol=4" class="d-block text-reset mb-1">Pacientes <span class="float-right"><strong><%= est.getPacientes() %></strong></span></a>
                <div class="progress progress-sm mb-3"><div class="progress-bar bg-primary" style="width: <%= pctPacientes %>%"></div></div>
                <a href="<%= ctx %>/admin/usuarios?rol=personal" class="d-block text-reset mb-1">Personal de la clínica <span class="float-right"><strong><%= est.getPersonal() %></strong></span></a>
                <div class="progress progress-sm"><div class="progress-bar bg-info" style="width: <%= 100 - pctPacientes %>%"></div></div>
            </div>
        </div>
    </div>
</div>
<% } %>

<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>

<% if (est != null) { %>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.js" defer></script>
<% } %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
