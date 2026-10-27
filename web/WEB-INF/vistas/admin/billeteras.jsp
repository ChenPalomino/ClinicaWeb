<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.BilleteraConfigDTO, modelo.dto.OpcionDTO" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    BilleteraConfigDTO cfg = (BilleteraConfigDTO) request.getAttribute("config");
    @SuppressWarnings("unchecked") List<OpcionDTO> billeteras = (List<OpcionDTO>) request.getAttribute("billeteras");
    String url = ctx + "/admin/billeteras";
%>
<div class="row">
    <div class="col-lg-5">
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-qrcode mr-2"></i>QR de pago</h3></div>
            <div class="card-body text-center">
                <% if (cfg.isTieneQr()) { %>
                <div class="qr-caja d-inline-block mb-2"><img src="<%= ctx %>/qr-pago?v=<%= cfg.getVersionQr() %>" alt="QR de pago actual" class="img-fluid qr-imagen" width="220" height="220"></div>
                <p class="small text-muted">Se muestra en caja al elegir «Billetera» y en el portal del paciente al pagar una cita.</p>
                <% } else { %>
                <div class="estado-vacio py-3"><i class="fas fa-qrcode text-muted"></i>
                    <p class="mb-0"><strong>Aún no hay QR.</strong> Los pacientes solo verán el celular.</p></div>
                <% } %>
                <form method="POST" action="<%= url %>" id="formQr" class="text-left">
                    <input type="hidden" name="accion" value="qr">
                    <input type="hidden" name="qr" id="qrDatos">
                    <div class="custom-file mb-2">
                        <input type="file" class="custom-file-input" id="archivoQr" accept="image/png,image/jpeg,image/webp">
                        <label class="custom-file-label" for="archivoQr"><%= cfg.isTieneQr() ? "Cambiar imagen del QR" : "Elegir imagen del QR" %></label>
                    </div>
                    <div id="vistaQr" class="text-center"></div>
                    <div id="errorQr" class="text-danger small" role="alert"></div>
                    <small class="form-text text-muted">PNG, JPG o WEBP. Recorta la captura para que se vea solo el QR. Máximo <%= request.getAttribute("qrMaximoKb") %> KB después de reducirla.</small>
                    <button type="submit" id="btnGuardarQr" class="btn btn-primary btn-block mt-2" disabled><i class="fas fa-upload mr-1"></i>Guardar QR</button>
                </form>
                <% if (cfg.isTieneQr()) { %>
                <details class="mt-2 rechazo-reporte">
                    <summary class="btn btn-link text-danger btn-sm">Quitar QR</summary>
                    <form method="POST" action="<%= url %>" class="small">
                        <input type="hidden" name="accion" value="quitarQr">
                        <p class="mb-1">Los pacientes ya no podrán escanearlo; solo verán el celular.</p>
                        <button type="submit" class="btn btn-danger btn-sm">Sí, quitar QR</button>
                    </form>
                </details>
                <% } %>
            </div>
        </div>
    </div>
    <div class="col-lg-7">
        <form method="POST" action="<%= url %>" class="card card-outline card-primary" novalidate>
            <input type="hidden" name="accion" value="datos">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-mobile-alt mr-2"></i>Datos para transferir</h3></div>
            <div class="card-body">
                <div class="form-row">
                    <div class="form-group col-md-5">
                        <label for="celular">Celular de la clínica</label>
                        <input type="tel" id="celular" name="celular" maxlength="11" inputmode="numeric" placeholder="9 dígitos"
                               class="form-control text-monospace <%= invalido(errores, "celular") %>" value="<%= EscapeHtml.texto(cfg.getCelular()) %>">
                        <%= error(errores, "celular") %>
                    </div>
                    <div class="form-group col-md-7">
                        <label for="titular">Titular que ve el paciente</label>
                        <input type="text" id="titular" name="titular" maxlength="80" placeholder="Ej. Clínica Modelo San Martín"
                               class="form-control <%= invalido(errores, "titular") %>" value="<%= EscapeHtml.texto(cfg.getTitular()) %>">
                        <%= error(errores, "titular") %>
                    </div>
                </div>
                <p class="small text-muted mb-0"><i class="fas fa-info-circle mr-1"></i>El paciente compara este nombre con el que le muestra su app antes de pagar.</p>
            </div>
            <div class="card-footer text-right"><button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i>Guardar</button></div>
        </form>

        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-wallet mr-2"></i>Billeteras aceptadas</h3></div>
            <ul class="list-group list-group-flush">
                <% for (OpcionDTO b : billeteras) { boolean activa = "Activo".equals(b.getExtra()); %>
                <li class="list-group-item d-flex align-items-center">
                    <span class="mr-auto <%= activa ? "" : "text-muted" %>"><%= EscapeHtml.texto(b.getNombre()) %>
                        <% if (!activa) { %><span class="badge badge-secondary ml-1">Oculta</span><% } %></span>
                    <form method="POST" action="<%= url %>">
                        <input type="hidden" name="accion" value="estado"><input type="hidden" name="id" value="<%= b.getId() %>">
                        <input type="hidden" name="activar" value="<%= activa ? "0" : "1" %>">
                        <button type="submit" class="btn btn-sm <%= activa ? "btn-outline-secondary" : "btn-outline-success" %>"><%= activa ? "Ocultar" : "Mostrar" %></button>
                    </form>
                </li>
                <% } %>
            </ul>
            <div class="card-footer">
                <form method="POST" action="<%= url %>" class="form-inline">
                    <input type="hidden" name="accion" value="agregar">
                    <label for="nombreBilletera" class="sr-only">Nueva billetera</label>
                    <input type="text" id="nombreBilletera" name="nombre" class="form-control form-control-sm mr-2 mb-1" maxlength="40" minlength="2" required placeholder="Ej. Tunki">
                    <button type="submit" class="btn btn-sm btn-outline-primary mb-1"><i class="fas fa-plus mr-1"></i>Agregar billetera</button>
                </form>
                <small class="text-muted">Las ocultas no se ofrecen al cobrar, pero los pagos antiguos conservan su nombre.</small>
            </div>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
