<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="modelo.dto.ParametroClinicaDTO" %>
<%-- RF-22: datos de la clínica que salen en boletas y facturas, IGV y reglas de las citas. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    ParametroClinicaDTO p = (ParametroClinicaDTO) request.getAttribute("parametrosForm");
    String n = p.getIgvPorcentaje() == null ? "" : p.getIgvPorcentaje().stripTrailingZeros().toPlainString();
    String m = p.getMontoBoletaDni() == null ? "" : p.getMontoBoletaDni().toPlainString();
%>
<form method="POST" action="<%= ctx %>/admin/parametros" id="formParametros" data-form-crud novalidate>
<div class="row">
    <div class="col-lg-8">
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-hospital mr-2"></i>Empresa emisora de los comprobantes</h3></div>
            <div class="card-body">
                <div class="form-row">
                    <div class="form-group col-md-4">
                        <label for="ruc">RUC <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" id="ruc" name="ruc" maxlength="11" inputmode="numeric" data-solo-numeros required
                               class="form-control <%= invalido(errores, "ruc") %>" value="<%= EscapeHtml.texto(p.getRuc()) %>">
                        <%= error(errores, "ruc") %>
                    </div>
                    <div class="form-group col-md-8">
                        <label for="razonSocial">Razón social <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" id="razonSocial" name="razonSocial" maxlength="150" required
                               class="form-control <%= invalido(errores, "razonSocial") %>" value="<%= EscapeHtml.texto(p.getRazonSocial()) %>">
                        <%= error(errores, "razonSocial") %>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="nombreComercial">Nombre comercial <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" id="nombreComercial" name="nombreComercial" maxlength="100" required
                               class="form-control <%= invalido(errores, "nombreComercial") %>" value="<%= EscapeHtml.texto(p.getNombreComercial()) %>">
                        <%= error(errores, "nombreComercial") %>
                    </div>
                    <div class="form-group col-md-6">
                        <label for="telefono">Teléfono</label>
                        <input type="tel" id="telefono" name="telefono" maxlength="20"
                               class="form-control <%= invalido(errores, "telefono") %>" value="<%= EscapeHtml.texto(p.getTelefono()) %>">
                        <%= error(errores, "telefono") %>
                    </div>
                </div>
                <div class="form-group mb-0">
                    <label for="direccion">Dirección <span class="text-danger" aria-hidden="true">*</span></label>
                    <input type="text" id="direccion" name="direccion" maxlength="200" required
                           class="form-control <%= invalido(errores, "direccion") %>" value="<%= EscapeHtml.texto(p.getDireccion()) %>">
                    <%= error(errores, "direccion") %>
                </div>
            </div>
        </div>
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-percent mr-2"></i>Impuestos y comprobantes</h3></div>
            <div class="card-body">
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="igvPorcentaje">IGV (%) <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" inputmode="decimal" id="igvPorcentaje" name="igvPorcentaje" required
                               class="form-control <%= invalido(errores, "igvPorcentaje") %>" value="<%= n %>">
                        <small class="form-text text-muted">Los precios del tarifario ya incluyen el IGV. Cambiarlo solo afecta las cuentas que se modifiquen desde ahora.</small>
                        <%= error(errores, "igvPorcentaje") %>
                    </div>
                    <div class="form-group col-md-6">
                        <label for="montoBoletaDni">Boleta con DNI obligatorio desde (S/) <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" inputmode="decimal" id="montoBoletaDni" name="montoBoletaDni" required
                               class="form-control <%= invalido(errores, "montoBoletaDni") %>" value="<%= m %>">
                        <small class="form-text text-muted">Regla de SUNAT: S/ 700.00.</small>
                        <%= error(errores, "montoBoletaDni") %>
                    </div>
                </div>
            </div>
        </div>
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-calendar-check mr-2"></i>Reglas de las citas</h3></div>
            <div class="card-body">
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="horasCancelacionPaciente">El paciente puede cancelar o cambiar su cita hasta (horas antes) <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="number" min="1" max="168" id="horasCancelacionPaciente" name="horasCancelacionPaciente" required
                               class="form-control <%= invalido(errores, "horasCancelacionPaciente") %>" value="<%= p.getHorasCancelacionPaciente() %>">
                        <small class="form-text text-muted">Regla usual en las clínicas de Lima: 24 horas. Después de ese plazo, el cambio se hace en recepción.</small>
                        <%= error(errores, "horasCancelacionPaciente") %>
                    </div>
                    <div class="form-group col-md-6">
                        <label for="diasMaximosReserva">Se puede reservar con hasta (días de anticipación) <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="number" min="7" max="365" id="diasMaximosReserva" name="diasMaximosReserva" required
                               class="form-control <%= invalido(errores, "diasMaximosReserva") %>" value="<%= p.getDiasMaximosReserva() %>">
                        <small class="form-text text-muted">Evita que se separen turnos con meses de anticipación.</small>
                        <%= error(errores, "diasMaximosReserva") %>
                    </div>
                </div>
            </div>
            <div class="card-footer text-right">
                <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> Guardar</button>
            </div>
        </div>
    </div>
    <div class="col-lg-4">
        <%@ include file="/WEB-INF/vistas/comun/historial.jspf" %>
    </div>
</div>
</form>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
