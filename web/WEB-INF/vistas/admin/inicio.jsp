<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.indicadores.IndicadorStrategy" %>
<%-- Inicio del ADMINISTRADOR: lo de hoy (consultas livianas) e indicadores que se calculan solo cuando se piden. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    @SuppressWarnings("unchecked") List<IndicadorStrategy> indicadores = (List<IndicadorStrategy>) request.getAttribute("indicadores");
    Integer porCobrar = (Integer) request.getAttribute("porCobrar");
    Integer pagosInformados = (Integer) request.getAttribute("pagosInformados");
    modelo.dto.ResumenCitasDTO citasHoy = (modelo.dto.ResumenCitasDTO) request.getAttribute("resumenHoy");
%>

<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>

<% if (errorDatos) { %>
    <div class="alert alert-warning"><i class="fas fa-exclamation-circle mr-2"></i>No pudimos cargar algunos datos de hoy. Recarga la página en unos minutos.</div>
<% } %>

<h2 class="h6 text-muted text-uppercase mb-2">Hoy</h2>
<div class="row">
    <% if (citasHoy != null) { %>
    <div class="col-sm-6 col-xl-3 animar">
        <a href="<%= ctx %>/recepcion/citas" class="info-box text-reset">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-calendar-day"></i></span>
            <div class="info-box-content"><span class="info-box-text">Citas de hoy</span>
                <span class="info-box-number"><%= citasHoy.getProgramadas() %></span>
                <small class="text-muted"><%= citasHoy.getPendientes() %> por atender</small></div>
        </a>
    </div>
    <div class="col-sm-6 col-xl-3 animar">
        <a href="<%= ctx %>/recepcion/citas?estado=ATENDIDA" class="info-box text-reset">
            <span class="info-box-icon bg-success elevation-1"><i class="fas fa-check-circle"></i></span>
            <div class="info-box-content"><span class="info-box-text">Atendidas</span>
                <span class="info-box-number"><%= citasHoy.getAtendidas() %></span>
                <small class="text-muted"><%= citasHoy.getNoAsistio() %> no asistieron · <%= citasHoy.getCanceladas() %> canceladas</small></div>
        </a>
    </div>
    <% } %>
    <% if (porCobrar != null) { %>
    <div class="col-sm-6 col-xl-3 animar">
        <a href="<%= ctx %>/caja/cuentas" class="info-box text-reset">
            <span class="info-box-icon bg-warning elevation-1"><i class="fas fa-hourglass-half"></i></span>
            <div class="info-box-content"><span class="info-box-text">Cuentas por cobrar</span>
                <span class="info-box-number"><%= porCobrar %></span>
                <small class="text-muted">abiertas en caja</small></div>
        </a>
    </div>
    <% } %>
    <% if (pagosInformados != null) { %>
    <div class="col-sm-6 col-xl-3 animar">
        <a href="<%= ctx %>/caja/cuentas#pagos-informados" class="info-box text-reset">
            <span class="info-box-icon <%= pagosInformados > 0 ? "bg-primary" : "bg-light" %> elevation-1"><i class="fas fa-mobile-alt"></i></span>
            <div class="info-box-content"><span class="info-box-text">Pagos informados</span>
                <span class="info-box-number"><%= pagosInformados %></span>
                <small class="text-muted"><%= pagosInformados > 0 ? "esperan revisión de caja" : "nada por revisar" %></small></div>
        </a>
    </div>
    <% } %>
</div>

<% if (indicadores != null) { %>
<div class="card card-outline card-primary animar" id="indicadores">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h2 class="card-title h5 mb-0 mr-auto"><i class="fas fa-chart-bar mr-2"></i>Indicadores</h2>
        <div class="btn-group btn-group-sm flex-wrap periodos" role="radiogroup" aria-label="Periodo">
            <% String[][] periodos = {{"7", "7 días"}, {"30", "30 días"}, {"mes", "Este mes"}, {"mesAnterior", "Mes anterior"}, {"90", "90 días"}, {"365", "12 meses"}};
               for (String[] pe : periodos) { %>
            <button type="button" class="btn btn-outline-primary <%= "30".equals(pe[0]) ? "active" : "" %>" data-periodo="<%= pe[0] %>" role="radio" aria-checked="<%= "30".equals(pe[0]) %>"><%= pe[1] %></button>
            <% } %>
        </div>
    </div>
    <div class="card-body">
        <p class="small text-muted mb-3"><i class="fas fa-info-circle mr-1"></i>Elige qué quieres ver. Cada indicador se calcula solo cuando lo pides, así el inicio abre rápido.</p>
        <div class="catalogo-indicadores">
        <% String grupo = "";
           for (IndicadorStrategy i : indicadores) {
               if (!i.grupo().equals(grupo)) { if (!grupo.isEmpty()) { %></div><% } grupo = i.grupo(); %>
            <div class="grupo-indicadores"><h3 class="h6 text-muted mb-2"><%= EscapeHtml.texto(grupo) %></h3><%
               } %>
            <button type="button" class="indicador-opcion" data-indicador="<%= i.codigo() %>" aria-pressed="false" aria-controls="resultadoIndicador">
                <i class="<%= i.icono() %>" aria-hidden="true"></i>
                <span><strong><%= EscapeHtml.texto(i.titulo()) %></strong><small><%= EscapeHtml.texto(i.descripcion()) %></small></span>
                <% if (!i.usaPeriodo()) { %><span class="badge badge-light border ml-auto align-self-start">Actual</span><% } %>
            </button>
        <% } if (!grupo.isEmpty()) { %></div><% } %>
        </div>
    </div>
    <div class="card-footer bg-transparent" id="resultadoIndicador" aria-live="polite" hidden>
        <div class="d-flex flex-wrap align-items-baseline mb-2">
            <h3 class="h5 mb-0 mr-2" id="tituloIndicador"></h3>
            <span class="text-muted small mr-auto" id="periodoIndicador"></span>
            <a href="#" class="small" id="enlaceIndicador" hidden>Ver el detalle <i class="fas fa-arrow-right"></i></a>
        </div>
        <div id="estadoIndicador"></div>
        <div class="row resumen-indicador" id="resumenIndicador"></div>
        <div class="grafico grafico-indicador"><canvas id="graficoIndicador" role="img"></canvas></div>
        <p class="small text-muted mt-2 mb-1" id="notaIndicador"></p>
        <details class="tabla-indicador"><summary class="small">Ver los datos en tabla</summary><div class="table-responsive" id="tablaIndicador"></div></details>
    </div>
</div>
<% } %>

<div class="card animar">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-bolt mr-2"></i>Accesos rápidos</h3></div>
    <div class="card-body">
        <a href="<%= ctx %>/admin/usuarios?accion=nuevo" class="btn btn-primary mb-2"><i class="fas fa-user-plus mr-1"></i> Nuevo usuario</a>
        <a href="<%= ctx %>/admin/usuarios?estado=BLOQUEADO" class="btn btn-outline-danger mb-2"><i class="fas fa-unlock mr-1"></i> Desbloquear cuentas</a>
        <a href="<%= ctx %>/admin/especialidades" class="btn btn-outline-secondary mb-2"><i class="fas fa-stethoscope mr-1"></i> Especialidades</a>
        <a href="<%= ctx %>/admin/billeteras" class="btn btn-outline-secondary mb-2"><i class="fas fa-qrcode mr-1"></i> Billeteras y QR</a>
    </div>
</div>

<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>

<script src="<%= ctx %>/js/indicadores.js?v=<%= VersionAplicacion.RECURSOS %>" data-url="<%= ctx %>/admin/indicadores" id="scriptIndicadores" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
