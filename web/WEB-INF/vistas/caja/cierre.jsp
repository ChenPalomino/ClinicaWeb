<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.math.BigDecimal, modelo.dto.ResumenCajaDTO, modelo.dto.PagoDTO, controlador.FormatoVista" %>
<%-- CIERRE DE CAJA DIARIO (RF-17): lo cobrado hoy por medio de pago, efectivo contado y diferencia. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    private String s(BigDecimal v) { return controlador.FormatoVista.soles(v); }
    private String fecha(java.time.LocalDate f) { return f == null ? "" : f.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
%>
<%
    ResumenCajaDTO r = (ResumenCajaDTO) request.getAttribute("resumen");
    @SuppressWarnings("unchecked") List<ResumenCajaDTO> historial = (List<ResumenCajaDTO>) request.getAttribute("historial");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
%>
<% if (errorDatos || r == null) { %>
<div class="card"><div class="estado-vacio"><i class="fas fa-plug text-warning"></i>
    <p class="mb-1"><strong>No pudimos cargar la caja.</strong></p><p class="text-muted">Revisa la conexión y recarga la página.</p></div></div>
<% } else { %>
<div class="row">
    <div class="col-lg-8">
        <div class="card">
            <div class="card-header d-flex align-items-center">
                <h3 class="card-title mr-auto"><i class="fas fa-cash-register mr-2"></i>Mi caja de hoy · <%= fecha(r.getFecha()) %></h3>
                <% if (r.isCerrada()) { %><span class="badge badge-secondary p-2"><i class="fas fa-lock mr-1"></i>Cerrada</span>
                <% } else { %><span class="badge badge-success p-2"><i class="fas fa-lock-open mr-1"></i>Abierta</span><% } %>
            </div>
            <div class="card-body">
                <div class="row text-center resumen-caja">
                    <div class="col-6 col-md-3 mb-3"><div class="small text-muted">Efectivo</div><div class="h5 monto mb-0">S/ <%= s(r.getTotalEfectivo()) %></div></div>
                    <div class="col-6 col-md-3 mb-3"><div class="small text-muted">Tarjeta</div><div class="h5 monto mb-0">S/ <%= s(r.getTotalTarjeta()) %></div></div>
                    <div class="col-6 col-md-3 mb-3"><div class="small text-muted">Yape / Plin</div><div class="h5 monto mb-0">S/ <%= s(r.getTotalBilletera()) %></div></div>
                    <div class="col-6 col-md-3 mb-3"><div class="small text-muted">Total cobrado</div><div class="h4 monto mb-0 text-primary">S/ <%= s(r.getTotalGeneral()) %></div></div>
                </div>
                <p class="small text-muted mb-0 text-center"><%= r.getCantidadPagos() %> <%= r.getCantidadPagos() == 1 ? "cobro" : "cobros" %>
                    <% if (r.getCantidadRechazados() > 0) { %> · <%= r.getCantidadRechazados() %> no <%= r.getCantidadRechazados() == 1 ? "aprobado" : "aprobados" %><% } %>
                    <% if (r.getCantidadAnulados() > 0) { %> · <%= r.getCantidadAnulados() %> <%= r.getCantidadAnulados() == 1 ? "anulado" : "anulados" %><% } %></p>
            </div>
            <% if (!r.getPagos().isEmpty()) { %>
            <div class="table-responsive border-top">
            <table class="table table-sm mb-0">
                <thead class="thead-light"><tr><th scope="col">Hora</th><th scope="col">Cuenta</th><th scope="col" class="d-none d-md-table-cell">Paciente</th>
                    <th scope="col">Medio</th><th scope="col" class="text-right">Monto</th><th scope="col">Estado</th></tr></thead>
                <tbody>
                <% for (PagoDTO p : r.getPagos()) { %>
                    <tr class="<%= p.isAprobado() ? "" : "fila-inactiva" %>">
                        <td><%= new java.text.SimpleDateFormat("HH:mm").format(p.getFechaCreacion()) %></td>
                        <td><a href="<%= ctx %>/caja/cuentas?accion=ver&id=<%= p.getIdCuentaAtencion() %>"><%= EscapeHtml.texto(p.getNumeroCuenta()) %></a>
                            <% if (p.getComprobante() != null) { %><div class="small text-monospace text-muted"><%= p.getComprobante() %></div><% } %></td>
                        <td class="d-none d-md-table-cell"><%= EscapeHtml.texto(p.getNombrePaciente()) %></td>
                        <td><%= EscapeHtml.texto(p.getNombreMetodo()) %></td>
                        <td class="text-right monto">S/ <%= s(p.getMonto()) %></td>
                        <td><% if (p.isAprobado()) { %><span class="badge badge-success">Cobrado</span>
                            <% } else if (p.isRechazado()) { %><span class="badge badge-danger" title="<%= EscapeHtml.texto(p.getMotivoRechazo()) %>">No aprobado</span>
                            <% } else { %><span class="badge badge-secondary">Anulado</span><% } %></td>
                    </tr>
                <% } %>
                </tbody>
            </table>
            </div>
            <% } %>
        </div>
    </div>

    <div class="col-lg-4">
        <% if (r.isCerrada()) { %>
        <div class="card card-outline card-secondary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-lock mr-2"></i>Caja cerrada</h3></div>
            <div class="card-body">
                <dl class="row mb-0 small">
                    <dt class="col-7">Efectivo según sistema</dt><dd class="col-5 text-right monto">S/ <%= s(r.getTotalEfectivo()) %></dd>
                    <dt class="col-7">Efectivo contado</dt><dd class="col-5 text-right monto">S/ <%= s(r.getEfectivoContado()) %></dd>
                    <dt class="col-7">Diferencia</dt><dd class="col-5 text-right monto <%= r.getDiferencia().signum() < 0 ? "text-danger" : r.getDiferencia().signum() > 0 ? "text-warning" : "text-success" %>">S/ <%= s(r.getDiferencia()) %></dd>
                </dl>
                <% if (r.getObservacion() != null) { %><p class="small mt-2 mb-0"><%= EscapeHtml.texto(r.getObservacion()) %></p><% } %>
                <p class="small text-muted mt-2 mb-0">Cerrada a las <%= FormatoVista.fechaHora(r.getFechaCierre()) %>. Hasta mañana no puedes cobrar con tu usuario.</p>
            </div>
        </div>
        <% } else { %>
        <form method="POST" action="<%= ctx %>/caja/cierre" class="card card-outline card-primary" id="formCierre" data-form-crud>
            <input type="hidden" name="accion" value="cerrar">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-lock mr-2"></i>Cerrar mi caja</h3></div>
            <div class="card-body">
                <p class="small text-muted">Cuenta el dinero en efectivo de tu caja. Según el sistema deberías tener <strong>S/ <%= s(r.getTotalEfectivo()) %></strong> (sin contar el fondo inicial).</p>
                <div class="form-group">
                    <label for="efectivoContado">Efectivo contado (S/)</label>
                    <input type="text" inputmode="decimal" id="efectivoContado" name="efectivoContado" class="form-control form-control-lg" required
                           autocomplete="off" placeholder="<%= r.getTotalEfectivo().toPlainString() %>">
                </div>
                <div class="form-group">
                    <label for="observacion">Observación <small class="text-muted">(obligatoria si hay diferencia)</small></label>
                    <textarea id="observacion" name="observacion" class="form-control" rows="2" maxlength="250" placeholder="Ej: faltó sencillo para un vuelto"></textarea>
                </div>
                <p class="small text-warning mb-0"><i class="fas fa-exclamation-triangle mr-1"></i>Al cerrar, ya no podrás cobrar con tu usuario hasta mañana.</p>
            </div>
            <div class="card-footer"><button type="submit" class="btn btn-primary btn-block"><i class="fas fa-lock mr-1"></i> Cerrar caja</button></div>
        </form>
        <% } %>
        <a href="<%= ctx %>/caja/cuentas" class="btn btn-link px-0"><i class="fas fa-arrow-left mr-1"></i> Volver a cobros</a>
    </div>
</div>

<% if (historial != null && !historial.isEmpty()) { %>
<div class="card">
    <div class="card-header"><h3 class="card-title"><i class="fas fa-history mr-2"></i>Cierres anteriores</h3></div>
    <div class="table-responsive">
    <table class="table table-sm table-hover mb-0">
        <thead class="thead-light"><tr><th scope="col">Fecha</th><th scope="col">Cajero</th><th scope="col" class="text-right">Cobros</th>
            <th scope="col" class="text-right">Total</th><th scope="col" class="text-right d-none d-sm-table-cell">Efectivo contado</th><th scope="col" class="text-right">Diferencia</th></tr></thead>
        <tbody>
        <% for (ResumenCajaDTO h : historial) { %>
            <tr>
                <td><%= fecha(h.getFecha()) %></td>
                <td>@<%= EscapeHtml.texto(h.getNombreCajero()) %></td>
                <td class="text-right"><%= h.getCantidadPagos() %></td>
                <td class="text-right monto">S/ <%= s(h.getTotalGeneral()) %></td>
                <td class="text-right monto d-none d-sm-table-cell">S/ <%= s(h.getEfectivoContado()) %></td>
                <td class="text-right monto <%= h.getDiferencia().signum() < 0 ? "text-danger" : h.getDiferencia().signum() > 0 ? "text-warning" : "" %>"
                    title="<%= h.getObservacion() == null ? "" : EscapeHtml.texto(h.getObservacion()) %>">S/ <%= s(h.getDiferencia()) %></td>
            </tr>
        <% } %>
        </tbody>
    </table>
    </div>
</div>
<% } %>
<% } %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
