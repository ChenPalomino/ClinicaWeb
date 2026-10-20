<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.CuentaAtencionDTO, controlador.FormatoVista" %>
<%-- Portal del paciente: SUS pagos y comprobantes (nunca los de otra persona). --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked") List<CuentaAtencionDTO> cuentas = (List<CuentaAtencionDTO>) request.getAttribute("cuentas");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
%>
<div class="card">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-receipt mr-2"></i>Mis pagos y comprobantes</h3></div>
    <% if (errorDatos || cuentas == null) { %>
    <div class="estado-vacio"><i class="fas fa-plug text-warning"></i><p class="mb-1"><strong>No pudimos cargar tus pagos.</strong></p>
        <p class="text-muted">Inténtalo de nuevo en unos minutos.</p></div>
    <% } else if (cuentas.isEmpty()) { %>
    <div class="estado-vacio"><i class="fas fa-receipt text-muted"></i>
        <p class="mb-1"><strong>Aún no tienes pagos registrados.</strong></p>
        <p class="text-muted">Cuando pagues una atención en caja, aquí podrás ver y descargar tu boleta o factura.</p></div>
    <% } else { %>
    <ul class="list-group list-group-flush">
        <% for (CuentaAtencionDTO c : cuentas) { %>
        <li class="list-group-item d-flex flex-wrap align-items-center <%= c.isAnulada() ? "fila-inactiva" : "" %>">
            <div class="mr-auto">
                <div class="font-weight-bold"><%= c.getComprobante() == null ? EscapeHtml.texto(c.getNumeroCuenta()) : c.getComprobante() %>
                    <% if (c.isAnulada()) { %><span class="badge badge-secondary ml-1">Anulado</span><% } %></div>
                <small class="text-muted"><%= FormatoVista.fechaHora(c.isAnulada() ? c.getFechaAnulacion() : c.getFechaPago()) %> · <%= c.getCantidadItems() %> <%= c.getCantidadItems() == 1 ? "ítem" : "ítems" %>
                    <% if (c.getMontoAseguradora().signum() > 0) { %> · tu seguro cubrió S/ <%= FormatoVista.soles(c.getMontoAseguradora()) %><% } %></small>
            </div>
            <div class="h5 mb-0 mr-3 monto">S/ <%= FormatoVista.soles(c.getTotal()) %></div>
            <% if (c.getIdComprobante() > 0) { %>
            <a href="<%= ctx %>/paciente/comprobante?id=<%= c.getIdComprobante() %>" class="btn btn-sm btn-outline-primary"><i class="fas fa-file-invoice mr-1"></i> Ver comprobante</a>
            <% } %>
        </li>
        <% } %>
    </ul>
    <% } %>
</div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
