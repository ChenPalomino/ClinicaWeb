<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Inicio del PACIENTE (portal). Las citas y pagos se llenarán con los módulos de citas y cobranza. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>

<div class="row">
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-primary elevation-1"><i class="fas fa-calendar-check"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Próxima cita</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Aún no tienes citas</small>
            </div>
        </div>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-history"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Citas anteriores</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Tu historial aparecerá aquí</small>
            </div>
        </div>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-success elevation-1"><i class="fas fa-receipt"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Pagos y comprobantes</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Boletas y facturas</small>
            </div>
        </div>
    </div>
</div>
<div class="card animar">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-calendar-plus mr-2"></i>Mis próximas citas</h3></div>
    <div class="card-body estado-vacio text-center py-4">
        <i class="fas fa-calendar-plus fa-2x text-muted mb-2" aria-hidden="true"></i>
        <p class="mb-0 text-muted">Todavía no tienes citas reservadas. Pronto podrás reservarlas desde aquí; mientras tanto, mantén tu correo y celular al día en <a href=\"<%= ctx %>/perfil\">Mi perfil</a> para recibir los recordatorios.</p>
    </div>
</div>
<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
