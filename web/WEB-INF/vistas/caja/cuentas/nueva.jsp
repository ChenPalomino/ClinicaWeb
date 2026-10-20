<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="modelo.dto.PacienteDTO, modelo.dto.PaginaDTO" %>
<%-- NUEVA CUENTA (RF-11): primero se busca al paciente; su cobertura define la tarifa. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked")
    PaginaDTO<PacienteDTO> pacientes = (PaginaDTO<PacienteDTO>) request.getAttribute("pacientes");
    String q = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
    String urlLista = ctx + "/caja/cuentas";
%>

<div class="row">
    <div class="col-lg-8">
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-user-injured mr-2"></i>1. ¿A quién se le cobra?</h3></div>
            <div class="card-body">
                <form method="GET" action="<%= urlLista %>" role="search">
                    <input type="hidden" name="accion" value="nueva">
                    <label for="q">Busca al paciente</label>
                    <div class="input-group">
                        <input type="search" id="q" name="q" class="form-control" maxlength="60" autofocus required
                               placeholder="DNI, apellidos o N.º de historia" value="<%= EscapeHtml.texto(q) %>">
                        <div class="input-group-append"><button type="submit" class="btn btn-primary"><i class="fas fa-search mr-1"></i> Buscar</button></div>
                    </div>
                    <small class="form-text text-muted">Solo aparecen pacientes activos. Si no está registrado, regístralo primero en
                        <a href="<%= ctx %>/recepcion/pacientes?accion=nuevo<%= q.matches("\\d{8}") ? "&dni=" + q : "" %>">Pacientes</a>.</small>
                </form>
            </div>
            <% if (pacientes != null) { %>
            <div class="card-body p-0 border-top">
                <% if (pacientes.getElementos().isEmpty()) { %>
                <div class="estado-vacio">
                    <i class="fas fa-user-slash text-muted"></i>
                    <p class="mb-1"><strong>No encontramos pacientes activos con «<%= EscapeHtml.texto(q) %>».</strong></p>
                    <a href="<%= ctx %>/recepcion/pacientes?accion=nuevo<%= q.matches("\\d{8}") ? "&dni=" + q : "" %>" class="btn btn-outline-primary btn-sm mt-2">
                        <i class="fas fa-plus mr-1"></i> Registrar paciente</a>
                </div>
                <% } else { %>
                <ul class="list-group list-group-flush lista-pacientes-caja">
                    <% for (PacienteDTO p : pacientes.getElementos()) { %>
                    <li class="list-group-item d-flex flex-wrap align-items-center">
                        <span class="avatar-iniciales avatar-tabla mr-3" aria-hidden="true"><%= EscapeHtml.texto(p.getIniciales()) %></span>
                        <div class="mr-auto">
                            <div class="font-weight-bold"><%= EscapeHtml.texto(p.getNombreCompleto()) %></div>
                            <small class="text-muted">DNI <%= EscapeHtml.texto(p.getDni()) %> · Historia <%= EscapeHtml.texto(p.getNumeroHistoria()) %>
                                · <span class="badge <%= "PARTICULAR".equals(p.getTipoCobertura()) ? "badge-light border" : "badge-info" %>"><%= EscapeHtml.texto(p.getNombreCobertura()) %></span></small>
                        </div>
                        <form method="POST" action="<%= urlLista %>" class="mt-2 mt-sm-0">
                            <input type="hidden" name="accion" value="abrir">
                            <input type="hidden" name="idPaciente" value="<%= p.getIdPaciente() %>">
                            <button type="submit" class="btn btn-primary btn-sm" aria-label="Abrir cuenta para <%= EscapeHtml.texto(p.getNombreCompleto()) %>">
                                <i class="fas fa-file-invoice-dollar mr-1"></i> Abrir cuenta</button>
                        </form>
                    </li>
                    <% } %>
                </ul>
                <% if (pacientes.getTotal() > pacientes.getElementos().size()) { %>
                <p class="small text-muted px-3 py-2 mb-0">Hay más resultados: escribe el DNI para encontrarlo directo.</p>
                <% } %>
                <% } %>
            </div>
            <% } %>
        </div>
    </div>
    <div class="col-lg-4">
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-route mr-2"></i>Cómo se cobra</h3></div>
            <div class="card-body small">
                <ol class="pl-3 mb-0 pasos-cobro">
                    <li><strong>Busca al paciente</strong> y abre su cuenta.</li>
                    <li><strong>Agrega</strong> las consultas, análisis o productos.</li>
                    <li>El sistema aplica la <strong>tarifa de su cobertura</strong> (particular, EPS, seguro o convenio) y calcula el IGV.</li>
                    <li><strong>Cobra</strong> en efectivo, tarjeta o Yape/Plin y emite <strong>boleta o factura</strong>.</li>
                </ol>
            </div>
        </div>
        <a href="<%= urlLista %>" class="btn btn-link px-0"><i class="fas fa-arrow-left mr-1"></i> Volver a las cuentas</a>
    </div>
</div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
