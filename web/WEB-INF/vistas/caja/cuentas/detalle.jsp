<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.util.Map, java.math.BigDecimal, modelo.dto.*, modelo.cobranza.pago.SolicitudPago, controlador.FormatoVista" %>
<%--
    CUENTA DE ATENCIÓN: cabecera + detalle (RF-11), tarifa por cobertura (RF-12),
    cobro con efectivo / tarjeta / billetera (RF-13, RF-14), boleta o factura (RF-15)
    y anulación con autorización (RF-16).
--%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    private String inv(Map<String, String> e, String campo) { return e.containsKey(campo) ? "is-invalid" : ""; }
    private String msj(Map<String, String> e, String campo) {
        return e.containsKey(campo) ? "<div class=\"invalid-feedback d-block\">" + seguridad.EscapeHtml.texto(e.get(campo)) + "</div>" : "";
    }
    private String s(BigDecimal v) { return controlador.FormatoVista.soles(v); }
%>
<%
    CuentaAtencionDTO c = (CuentaAtencionDTO) request.getAttribute("cuenta");
    ParametroClinicaDTO par = (ParametroClinicaDTO) request.getAttribute("parametros");
    String tarifa = (String) request.getAttribute("tarifa");
    boolean puedeAnular = Boolean.TRUE.equals(request.getAttribute("puedeAnular"));
    @SuppressWarnings("unchecked") List<ProductoServicioDTO> frecuentes = (List<ProductoServicioDTO>) request.getAttribute("frecuentes");
    @SuppressWarnings("unchecked") List<OpcionDTO> billeteras = (List<OpcionDTO>) request.getAttribute("billeteras");
    BilleteraConfigDTO cfgBilletera = (BilleteraConfigDTO) request.getAttribute("configBilletera");
    ReportePagoDTO reporte = (ReportePagoDTO) request.getAttribute("reporte");
    @SuppressWarnings("unchecked") List<OpcionDTO> metodos = (List<OpcionDTO>) request.getAttribute("metodos");
    @SuppressWarnings("unchecked") Map<String, String> errores = (Map<String, String>) request.getAttribute("errores");
    if (errores == null) errores = Map.of();
    SolicitudPago sol = (SolicitudPago) request.getAttribute("solicitud");
    if (sol == null) sol = new SolicitudPago();
    DatosClienteDTO cli = (DatosClienteDTO) request.getAttribute("cliente");
    boolean hayCliente = cli != null;
    if (cli == null) { cli = new DatosClienteDTO(); cli.setNumeroDocumento(c.getDniPaciente()); cli.setNombre(c.getNombrePaciente()); }
    boolean factura = cli.isFactura();
    String metodoSel = (String) request.getAttribute("metodoElegido");
    if (metodoSel == null) metodoSel = "EFECTIVO";
    String url = ctx + "/caja/cuentas";
    boolean cobrable = c.isPendiente() && !c.getDetalles().isEmpty();
    String igvTexto = par.getIgvPorcentaje().stripTrailingZeros().toPlainString();
    if (reporte != null && request.getAttribute("metodoElegido") == null) metodoSel = "BILLETERA";
    Integer billeteraSel = sol.getIdBilletera();
    if (billeteraSel == null && billeteras != null && !billeteras.isEmpty()) billeteraSel = billeteras.get(0).getId();
%>

<div class="d-flex flex-wrap align-items-center mb-3">
    <a href="<%= url %>" class="btn btn-link px-0 mr-auto"><i class="fas fa-arrow-left mr-1"></i> Todas las cuentas</a>
    <% if (c.isPendiente()) { %><span class="badge badge-warning p-2"><i class="fas fa-hourglass-half mr-1"></i>Por cobrar</span>
    <% } else if (c.isPagada()) { %><span class="badge badge-success p-2"><i class="fas fa-check mr-1"></i>Pagada</span>
    <% } else { %><span class="badge badge-secondary p-2"><i class="fas fa-ban mr-1"></i>Anulada</span><% } %>
</div>

