<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Inicio de RECEPCIÓN (admisión y caja). Los indicadores se llenarán con los módulos de citas y cobranza (Etapas 5 y 6). --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>

<div class="row">
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-calendar-day"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Citas de hoy</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Se activa con el módulo de citas</small>
            </div>
        </div>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-success elevation-1"><i class="fas fa-user-check"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Pacientes atendidos</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Se activa con el módulo de citas</small>
            </div>
        </div>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-warning elevation-1"><i class="fas fa-cash-register"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Cobros del día</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Se activa con el módulo de caja</small>
            </div>
        </div>
    </div>
</div>
<div class="card animar">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-clipboard-list mr-2"></i>Agenda de hoy</h3></div>
    <div class="card-body estado-vacio text-center py-4">
        <i class="fas fa-clipboard-list fa-2x text-muted mb-2" aria-hidden="true"></i>
        <p class="mb-0 text-muted">Aquí verás las citas del día para registrar la llegada de cada paciente y su cobro.</p>
    </div>
</div>
<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
