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
    @SuppressWarnings("unchecked") List<ProductoServicioDTO> items = (List<ProductoServicioDTO>) request.getAttribute("items");
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

        <%-- Agregar servicio o producto --%>
        <% if (c.isPendiente() && items != null) { %>
        <div class="card-footer">
            <form method="POST" action="<%= url %>#detalle" class="form-row align-items-end" id="formAgregar">
                <input type="hidden" name="accion" value="agregar">
                <input type="hidden" name="id" value="<%= c.getIdCuentaAtencion() %>">
                <div class="col-md-8 mb-2 mb-md-0">
                    <label for="idItem" class="small text-muted mb-1">Agregar servicio o producto</label>
                    <select id="idItem" name="idItem" class="form-control" required <%= c.getDetalles().isEmpty() ? "autofocus" : "" %>>
                        <option value="">— Elige del tarifario —</option>
                        <% String grupo = "";
                           for (ProductoServicioDTO it : items) {
                               if (!it.getTipo().equals(grupo)) {
                                   if (!grupo.isEmpty()) { %></optgroup><% }
                                   grupo = it.getTipo(); %><optgroup label="<%= "SERVICIO".equals(grupo) ? "Servicios" : "Productos" %>"><%
                               }
                               boolean agotado = it.getStock() != null && it.getStock() <= 0; %>
                            <option value="<%= it.getIdProductoServicio() %>" <%= agotado ? "disabled" : "" %>>
                                <%= EscapeHtml.texto(it.getNombre()) %> — S/ <%= s(it.getPrecio()) %><%= it.getStock() != null ? (agotado ? " (agotado)" : " (quedan " + it.getStock() + ")") : "" %></option>
                        <% } if (!grupo.isEmpty()) { %></optgroup><% } %>
                    </select>
                </div>
                <div class="col-4 col-md-2">
                    <label for="cantidad" class="small text-muted mb-1">Cantidad</label>
                    <input type="number" id="cantidad" name="cantidad" class="form-control" min="1" max="99" value="1" required>
                </div>
                <div class="col-8 col-md-2">
                    <button type="submit" class="btn btn-outline-primary btn-block"><i class="fas fa-plus mr-1"></i> Agregar</button>
                </div>
            </form>
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
                           String corto = "EFECTIVO".equals(cod) ? "Efectivo" : "TARJETA".equals(cod) ? "Tarjeta" : "Yape / Plin"; %>
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
                <div class="form-group">
                    <span class="d-block mb-1">Aplicación</span>
                    <div class="<%= inv(errores, "aplicacion") %>">
                        <div class="custom-control custom-radio custom-control-inline">
                            <input type="radio" id="appYape" name="aplicacion" value="YAPE" class="custom-control-input" <%= !"PLIN".equals(sol.getAplicacion()) ? "checked" : "" %>>
                            <label class="custom-control-label" for="appYape">Yape</label>
                        </div>
                        <div class="custom-control custom-radio custom-control-inline">
                            <input type="radio" id="appPlin" name="aplicacion" value="PLIN" class="custom-control-input" <%= "PLIN".equals(sol.getAplicacion()) ? "checked" : "" %>>
                            <label class="custom-control-label" for="appPlin">Plin</label>
                        </div>
                    </div>
                    <%= msj(errores, "aplicacion") %>
                </div>
                <div class="form-group">
                    <label for="numeroOperacion">N.º de operación</label>
                    <input type="text" id="numeroOperacion" name="numeroOperacion" maxlength="12" inputmode="numeric" data-solo-numeros autocomplete="off"
                           class="form-control text-monospace <%= inv(errores, "numeroOperacion") %>" placeholder="Lo ves en el celular de la clínica" value="<%= EscapeHtml.texto(sol.getNumeroOperacion()) %>">
                    <%= msj(errores, "numeroOperacion") %>
                </div>
                <div class="form-group">
                    <label for="celular">Celular que pagó</label>
                    <input type="tel" id="celular" name="celular" maxlength="9" inputmode="numeric" data-solo-numeros autocomplete="off"
                           class="form-control <%= inv(errores, "celular") %>" placeholder="9 dígitos" value="<%= EscapeHtml.texto(sol.getCelular()) %>">
                    <%= msj(errores, "celular") %>
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
<% } else if (c.isPendiente()) { %>
    <div class="card"><div class="card-body text-muted small"><i class="fas fa-info-circle mr-1"></i>Cuando agregues al menos un servicio o producto aquí aparecerá el cobro.</div></div>
<% } %>

<% if (c.getPago() != null) { PagoDTO p = c.getPago(); %>
    <div class="card <%= p.isAprobado() ? "card-success" : "card-secondary" %> card-outline">
        <div class="card-header"><h3 class="card-title"><i class="fas fa-receipt mr-2"></i><%= p.isAprobado() ? "Pago registrado" : "Pago anulado" %></h3></div>
        <div class="card-body">
            <dl class="row mb-0 small">
                <dt class="col-5">Medio</dt><dd class="col-7"><%= EscapeHtml.texto(p.getNombreMetodo()) %></dd>
                <% if (p.getReferencia() != null) { %><dt class="col-5">Referencia</dt><dd class="col-7"><%= EscapeHtml.texto(p.getReferencia()) %></dd><% } %>
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
