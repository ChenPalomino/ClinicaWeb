<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.*, modelo.citas.FechaTexto, modelo.servicios.ReportePagoService, modelo.cobranza.pago.SolicitudPago, controlador.FormatoVista" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    ReportePagoService.Preparacion prep = (ReportePagoService.Preparacion) request.getAttribute("preparacion");
    CitaDTO cita = prep.cita();
    CuentaAtencionDTO cuenta = prep.cuenta();
    ReportePagoDTO ultimo = prep.ultimoReporte();
    @SuppressWarnings("unchecked") List<OpcionDTO> billeteras = (List<OpcionDTO>) request.getAttribute("billeteras");
    BilleteraConfigDTO cfg = (BilleteraConfigDTO) request.getAttribute("configBilletera");
    SolicitudPago sol = (SolicitudPago) request.getAttribute("solicitud");
    Integer billeteraSel = sol.getIdBilletera();
    if (billeteraSel == null && billeteras != null && !billeteras.isEmpty()) billeteraSel = billeteras.get(0).getId();
    boolean enRevision = ultimo != null && ultimo.isPendiente();
    String total = FormatoVista.soles(cuenta.getTotal());
%>
<a href="<%= ctx %>/paciente/citas" class="btn btn-link px-0 mb-2"><i class="fas fa-arrow-left mr-1"></i> Mis citas</a>

<div class="row">
<div class="col-lg-5 order-lg-2">
    <div class="card card-outline card-primary">
        <div class="card-body">
            <div class="text-muted small text-uppercase">Tu cita</div>
            <div class="h6 mb-1"><%= EscapeHtml.texto(cita.getEspecialidad()) %> con Dr(a). <%= EscapeHtml.texto(cita.getNombreMedico()) %></div>
            <div class="small text-muted mb-2"><%= FechaTexto.relativaCompleta(cita.getFecha(), java.time.LocalDate.now()) %> · <%= FechaTexto.hora(cita.getHoraInicio()) %> · Código <%= EscapeHtml.texto(cita.getCodigo()) %></div>
            <ul class="list-unstyled small mb-2">
                <% for (DetalleCuentaDTO d : cuenta.getDetalles()) { %>
                <li class="d-flex"><span class="mr-auto"><%= EscapeHtml.texto(d.getDescripcion()) %><%= d.getCantidad() > 1 ? " × " + d.getCantidad() : "" %></span><span class="monto">S/ <%= FormatoVista.soles(d.getImporte()) %></span></li>
                <% } %>
                <% if (cuenta.getMontoAseguradora().signum() > 0) { %><li class="text-success">Tu seguro cubre S/ <%= FormatoVista.soles(cuenta.getMontoAseguradora()) %></li><% } %>
                <% if (cuenta.getMontoDescuento().signum() > 0) { %><li class="text-success">Descuento por convenio S/ <%= FormatoVista.soles(cuenta.getMontoDescuento()) %></li><% } %>
            </ul>
            <div class="d-flex align-items-baseline border-top pt-2"><span class="mr-auto">Total a pagar</span><span class="h3 mb-0 monto" id="montoPagar">S/ <%= total %></span></div>
            <small class="text-muted">IGV incluido. Al confirmarse, recibirás tu boleta en el correo y en «Mis pagos».</small>
        </div>
    </div>
</div>

