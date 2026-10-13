<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, modelo.dto.PacienteDTO, modelo.dto.OpcionDTO, controlador.FormatoVista" %>
<%-- Formulario de PACIENTE. RF-01: DNI, datos de contacto y cobertura. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%@ include file="/WEB-INF/vistas/comun/formulario.jspf" %>
<%
    PacienteDTO p = (PacienteDTO) request.getAttribute("pacienteForm");
    @SuppressWarnings("unchecked")
    List<OpcionDTO> coberturas = (List<OpcionDTO>) request.getAttribute("coberturas");
    @SuppressWarnings("unchecked")
    List<OpcionDTO> sexos = (List<OpcionDTO>) request.getAttribute("sexos");
    boolean nuevo = p.isNuevo();
    String urlLista = ctx + "/recepcion/pacientes";
%>

<form method="POST" action="<%= urlLista %>" id="formPaciente" data-form-crud novalidate>
    <input type="hidden" name="accion" value="guardar">
    <input type="hidden" name="id" value="<%= p.getIdPaciente() %>">
    <input type="hidden" name="idDatosPersonales" value="<%= p.getIdDatosPersonales() %>">
    <input type="hidden" name="numeroHistoria" value="<%= EscapeHtml.texto(p.getNumeroHistoria()) %>">

    <div class="row">
        <div class="col-lg-8">
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-id-card mr-2"></i>Datos del paciente</h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="dni">DNI <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "dni") %>" id="dni" name="dni" data-solo-numeros
                                   maxlength="8" inputmode="numeric" required value="<%= EscapeHtml.texto(p.getDni()) %>" placeholder="8 números" <%= nuevo && p.getDni().isEmpty() ? "autofocus" : "" %>>
                            <%= error(errores, "dni") %>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="nombres">Nombres <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "nombres") %>" id="nombres" name="nombres"
                                   maxlength="100" required autocomplete="off" value="<%= EscapeHtml.texto(p.getNombres()) %>" placeholder="Ej: Ana María" <%= nuevo && !p.getDni().isEmpty() ? "autofocus" : "" %>>
                            <%= error(errores, "nombres") %>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="apellidos">Apellidos <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "apellidos") %>" id="apellidos" name="apellidos"
                                   maxlength="100" required autocomplete="off" value="<%= EscapeHtml.texto(p.getApellidos()) %>" placeholder="Ej: Torres Quispe">
                            <%= error(errores, "apellidos") %>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="idSexo">Sexo <small class="text-muted">(opcional)</small></label>
                            <select class="form-control" id="idSexo" name="idSexo">
                                <option value="">— Sin indicar —</option>
                                <% for (OpcionDTO s : sexos) { %>
                                <option value="<%= s.getId() %>" <%= sel(p.getIdSexo() != null && p.getIdSexo() == s.getId()) %>><%= EscapeHtml.texto(s.getNombre()) %></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="form-group col-md-4">
                            <label for="fechaNacimiento">Fecha de nacimiento <small class="text-muted">(opcional)</small></label>
                            <input type="date" class="form-control <%= invalido(errores, "fechaNacimiento") %>" id="fechaNacimiento" name="fechaNacimiento"
                                   max="<%= java.time.LocalDate.now() %>" value="<%= p.getFechaNacimiento() == null ? "" : p.getFechaNacimiento() %>">
                            <small class="form-text text-muted">Edad: <strong data-edad-de="fechaNacimiento"></strong></small>
                            <%= error(errores, "fechaNacimiento") %>
                        </div>
                    </div>
                    <h6 class="text-muted text-uppercase small font-weight-bold mt-2">Contacto <small class="text-lowercase">(al menos uno, para avisarle de sus citas)</small></h6>
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="celular">Celular</label>
                            <input type="tel" class="form-control <%= invalido(errores, "celular") %>" id="celular" name="celular" data-solo-numeros
                                   maxlength="9" inputmode="numeric" value="<%= EscapeHtml.texto(p.getCelular()) %>" placeholder="9XXXXXXXX">
                            <%= error(errores, "celular") %>
                        </div>
                        <div class="form-group col-md-8">
                            <label for="correo">Correo electrónico</label>
                            <input type="email" class="form-control <%= invalido(errores, "correo") %>" id="correo" name="correo"
                                   maxlength="100" value="<%= EscapeHtml.texto(p.getCorreo()) %>" placeholder="nombre@correo.com">
                            <%= error(errores, "correo") %>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-shield-alt mr-2"></i>Cobertura y alertas</h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="idCobertura">Cobertura <span class="text-danger" aria-hidden="true">*</span></label>
                            <select class="form-control <%= invalido(errores, "idCobertura") %>" id="idCobertura" name="idCobertura" required>
                                <% for (OpcionDTO c : coberturas) { %>
                                <option value="<%= c.getId() %>" data-extra="<%= c.getExtra() %>" <%= sel(p.getIdCobertura() == c.getId()) %>><%= EscapeHtml.texto(c.getNombre()) %></option>
                                <% } %>
                            </select>
                            <small class="form-text text-muted">Se usará para calcular cuánto paga el paciente en caja.</small>
                            <%= error(errores, "idCobertura") %>
                        </div>
                        <div class="form-group col-md-6" data-mostrar-si="idCobertura" data-valores="EPS,SEGURO,CONVENIO">
                            <label for="numeroAfiliacion">N.º de afiliación o póliza <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "numeroAfiliacion") %>" id="numeroAfiliacion" name="numeroAfiliacion"
                                   maxlength="30" autocomplete="off" value="<%= EscapeHtml.texto(p.getNumeroAfiliacion()) %>" placeholder="Como figura en su carné">
                            <%= error(errores, "numeroAfiliacion") %>
                        </div>
                    </div>
                    <div class="form-group mb-0">
                        <label for="observaciones">Alergias u observaciones <small class="text-muted">(opcional)</small></label>
                        <textarea class="form-control <%= invalido(errores, "observaciones") %>" id="observaciones" name="observaciones" rows="2"
                                  maxlength="250" data-contador-de="contObs" placeholder="Ej: alérgico a la penicilina"><%= EscapeHtml.texto(p.getObservaciones()) %></textarea>
                        <small class="form-text text-muted"><span id="contObs">0</span>/250 · Se mostrará como alerta en la lista de pacientes.</small>
                        <%= error(errores, "observaciones") %>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="card">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-folder-open mr-2"></i>Historia clínica</h3></div>
                <div class="card-body">
                    <% if (nuevo) { %>
                    <p class="mb-0 text-muted small"><i class="fas fa-magic mr-1"></i>El número de historia se genera automáticamente al guardar (HC-000001, HC-000002…).</p>
                    <% } else { %>
                    <p class="h4 mb-1"><%= EscapeHtml.texto(p.getNumeroHistoria()) %></p>
                    <p class="small text-muted mb-2">Registrado el <%= FormatoVista.fecha(p.getFechaCreacion()) %><% if (p.getUsuarioRegistro() != null) { %> por <%= EscapeHtml.texto(p.getUsuarioRegistro()) %><% } %></p>
                    <p class="small mb-3"><% if (p.isTieneCuenta()) { %><i class="fas fa-globe text-success mr-1"></i>Tiene cuenta en el portal del paciente.
                       <% } else { %><i class="fas fa-info-circle text-muted mr-1"></i>Aún no tiene cuenta: puede crearla en «Crear cuenta» con su DNI.<% } %></p>
                    <span class="d-block mb-1 font-weight-bold">Estado</span>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoActivo" name="estado" value="Activo" class="custom-control-input" <%= marcado(p.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoActivo">Activo</label>
                    </div>
                    <div class="custom-control custom-radio custom-control-inline">
                        <input type="radio" id="estadoInactivo" name="estado" value="Inactivo" class="custom-control-input" <%= marcado(!p.isActivo()) %>>
                        <label class="custom-control-label font-weight-normal" for="estadoInactivo">Desactivado</label>
                    </div>
                    <% } %>
                </div>
            </div>
            <div class="callout callout-info small">
                <i class="fas fa-user-shield mr-1"></i> Los datos personales se tratan según la Ley N.º 29733 de Protección de Datos Personales.
            </div>
        </div>
    </div>

    <div class="barra-acciones">
        <a href="<%= urlLista %>" class="btn btn-outline-secondary">Cancelar</a>
        <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> <%= nuevo ? "Registrar paciente" : "Guardar cambios" %></button>
    </div>
</form>

<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
