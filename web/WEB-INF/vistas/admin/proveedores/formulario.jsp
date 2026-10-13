<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.ProveedorDTO, controlador.FormatoVista" %>
<%-- Formulario de PROVEEDOR. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    ProveedorDTO p = (ProveedorDTO) request.getAttribute("proveedorForm");
    @SuppressWarnings("unchecked")
    List<String> rubros = (List<String>) request.getAttribute("rubros");
    boolean nuevo = p.isNuevo();
    String urlLista = ctx + "/admin/proveedores";
%>

<form method="POST" action="<%= urlLista %>" id="formProveedor" data-form-crud novalidate>
    <input type="hidden" name="accion" value="guardar">
    <input type="hidden" name="id" value="<%= p.getIdProveedor() %>">

    <div class="row">
        <div class="col-lg-8">
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-building mr-2"></i>Datos de la empresa</h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="ruc">RUC <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "ruc") %>" id="ruc" name="ruc" data-solo-numeros
                                   maxlength="11" inputmode="numeric" required value="<%= EscapeHtml.texto(p.getRuc()) %>" placeholder="11 números" <%= nuevo ? "autofocus" : "" %>>
                            <small class="form-text text-muted">Se verifica el dígito final como lo hace SUNAT.</small>
                            <%= error(errores, "ruc") %>
                        </div>
                        <div class="form-group col-md-8">
                            <label for="razonSocial">Razón social <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "razonSocial") %>" id="razonSocial" name="razonSocial"
                                   maxlength="150" required value="<%= EscapeHtml.texto(p.getRazonSocial()) %>" placeholder="Ej: Distribuidora Médica S.A.C.">
                            <%= error(errores, "razonSocial") %>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="nombreComercial">Nombre comercial <small class="text-muted">(opcional)</small></label>
                            <input type="text" class="form-control <%= invalido(errores, "nombreComercial") %>" id="nombreComercial" name="nombreComercial"
                                   maxlength="100" value="<%= EscapeHtml.texto(p.getNombreComercial()) %>" placeholder="Como se le conoce">
                            <%= error(errores, "nombreComercial") %>
                        </div>
                        <div class="form-group col-md-6">
                            <label for="rubro">Rubro <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "rubro") %>" id="rubro" name="rubro" list="listaRubros"
                                   maxlength="60" required value="<%= EscapeHtml.texto(p.getRubro()) %>" placeholder="Elige o escribe">
                            <datalist id="listaRubros"><% for (String r : rubros) { %><option value="<%= EscapeHtml.texto(r) %>"><% } %></datalist>
                            <%= error(errores, "rubro") %>
                        </div>
                    </div>
                    <div class="form-group mb-0">
                        <label for="direccion">Dirección <small class="text-muted">(opcional)</small></label>
                        <input type="text" class="form-control <%= invalido(errores, "direccion") %>" id="direccion" name="direccion"
                               maxlength="200" value="<%= EscapeHtml.texto(p.getDireccion()) %>" placeholder="Av. / Jr. / Calle, número, distrito">
                        <%= error(errores, "direccion") %>
                    </div>
                </div>
            </div>
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-address-book mr-2"></i>Contacto para pedidos</h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-5">
                            <label for="contactoNombre">Persona de contacto</label>
                            <input type="text" class="form-control <%= invalido(errores, "contactoNombre") %>" id="contactoNombre" name="contactoNombre"
                                   maxlength="100" value="<%= EscapeHtml.texto(p.getContactoNombre()) %>" placeholder="Nombre y apellido">
                            <%= error(errores, "contactoNombre") %>
                        </div>
                        <div class="form-group col-md-3">
                            <label for="telefono">Teléfono</label>
                            <input type="tel" class="form-control <%= invalido(errores, "telefono") %>" id="telefono" name="telefono" data-solo-numeros
                                   maxlength="9" inputmode="numeric" value="<%= EscapeHtml.texto(p.getTelefono()) %>" placeholder="Fijo o celular">
                            <%= error(errores, "telefono") %>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="correo">Correo</label>
                            <input type="email" class="form-control <%= invalido(errores, "correo") %>" id="correo" name="correo"
                                   maxlength="100" value="<%= EscapeHtml.texto(p.getCorreo()) %>" placeholder="ventas@empresa.com">
                            <%= error(errores, "correo") %>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-4">
            <% if (!nuevo) { %>
            <div class="card">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-info-circle mr-2"></i>Información</h3></div>
                <div class="card-body">
                    <p class="small text-muted">Registrado el <%= FormatoVista.fecha(p.getFechaCreacion()) %><% if (p.getUsuarioRegistro() != null) { %> por <%= EscapeHtml.texto(p.getUsuarioRegistro()) %><% } %>.</p>
                    <p class="small">Provee <strong><%= p.getProductos() %></strong> servicio(s) o producto(s) activos.</p>
                    <span class="d-block mb-1 font-weight-bold">Estado</span>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoActivo" name="estado" value="Activo" class="custom-control-input" <%= marcado(p.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoActivo">Activo</label>
                    </div>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoInactivo" name="estado" value="Inactivo" class="custom-control-input" <%= marcado(!p.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoInactivo">Desactivado</label>
                    </div>
                </div>
            </div>
            <% } else { %>
            <div class="callout callout-info small"><i class="fas fa-lightbulb mr-1"></i>Después podrás asignar este proveedor a los productos del tarifario (medicamentos e insumos).</div>
            <% } %>
        </div>
    </div>

    <div class="barra-acciones">
        <a href="<%= urlLista %>" class="btn btn-outline-secondary">Cancelar</a>
        <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> <%= nuevo ? "Registrar proveedor" : "Guardar cambios" %></button>
    </div>
</form>

<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