<div class="col-lg-7 order-lg-1">
<% if (enRevision) { %>
    <div class="card card-outline card-info">
        <div class="card-body text-center py-4">
            <i class="fas fa-hourglass-half fa-2x text-info mb-2" aria-hidden="true"></i>
            <h2 class="h5">Estamos revisando tu pago</h2>
            <p class="mb-1">Nos avisaste un pago con <strong><%= EscapeHtml.texto(ultimo.getNombreBilletera()) %></strong> por <strong>S/ <%= FormatoVista.soles(ultimo.getMonto()) %></strong>
                el <%= FormatoVista.fechaHora(ultimo.getFechaCreacion()) %>.</p>
            <p class="text-muted small mb-3">No vuelvas a pagar. Cuando caja lo confirme verás «Pagado» en tu cita y te llegará la boleta.</p>
            <a href="<%= ctx %>/paciente/citas" class="btn btn-outline-primary">Volver a mis citas</a>
        </div>
    </div>
<% } else { %>
    <% if (ultimo != null && ultimo.isRechazado()) { %>
    <div class="alert alert-warning" role="alert">
        <strong><i class="fas fa-exclamation-triangle mr-1"></i>Tu aviso anterior no se pudo confirmar.</strong>
        <div class="small">Motivo: <%= EscapeHtml.texto(ultimo.getMotivoRechazo()) %>. Revisa los datos e inténtalo de nuevo, o paga en caja el día de tu cita.</div>
    </div>
    <% } %>
    <% if (errores.containsKey("general")) { %><div class="alert alert-danger" role="alert"><%= EscapeHtml.texto(errores.get("general")) %></div><% } %>
    <div class="card">
        <div class="card-header"><h2 class="card-title h5 mb-0"><i class="fas fa-mobile-alt mr-2"></i>Paga desde tu billetera digital</h2></div>
        <div class="card-body">
            <ol class="pasos-pago">
                <li>
                    <strong>Abre tu app y paga a la clínica</strong>
                    <% if (cfg.isTieneQr()) { %>
                    <div class="qr-caja d-inline-block my-2"><img src="<%= ctx %>/qr-pago?v=<%= cfg.getVersionQr() %>" alt="QR de pago de la clínica" class="img-fluid qr-imagen" width="200" height="200"></div>
                    <% } %>
                    <% if (!cfg.getCelular().isEmpty()) { %>
                    <div class="d-flex align-items-center flex-wrap" style="gap:.5rem">
                        <span>o al celular <strong class="text-monospace h5 mb-0" id="celularClinica"><%= EscapeHtml.texto(cfg.getCelularLegible()) %></strong></span>
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-copiar="<%= EscapeHtml.texto(cfg.getCelular()) %>"><i class="far fa-copy mr-1"></i>Copiar</button>
                    </div>
                    <% } %>
                    <% if (!cfg.getTitular().isEmpty()) { %><small class="d-block text-muted">Tu app debe mostrar el nombre <strong><%= EscapeHtml.texto(cfg.getTitular()) %></strong>.</small><% } %>
                    <% if (cfg.isTieneQr()) { %><small class="d-block text-muted">¿Estás en el celular? Guarda la imagen del QR y ábrela desde tu app, o usa el número.</small><% } %>
                </li>
                <li><strong>Paga exactamente S/ <%= total %></strong>
                    <small class="d-block text-muted">Si pagas otro monto no podremos confirmarlo en línea.</small></li>
                <li><strong>Cuéntanos los datos del pago</strong>
                    <form method="POST" action="<%= ctx %>/paciente/pagar" class="mt-2" novalidate data-un-envio>
                        <input type="hidden" name="cita" value="<%= cita.getIdCita() %>">
                        <fieldset class="form-group">
                            <legend class="col-form-label pt-0">¿Con qué app pagaste?</legend>
                            <div class="<%= invalido(errores, "billetera") %>">
                            <% for (OpcionDTO b : billeteras) { %>
                                <div class="custom-control custom-radio custom-control-inline mb-1">
                                    <input type="radio" id="bil<%= b.getId() %>" name="idBilletera" value="<%= b.getId() %>" class="custom-control-input" <%= billeteraSel != null && billeteraSel == b.getId() ? "checked" : "" %>>
                                    <label class="custom-control-label" for="bil<%= b.getId() %>"><%= EscapeHtml.texto(b.getNombre()) %></label>
                                </div>
                            <% } %>
                            </div>
                            <%= error(errores, "billetera") %>
                        </fieldset>
                        <div class="form-group">
                            <label for="numeroOperacion">N.º de operación</label>
                            <input type="text" id="numeroOperacion" name="numeroOperacion" maxlength="20" inputmode="numeric" data-solo-numeros autocomplete="off"
                                   class="form-control text-monospace <%= invalido(errores, "numeroOperacion") %>" value="<%= EscapeHtml.texto(sol.getNumeroOperacion()) %>" placeholder="Ej. 00123456 o solo 3456">
                            <small class="form-text text-muted">Lo ves en la constancia de tu app. Si no aparece completo, escribe los dígitos que se vean (mínimo 3).</small>
                            <%= error(errores, "numeroOperacion") %>
                        </div>
                        <div class="form-row">
                            <div class="form-group col-sm-5">
                                <label for="celular">Tu celular</label>
                                <input type="tel" id="celular" name="celular" maxlength="9" inputmode="numeric" data-solo-numeros autocomplete="tel-national"
                                       class="form-control <%= invalido(errores, "celular") %>" value="<%= EscapeHtml.texto(sol.getCelular()) %>" placeholder="9 dígitos">
                                <%= error(errores, "celular") %>
                            </div>
                            <div class="form-group col-sm-7">
                                <label for="nombrePagador">Nombre que sale en tu app <span class="text-muted small">(opcional)</span></label>
                                <input type="text" id="nombrePagador" name="nombrePagador" maxlength="80" autocomplete="name"
                                       class="form-control <%= invalido(errores, "nombrePagador") %>" value="<%= EscapeHtml.texto(sol.getNombrePagador()) %>">
                                <%= error(errores, "nombrePagador") %>
                            </div>
                        </div>
                        <small class="d-block text-muted mb-2">Necesitamos el N.º de operación o tu celular (al menos uno) para encontrar tu pago.</small>
                        <button type="submit" class="btn btn-success btn-lg btn-block"><i class="fas fa-paper-plane mr-1"></i>Ya pagué S/ <%= total %>: avisar a caja</button>
                    </form>
                </li>
            </ol>
            <p class="small text-muted mb-0"><i class="fas fa-store mr-1"></i>¿Prefieres pagar en persona? No hagas nada: paga en caja el día de tu cita con efectivo, tarjeta o billetera.</p>
        </div>
    </div>
<% } %>
</div>
</div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