<div class="row">
<div class="col-lg-8">

    <%-- ===================== CABECERA ===================== --%>
    <div class="card card-outline card-primary">
        <div class="card-body">
            <div class="row">
                <div class="col-sm-6 mb-2 mb-sm-0">
                    <div class="text-muted small text-uppercase">Paciente</div>
                    <div class="h5 mb-0"><%= EscapeHtml.texto(c.getNombrePaciente()) %></div>
                    <small class="text-muted">DNI <%= EscapeHtml.texto(c.getDniPaciente()) %> · Historia <%= EscapeHtml.texto(c.getNumeroHistoria()) %></small>
                </div>
                <div class="col-sm-6">
                    <div class="text-muted small text-uppercase">Cobertura</div>
                    <span class="badge <%= "PARTICULAR".equals(c.getTipoCobertura()) ? "badge-light border" : "badge-info" %>"><%= EscapeHtml.texto(c.getNombreCobertura()) %></span>
                    <div class="small text-muted mt-1"><i class="fas fa-info-circle mr-1"></i><%= EscapeHtml.texto(tarifa) %></div>
                </div>
            </div>
            <hr class="my-2">
            <small class="text-muted">Cuenta <strong class="text-monospace"><%= EscapeHtml.texto(c.getNumeroCuenta()) %></strong>
                · abierta el <%= FormatoVista.fechaHora(c.getFechaCreacion()) %> por @<%= EscapeHtml.texto(c.getUsuarioRegistro()) %>
                <% if (!c.getObservacion().isEmpty()) { %> · <%= EscapeHtml.texto(c.getObservacion()) %><% } %></small>
        </div>
    </div>

    <%-- ===================== DETALLE ===================== --%>
    <div class="card" id="detalle">
        <div class="card-header"><h3 class="card-title"><i class="fas fa-list-ul mr-2"></i>Detalle</h3></div>
        <% if (c.getDetalles().isEmpty()) { %>
        <div class="estado-vacio py-4">
            <i class="fas fa-notes-medical text-muted"></i>
            <p class="mb-0"><strong>La cuenta está vacía.</strong> Agrega abajo la consulta, el análisis o el producto que se va a cobrar.</p>
        </div>
        <% } else { %>
        <div class="table-responsive">
        <table class="table table-sm mb-0 tabla-detalle">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Descripción</th>
                    <th scope="col" class="text-center" style="width:120px">Cant.</th>
                    <th scope="col" class="text-right d-none d-sm-table-cell">P. unit.</th>
                    <th scope="col" class="text-right d-none d-md-table-cell">Cobertura</th>
                    <th scope="col" class="text-right">Paga</th>
                    <% if (c.isPendiente()) { %><th scope="col"><span class="sr-only">Quitar</span></th><% } %>
                </tr>
            </thead>
            <tbody>
            <% for (DetalleCuentaDTO d : c.getDetalles()) {
                   BigDecimal cubierto = d.getMontoAseguradora().add(d.getMontoDescuento()); %>
                <tr>
                    <td>
                        <%= EscapeHtml.texto(d.getDescripcion()) %>
                        <div class="small text-muted"><span class="text-monospace"><%= EscapeHtml.texto(d.getCodigo()) %></span>
                            · <%= d.isProducto() ? "Producto" : "Servicio" %><%= d.isAfectoIgv() ? "" : " · exonerado de IGV" %></div>
                    </td>
                    <td class="text-center">
                        <% if (c.isPendiente()) { %>
                        <form method="POST" action="<%= url %>" class="form-cantidad d-inline-flex">
                            <input type="hidden" name="accion" value="cantidad">
                            <input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
                            <input type="hidden" name="idItem" value="<%= d.getIdProductoServicio() %>">
                            <label class="sr-only" for="cant<%= d.getIdProductoServicio() %>">Cantidad de <%= EscapeHtml.texto(d.getDescripcion()) %></label>
                            <input type="number" id="cant<%= d.getIdProductoServicio() %>" name="cantidad" min="1" max="99" value="<%= d.getCantidad() %>"
                                   class="form-control form-control-sm text-center" style="width:64px" data-auto-enviar>
                            <button type="submit" class="btn btn-sm btn-link px-1 boton-cantidad" title="Actualizar cantidad"><i class="fas fa-sync-alt"></i></button>
                        </form>
                        <% } else { %><%= d.getCantidad() %><% } %>
                    </td>
                    <td class="text-right d-none d-sm-table-cell monto"><%= s(d.getPrecioUnitario()) %></td>
                    <td class="text-right d-none d-md-table-cell monto <%= cubierto.signum() > 0 ? "text-success" : "text-muted" %>">
                        <%= cubierto.signum() > 0 ? "−" + s(cubierto) + " <small>(" + d.getPorcentajeCubierto().stripTrailingZeros().toPlainString() + " %)</small>" : "—" %></td>
                    <td class="text-right monto font-weight-bold"><%= s(d.getImporte()) %></td>
                    <% if (c.isPendiente()) { %>
                    <td class="text-right">
                        <form method="POST" action="<%= url %>">
                            <input type="hidden" name="accion" value="quitar">
                            <input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
                            <input type="hidden" name="idItem" value="<%= d.getIdProductoServicio() %>">
                            <button type="submit" class="btn btn-sm btn-outline-danger" title="Quitar" aria-label="Quitar <%= EscapeHtml.texto(d.getDescripcion()) %>"><i class="fas fa-times"></i></button>
                        </form>
                    </td>
                    <% } %>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
        <%-- Totales (RF-11): subtotal, IGV y total --%>
        <div class="card-body border-top">
            <dl class="row totales-cuenta mb-0">
                <% if (c.getMontoAseguradora().signum() > 0 || c.getMontoDescuento().signum() > 0) { %>
                <dt class="col-7 font-weight-normal text-muted">Precio de lista</dt><dd class="col-5 text-right monto text-muted"><%= s(c.getTotalLista()) %></dd>
                <% } %>
                <% if (c.getMontoAseguradora().signum() > 0) { %>
                <dt class="col-7 font-weight-normal text-success">Cubre la aseguradora (<%= EscapeHtml.texto(c.getNombreCobertura()) %>)</dt><dd class="col-5 text-right monto text-success">−<%= s(c.getMontoAseguradora()) %></dd>
                <% } %>
                <% if (c.getMontoDescuento().signum() > 0) { %>
                <dt class="col-7 font-weight-normal text-success">Descuento por convenio</dt><dd class="col-5 text-right monto text-success">−<%= s(c.getMontoDescuento()) %></dd>
                <% } %>
                <dt class="col-7 font-weight-normal">Op. gravada</dt><dd class="col-5 text-right monto"><%= s(c.getOpGravada()) %></dd>
                <% if (c.getOpExonerada().signum() > 0) { %>
                <dt class="col-7 font-weight-normal">Op. exonerada</dt><dd class="col-5 text-right monto"><%= s(c.getOpExonerada()) %></dd>
                <% } %>
                <dt class="col-7 font-weight-normal">IGV (<%= igvTexto %> %)</dt><dd class="col-5 text-right monto"><%= s(c.getIgv()) %></dd>
                <dt class="col-7 h5 mb-0 pt-2 border-top">Total a pagar</dt><dd class="col-5 h5 mb-0 pt-2 border-top text-right monto" id="totalCuenta">S/ <%= s(c.getTotal()) %></dd>
            </dl>
        </div>
        <% } %>

        <%-- Agregar servicio o producto: búsqueda en el tarifario + los más usados --%>
        <% if (c.isPendiente()) { %>
        <div class="card-footer agregar-item">
            <form method="POST" action="<%= url %>#detalle" id="formAgregar" data-url-items="<%= url %>?accion=items">
                <input type="hidden" name="accion" value="agregar">
                <input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
                <input type="hidden" name="idItem" id="idItem" value="">
                <label for="buscarItem" class="small text-muted mb-1">Agregar servicio o producto</label>
                <div class="form-row">
                    <div class="col-md-8 mb-2 mb-md-0 position-relative">
                        <div class="input-group">
                            <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                            <input type="search" id="buscarItem" class="form-control" autocomplete="off" maxlength="60"
                                   placeholder="Escribe nombre o código (mín. 2 letras)" <%= c.getDetalles().isEmpty() ? "autofocus" : "" %>
                                   role="combobox" aria-expanded="false" aria-controls="resultadosItem" aria-autocomplete="list">
                        </div>
                        <ul id="resultadosItem" class="list-group resultados-item shadow" role="listbox" hidden></ul>
                        <div id="itemElegido" class="small mt-1 text-success" aria-live="polite"></div>
                    </div>
                    <div class="col-4 col-md-2">
                        <label for="cantidad" class="sr-only">Cantidad</label>
                        <input type="number" id="cantidad" name="cantidad" class="form-control" min="1" max="99" value="1" required title="Cantidad">
                    </div>
                    <div class="col-8 col-md-2">
                        <button type="submit" class="btn btn-primary btn-block" id="botonAgregar" disabled><i class="fas fa-plus mr-1"></i> Agregar</button>
                    </div>
                </div>
            </form>
            <% if (frecuentes != null && !frecuentes.isEmpty()) { %>
            <div class="mt-2 frecuentes" aria-label="Más cobrados">
                <span class="small text-muted mr-1">Más cobrados:</span>
                <% for (ProductoServicioDTO f : frecuentes) { boolean agotado = f.getStock() != null && f.getStock() <= 0; %>
                <form method="POST" action="<%= url %>#detalle" class="d-inline">
                    <input type="hidden" name="accion" value="agregar"><input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
                    <input type="hidden" name="idItem" value="<%= f.getIdProductoServicio() %>"><input type="hidden" name="cantidad" value="1">
                    <button type="submit" class="btn btn-sm btn-outline-secondary chip-item mb-1" <%= agotado ? "disabled title=\"Agotado\"" : "" %>>
                        <i class="fas fa-plus fa-xs mr-1"></i><%= EscapeHtml.texto(f.getNombre()) %> <span class="text-muted">S/ <%= s(f.getPrecio()) %></span></button>
                </form>
                <% } %>
            </div>
            <% } %>
        </div>
        <% } %>
    </div>

    <%-- Intentos de pago rechazados (RF-14) --%>
    <% if (!c.getIntentosRechazados().isEmpty()) { %>
    <div class="card card-outline card-danger">
        <div class="card-header"><h3 class="card-title"><i class="fas fa-times-circle text-danger mr-2"></i>Pagos no aprobados (<%= c.getIntentosRechazados().size() %>)</h3></div>
        <ul class="list-group list-group-flush small">
            <% for (PagoDTO p : c.getIntentosRechazados()) { %>
            <li class="list-group-item">
                <strong><%= EscapeHtml.texto(p.getNombreMetodo()) %></strong> · <%= EscapeHtml.texto(p.getReferencia() == null ? "" : p.getReferencia()) %>
                · <span class="text-muted"><%= FormatoVista.fechaHora(p.getFechaCreacion()) %> · @<%= EscapeHtml.texto(p.getNombreCajero()) %></span>
                <div class="text-danger"><%= EscapeHtml.texto(p.getMotivoRechazo()) %></div>
            </li>
            <% } %>
        </ul>
    </div>
    <% } %>
</div>

<div class="col-lg-4">
<% if (cobrable) { %>
    <%-- ===================== COBRAR ===================== --%>
    <form method="POST" action="<%= url %>" id="formCobro" class="card card-primary card-outline" novalidate data-total="<%= c.getTotal().toPlainString() %>">
        <input type="hidden" name="accion" value="cobrar">
        <input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
        <div class="card-header"><h3 class="card-title"><i class="fas fa-hand-holding-usd mr-2"></i>Cobrar</h3></div>
        <div class="card-body">
            <% if (!errores.isEmpty()) { %>
            <div class="alert alert-danger py-2" role="alert"><i class="fas fa-exclamation-triangle mr-1"></i>No se cobró: revisa <%= errores.size() == 1 ? "el campo marcado" : "los campos marcados" %>.</div>
            <% } %>
            <% if (reporte != null) { %>
            <div class="alert alert-info py-2 reporte-cuenta" role="status">
                <strong><i class="fas fa-mobile-alt mr-1"></i>El paciente informó un pago</strong>
                <div class="small"><%= EscapeHtml.texto(reporte.getNombreBilletera()) %> · S/ <%= s(reporte.getMonto()) %>
                    <% if (!reporte.getNumeroOperacion().isEmpty()) { %> · Op. <span class="text-monospace"><%= EscapeHtml.texto(reporte.getNumeroOperacion()) %></span><% } %>
                    <% if (!reporte.getCelular().isEmpty()) { %> · Cel. <%= EscapeHtml.texto(reporte.getCelular()) %><% } %>
                    <% if (!reporte.getNombrePagador().isEmpty()) { %> · <%= EscapeHtml.texto(reporte.getNombrePagador()) %><% } %>
                    <br><span class="text-muted"><%= FormatoVista.fechaHora(reporte.getFechaCreacion()) %></span></div>
                <% if (!reporte.isMontoCoincide()) { %><div class="small text-danger mt-1">El monto no coincide con el total actual: recházalo o cobra con otro medio.</div><% } %>
                <div class="d-flex flex-wrap mt-2" style="gap:.4rem">
                    <button type="submit" form="formVerificar" class="btn btn-success btn-sm" <%= reporte.isMontoCoincide() ? "" : "disabled" %>><i class="fas fa-check mr-1"></i>Lo vi: confirmar</button>
                    <button type="button" class="btn btn-outline-danger btn-sm" data-toggle="collapse" data-target="#rechazoReporte" aria-expanded="false">No aparece</button>
                </div>
                <div class="collapse mt-2" id="rechazoReporte">
                    <label for="motivoReporte" class="small mb-1">Motivo para el paciente</label>
                    <input type="text" id="motivoReporte" name="motivo" form="formRechazar" class="form-control form-control-sm" minlength="5" maxlength="200"
                           value="No encontramos el pago en la app de la clínica">
                    <button type="submit" form="formRechazar" class="btn btn-danger btn-sm mt-1">Rechazar aviso</button>
                </div>
            </div>
            <% } %>
            <div class="text-center mb-3">
                <div class="small text-muted text-uppercase">Total a pagar</div>
                <div class="display-total monto">S/ <%= s(c.getTotal()) %></div>
            </div>

            <fieldset class="mb-3">
                <legend class="h6">¿Cómo paga?</legend>
                <div id="metodo" class="btn-group btn-group-toggle d-flex selector-metodo" data-toggle="buttons" role="radiogroup" aria-label="Medio de pago">
                    <% for (OpcionDTO m : metodos) {
                           String cod = m.getExtra();
                           String icono = "EFECTIVO".equals(cod) ? "fa-money-bill-wave" : "TARJETA".equals(cod) ? "fa-credit-card" : "fa-mobile-alt";
                           String corto = "EFECTIVO".equals(cod) ? "Efectivo" : "TARJETA".equals(cod) ? "Tarjeta" : "Billetera"; %>
                    <label class="btn btn-outline-primary flex-fill <%= cod.equals(metodoSel) ? "active" : "" %>" title="<%= EscapeHtml.texto(m.getNombre()) %>">
                        <input type="radio" name="metodo" value="<%= EscapeHtml.texto(cod) %>" <%= cod.equals(metodoSel) ? "checked" : "" %>>
                        <i class="fas <%= icono %> d-block mb-1"></i><span class="small"><%= corto %></span>
                    </label>
                    <% } %>
                </div>
                <%= msj(errores, "metodo") %>
            </fieldset>

            <div data-mostrar-si="metodo" data-valores="EFECTIVO">
                <div class="form-group">
                    <label for="montoRecibido">Paga con (S/)</label>
                    <input type="text" inputmode="decimal" id="montoRecibido" name="montoRecibido" class="form-control form-control-lg <%= inv(errores, "montoRecibido") %>"
                           placeholder="<%= c.getTotal().toPlainString() %>" autocomplete="off"
                           value="<%= sol.getMontoRecibido() == null ? "" : sol.getMontoRecibido().toPlainString() %>">
                    <%= msj(errores, "montoRecibido") %>
                    <div class="montos-rapidos mt-2" aria-label="Montos rápidos">
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="<%= c.getTotal().toPlainString() %>">Exacto</button>
                        <% for (int billete : new int[]{10, 20, 50, 100, 200}) { if (c.getTotal().compareTo(BigDecimal.valueOf(billete)) < 0) { %>
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="<%= billete %>">S/ <%= billete %></button>
                        <% } } %>
                    </div>
                    <div class="mt-2 vuelto" id="vuelto" aria-live="polite"></div>
                </div>
            </div>

            <div data-mostrar-si="metodo" data-valores="TARJETA">
                <div class="form-group">
                    <label for="tipoTarjeta">Tarjeta</label>
                    <select id="tipoTarjeta" name="tipoTarjeta" class="form-control <%= inv(errores, "tipoTarjeta") %>">
                        <option value="">— Elige —</option>
                        <option value="DEBITO" <%= "DEBITO".equals(sol.getTipoTarjeta()) ? "selected" : "" %>>Débito</option>
                        <option value="CREDITO" <%= "CREDITO".equals(sol.getTipoTarjeta()) ? "selected" : "" %>>Crédito</option>
                    </select>
                    <%= msj(errores, "tipoTarjeta") %>
                </div>
                <div class="form-row">
                    <div class="form-group col-5">
                        <label for="ultimosDigitos">Últimos 4</label>
                        <input type="text" id="ultimosDigitos" name="ultimosDigitos" maxlength="4" inputmode="numeric" data-solo-numeros autocomplete="off"
                               class="form-control text-monospace <%= inv(errores, "ultimosDigitos") %>" placeholder="1234" value="<%= EscapeHtml.texto(sol.getUltimosDigitos()) %>">
                        <%= msj(errores, "ultimosDigitos") %>
                    </div>
                    <div class="form-group col-7">
                        <label for="codigoAutorizacion">Cód. autorización</label>
                        <input type="text" id="codigoAutorizacion" name="codigoAutorizacion" maxlength="6" inputmode="numeric" data-solo-numeros autocomplete="off"
                               class="form-control text-monospace <%= inv(errores, "codigoAutorizacion") %>" placeholder="6 dígitos" value="<%= EscapeHtml.texto(sol.getCodigoAutorizacion()) %>">
                        <%= msj(errores, "codigoAutorizacion") %>
                    </div>
                </div>
                <p class="small text-muted"><i class="fas fa-shield-alt mr-1"></i>Cobra primero en el POS y copia los datos del voucher. Nunca se pide el número completo de la tarjeta.</p>
            </div>

            <div data-mostrar-si="metodo" data-valores="BILLETERA">
                <% if (cfgBilletera != null && (cfgBilletera.isTieneQr() || !cfgBilletera.getCelular().isEmpty())) { %>
                <div class="qr-caja text-center mb-3">
                    <% if (cfgBilletera.isTieneQr()) { %>
                    <img src="<%= ctx %>/qr-pago?v=<%= cfgBilletera.getVersionQr() %>" alt="QR de pago de la clínica" class="img-fluid qr-imagen" width="180" height="180" loading="lazy">
                    <% } %>
                    <% if (!cfgBilletera.getCelular().isEmpty()) { %><div class="mt-1"><span class="text-muted small">Celular</span> <strong class="text-monospace"><%= EscapeHtml.texto(cfgBilletera.getCelularLegible()) %></strong></div><% } %>
                    <% if (!cfgBilletera.getTitular().isEmpty()) { %><div class="small text-muted">a nombre de <%= EscapeHtml.texto(cfgBilletera.getTitular()) %></div><% } %>
                    <small class="d-block text-muted mt-1">Muestra este QR al paciente o gira la pantalla.</small>
                </div>
                <% } %>
                <fieldset class="form-group">
                    <legend class="col-form-label pt-0">Billetera</legend>
                    <div class="chips-billetera <%= inv(errores, "billetera") %>">
                        <% if (billeteras != null) for (OpcionDTO b : billeteras) { boolean sel = billeteraSel != null && billeteraSel == b.getId(); %>
                        <div class="custom-control custom-radio custom-control-inline mb-1">
                            <input type="radio" id="bil<%= b.getId() %>" name="idBilletera" value="<%= b.getId() %>" class="custom-control-input" <%= sel ? "checked" : "" %>>
                            <label class="custom-control-label" for="bil<%= b.getId() %>"><%= EscapeHtml.texto(b.getNombre()) %></label>
                        </div>
                        <% } %>
                    </div>
                    <%= msj(errores, "billetera") %>
                </fieldset>
                <div class="form-group">
                    <label for="numeroOperacion">N.º de operación <span class="text-muted small">(lo que se vea)</span></label>
                    <input type="text" id="numeroOperacion" name="numeroOperacion" maxlength="20" inputmode="numeric" data-solo-numeros autocomplete="off"
                           class="form-control text-monospace <%= inv(errores, "numeroOperacion") %>" placeholder="Ej. 4521 o el número completo" value="<%= EscapeHtml.texto(sol.getNumeroOperacion()) %>">
                    <small class="form-text text-muted">Si la app no muestra el número completo, escribe los dígitos visibles (mínimo 3) o el celular que pagó.</small>
                    <%= msj(errores, "numeroOperacion") %>
                </div>
                <div class="form-row">
                    <div class="form-group col-5">
                        <label for="celular">Celular</label>
                        <input type="tel" id="celular" name="celular" maxlength="9" inputmode="numeric" data-solo-numeros autocomplete="off"
                               class="form-control <%= inv(errores, "celular") %>" placeholder="9 dígitos" value="<%= EscapeHtml.texto(sol.getCelular()) %>">
                        <%= msj(errores, "celular") %>
                    </div>
                    <div class="form-group col-7">
                        <label for="nombrePagador">Nombre en la app</label>
                        <input type="text" id="nombrePagador" name="nombrePagador" maxlength="80" autocomplete="off"
                               class="form-control <%= inv(errores, "nombrePagador") %>" placeholder="Opcional" value="<%= EscapeHtml.texto(sol.getNombrePagador()) %>">
                        <%= msj(errores, "nombrePagador") %>
                    </div>
                </div>
            </div>

            <%-- Comprobante (Factory Method: boleta o factura) --%>
            <fieldset class="border-top pt-3">
                <legend class="h6">Comprobante</legend>
                <div id="tipoComprobante" class="mb-2">
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="compBoleta" name="tipoComprobante" value="BOLETA" class="custom-control-input" <%= factura ? "" : "checked" %>>
                        <label class="custom-control-label" for="compBoleta">Boleta</label>
                    </div>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="compFactura" name="tipoComprobante" value="FACTURA" class="custom-control-input" <%= factura ? "checked" : "" %>>
                        <label class="custom-control-label" for="compFactura">Factura (empresa)</label>
                    </div>
                </div>
                <div data-mostrar-si="tipoComprobante" data-valores="BOLETA">
                    <div class="form-row">
                        <div class="form-group col-5">
                            <label for="dni">DNI</label>
                            <input type="text" id="dni" name="dni" maxlength="8" inputmode="numeric" data-solo-numeros
                                   class="form-control <%= !factura ? inv(errores, "numeroDocumento") : "" %>" value="<%= EscapeHtml.texto(factura ? c.getDniPaciente() : cli.getNumeroDocumento()) %>">
                        </div>
                        <div class="form-group col-7">
                            <label for="nombreCliente">A nombre de</label>
                            <input type="text" id="nombreCliente" name="nombreCliente" maxlength="150"
                                   class="form-control <%= !factura ? inv(errores, "nombre") : "" %>" value="<%= EscapeHtml.texto(factura ? c.getNombrePaciente() : cli.getNombre()) %>">
                        </div>
                    </div>
                    <% if (!factura) { %><%= msj(errores, "numeroDocumento") %><%= msj(errores, "nombre") %><% } %>
                    <small class="form-text text-muted mt-n2 mb-2">El DNI es opcional en boletas menores a S/ <%= s(par.getMontoBoletaDni()) %>.</small>
                    <input type="hidden" name="direccionCliente" value="">
                </div>
                <div data-mostrar-si="tipoComprobante" data-valores="FACTURA">
                    <div class="form-group">
                        <label for="ruc">RUC</label>
                        <input type="text" id="ruc" name="ruc" maxlength="11" inputmode="numeric" data-solo-numeros
                               class="form-control <%= factura ? inv(errores, "numeroDocumento") : "" %>" placeholder="11 dígitos" value="<%= factura ? EscapeHtml.texto(cli.getNumeroDocumento()) : "" %>">
                        <% if (factura) { %><%= msj(errores, "numeroDocumento") %><% } %>
                    </div>
                    <div class="form-group">
                        <label for="razonSocial">Razón social</label>
                        <input type="text" id="razonSocial" name="razonSocial" maxlength="150"
                               class="form-control <%= factura ? inv(errores, "nombre") : "" %>" value="<%= factura ? EscapeHtml.texto(cli.getNombre()) : "" %>">
                        <% if (factura) { %><%= msj(errores, "nombre") %><% } %>
                    </div>
                    <div class="form-group">
                        <label for="direccionFiscal">Dirección fiscal</label>
                        <input type="text" id="direccionFiscal" name="direccionFiscal" maxlength="200"
                               class="form-control <%= factura ? inv(errores, "direccion") : "" %>" value="<%= factura ? EscapeHtml.texto(cli.getDireccion()) : "" %>">
                        <% if (factura) { %><%= msj(errores, "direccion") %><% } %>
                    </div>
                </div>
            </fieldset>
        </div>
        <div class="card-footer">
            <button type="submit" class="btn btn-success btn-lg btn-block" id="botonCobrar">
                <i class="fas fa-check mr-1"></i> Cobrar S/ <%= s(c.getTotal()) %></button>
        </div>
    </form>
    <% if (reporte != null) { %>
    <form method="POST" action="<%= url %>" id="formVerificar" data-un-envio><input type="hidden" name="accion" value="verificarReporte"><input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>"><input type="hidden" name="idReporte" value="<%= reporte.getIdReportePago() %>"></form>
    <form method="POST" action="<%= url %>" id="formRechazar"><input type="hidden" name="accion" value="rechazarReporte"><input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>"><input type="hidden" name="idReporte" value="<%= reporte.getIdReportePago() %>"></form>
    <% } %>
<% } else if (c.isPendiente()) { %>
    <div class="card"><div class="card-body text-muted small"><i class="fas fa-info-circle mr-1"></i>Cuando agregues al menos un servicio o producto aquí aparecerá el cobro.</div></div>
<% } %>

<% if (c.getPago() != null) { PagoDTO p = c.getPago(); %>
    <div class="card <%= p.isAprobado() ? "card-success" : "card-secondary" %> card-outline">
        <div class="card-header"><h3 class="card-title"><i class="fas fa-receipt mr-2"></i><%= p.isAprobado() ? "Pago registrado" : "Pago anulado" %></h3></div>
        <div class="card-body">
            <dl class="row mb-0 small">
                <dt class="col-5">Medio</dt><dd class="col-7"><%= EscapeHtml.texto(p.getNombreMetodo()) %></dd>
                <% if (p.getNombreBilletera() != null && !p.getNombreBilletera().isEmpty()) { %><dt class="col-5">Billetera</dt><dd class="col-7"><%= EscapeHtml.texto(p.getNombreBilletera()) %></dd><% } %>
                <% if (p.getReferencia() != null && !p.getReferencia().isEmpty()) { %><dt class="col-5">Referencia</dt><dd class="col-7"><%= EscapeHtml.texto(p.getReferencia()) %></dd><% } %>
                <% if (p.getNombrePagador() != null && !p.getNombrePagador().isEmpty()) { %><dt class="col-5">Pagó</dt><dd class="col-7"><%= EscapeHtml.texto(p.getNombrePagador()) %></dd><% } %>
                <% if (p.getMontoRecibido() != null) { %>
                <dt class="col-5">Recibido</dt><dd class="col-7 monto">S/ <%= s(p.getMontoRecibido()) %></dd>
                <dt class="col-5">Vuelto</dt><dd class="col-7 monto">S/ <%= s(p.getVuelto()) %></dd>
                <% } %>
                <dt class="col-5">Cobró</dt><dd class="col-7">@<%= EscapeHtml.texto(p.getNombreCajero()) %></dd>
                <dt class="col-5">Fecha</dt><dd class="col-7"><%= FormatoVista.fechaHora(p.getFechaCreacion()) %></dd>
            </dl>
            <% if (c.getIdComprobante() > 0) { %>
            <a href="<%= ctx %>/caja/comprobante?id=<%= c.getIdComprobante() %>" class="btn btn-outline-primary btn-block mt-3">
                <i class="fas fa-file-invoice mr-1"></i> Ver comprobante <span class="text-monospace"><%= c.getComprobante() %></span></a>
            <% } %>
        </div>
    </div>
<% } %>

<% if (c.isAnulada()) { %>
    <div class="card card-outline card-secondary">
        <div class="card-header"><h3 class="card-title"><i class="fas fa-ban mr-2"></i>Cuenta anulada</h3></div>
        <div class="card-body small">
            <p class="mb-1"><%= EscapeHtml.texto(c.getMotivoAnulacion()) %></p>
            <span class="text-muted">Autorizó @<%= EscapeHtml.texto(c.getUsuarioAnula()) %> · <%= FormatoVista.fechaHora(c.getFechaAnulacion()) %></span>
        </div>
    </div>
<% } else if (puedeAnular) { %>
    <%-- RF-16: anulación con autorización (solo administrador) y motivo obligatorio --%>
    <details class="card card-outline card-danger anular-cuenta">
        <summary class="card-header text-danger"><i class="fas fa-ban mr-2"></i>Anular esta cuenta</summary>
        <form method="POST" action="<%= url %>" class="card-body">
            <input type="hidden" name="accion" value="anular">
            <input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
            <p class="small text-muted"><%= c.isPagada() ? "Se anulan el pago y el comprobante y se devuelve el stock de los productos. Si la caja de ese día ya se cerró, no se podrá." : "La cuenta dejará de aparecer como pendiente." %></p>
            <div class="form-group">
                <label for="motivo">Motivo <span class="text-danger" aria-hidden="true">*</span></label>
                <textarea id="motivo" name="motivo" class="form-control" rows="2" minlength="10" maxlength="250" required
                          placeholder="Ej: el paciente se retiró sin atenderse"></textarea>
            </div>
            <button type="submit" class="btn btn-danger btn-block"><i class="fas fa-ban mr-1"></i> Confirmar anulación</button>
        </form>
    </details>
<% } %>
</div>
</div>
<script src="<%= ctx %>/js/caja.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
