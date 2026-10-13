<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.ProductoServicioDTO, modelo.dto.OpcionDTO, controlador.FormatoVista" %>
<%-- Formulario del TARIFARIO: un servicio médico o un producto con stock. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    ProductoServicioDTO p = (ProductoServicioDTO) request.getAttribute("itemForm");
    @SuppressWarnings("unchecked") List<OpcionDTO> categorias = (List<OpcionDTO>) request.getAttribute("categorias");
    @SuppressWarnings("unchecked") List<OpcionDTO> especialidades = (List<OpcionDTO>) request.getAttribute("especialidades");
    @SuppressWarnings("unchecked") List<OpcionDTO> marcas = (List<OpcionDTO>) request.getAttribute("marcas");
    @SuppressWarnings("unchecked") List<OpcionDTO> unidades = (List<OpcionDTO>) request.getAttribute("unidades");
    @SuppressWarnings("unchecked") List<OpcionDTO> proveedores = (List<OpcionDTO>) request.getAttribute("proveedores");
    boolean nuevo = p.isNuevo();
    String urlLista = ctx + "/admin/servicios";
%>

<form method="POST" action="<%= urlLista %>" id="formItem" data-form-crud novalidate>
    <input type="hidden" name="accion" value="guardar">
    <input type="hidden" name="id" value="<%= p.getIdProductoServicio() %>">

    <div class="row">
        <div class="col-lg-8">
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-tag mr-2"></i>Datos del ítem</h3></div>
                <div class="card-body">
                    <div class="form-group">
                        <span class="d-block mb-1 font-weight-bold">Tipo</span>
                        <div id="tipo" class="btn-group btn-group-toggle" data-toggle="buttons" role="radiogroup" aria-label="Tipo">
                            <label class="btn btn-outline-primary <%= p.isProducto() ? "" : "active" %>">
                                <input type="radio" name="tipo" value="SERVICIO" <%= marcado(!p.isProducto()) %>><i class="fas fa-stethoscope mr-1"></i> Servicio
                            </label>
                            <label class="btn btn-outline-primary <%= p.isProducto() ? "active" : "" %>">
                                <input type="radio" name="tipo" value="PRODUCTO" <%= marcado(p.isProducto()) %>><i class="fas fa-pills mr-1"></i> Producto
                            </label>
                        </div>
                        <small class="form-text text-muted">Servicio: consulta, análisis, imagen (sin stock). Producto: medicamento o insumo (con stock).</small>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="codigo">Código</label>
                            <input type="text" class="form-control text-uppercase <%= invalido(errores, "codigo") %>" id="codigo" name="codigo"
                                   maxlength="20" value="<%= EscapeHtml.texto(p.getCodigo()) %>" placeholder="Automático">
                            <small class="form-text text-muted">Déjalo vacío para generarlo.</small>
                            <%= error(errores, "codigo") %>
                        </div>
                        <div class="form-group col-md-8">
                            <label for="nombre">Nombre <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "nombre") %>" id="nombre" name="nombre"
                                   maxlength="120" required value="<%= EscapeHtml.texto(p.getNombre()) %>" placeholder="Ej: Consulta de Dermatología" <%= nuevo ? "autofocus" : "" %>>
                            <%= error(errores, "nombre") %>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="idCategoria">Categoría <span class="text-danger" aria-hidden="true">*</span></label>
                            <select class="form-control <%= invalido(errores, "idCategoria") %>" id="idCategoria" name="idCategoria" required data-filtrar-por="tipo">
                                <% for (OpcionDTO c : categorias) { %>
                                <option value="<%= c.getId() %>" data-extra="<%= c.getExtra() %>" <%= sel(p.getIdCategoria() == c.getId()) %>><%= EscapeHtml.texto(c.getNombre()) %></option>
                                <% } %>
                            </select>
                            <%= error(errores, "idCategoria") %>
                        </div>
                        <div class="form-group col-md-6" data-mostrar-si="tipo" data-valores="SERVICIO">
                            <label for="idEspecialidad">Especialidad <small class="text-muted">(si es una consulta)</small></label>
                            <select class="form-control" id="idEspecialidad" name="idEspecialidad">
                                <option value="">— Ninguna —</option>
                                <% for (OpcionDTO e : especialidades) { %>
                                <option value="<%= e.getId() %>" <%= sel(p.getIdEspecialidad() != null && p.getIdEspecialidad() == e.getId()) %>><%= EscapeHtml.texto(e.getNombre()) %></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="form-group col-md-6" data-mostrar-si="tipo" data-valores="PRODUCTO">
                            <label for="idMarca">Marca</label>
                            <select class="form-control" id="idMarca" name="idMarca">
                                <option value="">— Sin marca —</option>
                                <% for (OpcionDTO m : marcas) { %>
                                <option value="<%= m.getId() %>" <%= sel(p.getIdMarca() != null && p.getIdMarca() == m.getId()) %>><%= EscapeHtml.texto(m.getNombre()) %></option>
                                <% } %>
                            </select>
                        </div>
                    </div>
                    <div class="form-group mb-0">
                        <label for="descripcion">Descripción <small class="text-muted">(opcional)</small></label>
                        <textarea class="form-control <%= invalido(errores, "descripcion") %>" id="descripcion" name="descripcion" rows="2"
                                  maxlength="250" data-contador-de="contDesc" placeholder="Indicaciones, preparación del paciente, presentación..."><%= EscapeHtml.texto(p.getDescripcion()) %></textarea>
                        <small class="form-text text-muted"><span id="contDesc">0</span>/250</small>
                        <%= error(errores, "descripcion") %>
                    </div>
                </div>
            </div>

            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-coins mr-2"></i>Precio<span data-mostrar-si="tipo" data-valores="PRODUCTO"> y stock</span></h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="precio">Precio de venta <span class="text-danger" aria-hidden="true">*</span></label>
                            <div class="input-group">
                                <div class="input-group-prepend"><span class="input-group-text">S/</span></div>
                                <input type="text" class="form-control text-right <%= invalido(errores, "precio") %>" id="precio" name="precio" inputmode="decimal"
                                       maxlength="9" required value="<%= p.getPrecio() == null ? "" : p.getPrecio().toPlainString() %>" placeholder="0.00">
                            </div>
                            <%= error(errores, "precio") %>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="idUnidadMedida">Unidad <span class="text-danger" aria-hidden="true">*</span></label>
                            <select class="form-control <%= invalido(errores, "idUnidadMedida") %>" id="idUnidadMedida" name="idUnidadMedida">
                                <% for (OpcionDTO u : unidades) { %>
                                <option value="<%= u.getId() %>" <%= sel(p.getIdUnidadMedida() == u.getId()) %>><%= EscapeHtml.texto(u.getNombre()) %> (<%= EscapeHtml.texto(u.getExtra()) %>)</option>
                                <% } %>
                            </select>
                            <%= error(errores, "idUnidadMedida") %>
                        </div>
                        <div class="form-group col-md-4 d-flex align-items-end">
                            <div class="custom-control custom-switch mb-2">
                                <input type="checkbox" class="custom-control-input" id="afectoIgv" name="afectoIgv" <%= marcado(p.isAfectoIgv()) %>>
                                <label class="custom-control-label" for="afectoIgv">El precio incluye IGV (18 %)</label>
                            </div>
                        </div>
                    </div>
                    <div class="form-row" data-mostrar-si="tipo" data-valores="PRODUCTO">
                        <div class="form-group col-md-4">
                            <label for="stock">Stock actual <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="number" class="form-control <%= invalido(errores, "stock") %>" id="stock" name="stock" min="0" step="1"
                                   value="<%= p.getStock() == null ? "0" : p.getStock() %>">
                            <%= error(errores, "stock") %>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="stockMinimo">Stock mínimo</label>
                            <input type="number" class="form-control <%= invalido(errores, "stockMinimo") %>" id="stockMinimo" name="stockMinimo" min="0" step="1"
                                   value="<%= p.getStockMinimo() == null ? "0" : p.getStockMinimo() %>">
                            <small class="form-text text-muted">Al llegar aquí se marca «Reponer».</small>
                            <%= error(errores, "stockMinimo") %>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="idProveedor">Proveedor habitual</label>
                            <select class="form-control" id="idProveedor" name="idProveedor">
                                <option value="">— Ninguno —</option>
                                <% for (OpcionDTO pr : proveedores) { %>
                                <option value="<%= pr.getId() %>" <%= sel(p.getIdProveedor() != null && p.getIdProveedor() == pr.getId()) %>><%= EscapeHtml.texto(pr.getNombre()) %></option>
                                <% } %>
                            </select>
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
                    <span class="d-block mb-1 font-weight-bold">Estado</span>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoActivo" name="estado" value="Activo" class="custom-control-input" <%= marcado(p.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoActivo">Activo (se cobra en caja)</label>
                    </div>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoInactivo" name="estado" value="Inactivo" class="custom-control-input" <%= marcado(!p.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoInactivo">Desactivado</label>
                    </div>
                </div>
            </div>
            <% } %>
            <div class="callout callout-info small">
                <i class="fas fa-calculator mr-1"></i> En caja (Etapa 6) el precio se ajustará según la cobertura del paciente (particular, EPS, seguro o convenio).
            </div>
        </div>
    </div>

    <div class="barra-acciones">
        <a href="<%= urlLista %>" class="btn btn-outline-secondary">Cancelar</a>
        <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> <%= nuevo ? "Registrar" : "Guardar cambios" %></button>
    </div>
</form>

<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
