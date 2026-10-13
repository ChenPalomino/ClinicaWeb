<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- Inicio del MÉDICO. La agenda se llenará con el módulo de citas. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/bienvenida.jspf" %>

<div class="row">
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-primary elevation-1"><i class="fas fa-stethoscope"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Pacientes de hoy</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Se activa con el módulo de citas</small>
            </div>
        </div>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-info elevation-1"><i class="fas fa-clock"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Próxima cita</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Se activa con el módulo de citas</small>
            </div>
        </div>
    </div>
    <div class="col-md-4 animar">
        <div class="info-box">
            <span class="info-box-icon bg-secondary elevation-1"><i class="fas fa-notes-medical"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Atenciones del mes</span>
                <span class="info-box-number text-muted" title="Disponible cuando se active el módulo">&mdash;</span>
                <small class="text-muted">Se activa con historias clínicas</small>
            </div>
        </div>
    </div>
</div>
<div class="card animar">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-calendar-alt mr-2"></i>Mi agenda de hoy</h3></div>
    <div class="card-body estado-vacio text-center py-4">
        <i class="fas fa-calendar-alt fa-2x text-muted mb-2" aria-hidden="true"></i>
        <p class="mb-0 text-muted">Aquí aparecerán tus citas en orden de hora. Mientras tanto, completa tu presentación en <a href=\"<%= ctx %>/perfil\">Mi perfil</a>: los pacientes la verán al reservar.</p>
    </div>
</div>
<%@ include file="/WEB-INF/vistas/comun/modulos.jspf" %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
