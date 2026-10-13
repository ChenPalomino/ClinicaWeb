<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.util.Map, modelo.dto.UsuarioDTO, modelo.entidades.Rol, controlador.FormatoVista" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    // Clase CSS y mensaje de error de Bootstrap para un campo
    private String invalido(Map<String, String> e, String campo) { return e.containsKey(campo) ? "is-invalid" : ""; }
    private String error(Map<String, String> e, String campo) {
        return e.containsKey(campo) ? "<div class=\"invalid-feedback d-block\">" + EscapeHtml.texto(e.get(campo)) + "</div>" : "";
    }
%>
<%
    UsuarioDTO u = (UsuarioDTO) request.getAttribute("usuarioForm");
    @SuppressWarnings("unchecked")
    Map<String, String> errores = (Map<String, String>) request.getAttribute("errores");
    @SuppressWarnings("unchecked")
    List<Rol> roles = (List<Rol>) request.getAttribute("roles");
    boolean nuevo = u.isNuevo();
    boolean esYo = !nuevo && u.getIdUsuario() == usuarioSesion.getIdUsuario();
%>

<% if (!errores.isEmpty()) { %>
<div class="alert alert-danger" role="alert">
    <i class="fas fa-exclamation-triangle mr-2"></i>
    <% if (errores.containsKey("general")) { %><%= EscapeHtml.texto(errores.get("general")) %>
    <% } else { %>No se guardó: revisa <%= errores.size() == 1 ? "el campo marcado" : "los " + errores.size() + " campos marcados" %> en rojo.<% } %>
</div>
<% } %>

<form method="POST" action="<%= ctx %>/admin/usuarios" id="formUsuario" novalidate>
    <input type="hidden" name="accion" value="guardar">
    <input type="hidden" name="id" value="<%= u.getIdUsuario() %>">

    <div class="row">
        <div class="col-lg-8">
            <%-- ---------- Datos personales ---------- --%>
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-id-card mr-2"></i>Datos personales</h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="nombres">Nombres <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "nombres") %>" id="nombres" name="nombres"
                                   maxlength="100" required autocomplete="off" value="<%= EscapeHtml.texto(u.getNombres()) %>" placeholder="Ej: Juan Carlos">
                            <%= error(errores, "nombres") %>
                        </div>
                        <div class="form-group col-md-6">
                            <label for="apellidos">Apellidos <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "apellidos") %>" id="apellidos" name="apellidos"
                                   maxlength="100" required autocomplete="off" value="<%= EscapeHtml.texto(u.getApellidos()) %>" placeholder="Ej: Ramírez Torres">
                            <%= error(errores, "apellidos") %>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-4">
                            <label for="dni">DNI <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="text" class="form-control <%= invalido(errores, "dni") %>" id="dni" name="dni" data-solo-numeros
                                   maxlength="8" inputmode="numeric" required value="<%= EscapeHtml.texto(u.getDni()) %>" placeholder="8 números">
                            <%= error(errores, "dni") %>
                        </div>
                        <div class="form-group col-md-5">
                            <label for="correo">Correo electrónico <span class="text-danger" aria-hidden="true">*</span></label>
                            <input type="email" class="form-control <%= invalido(errores, "correo") %>" id="correo" name="correo"
                                   maxlength="100" required value="<%= EscapeHtml.texto(u.getCorreo()) %>" placeholder="nombre@clinica.pe">
                            <small class="form-text text-muted">Aquí llegarán los códigos para recuperar la contraseña.</small>
                            <%= error(errores, "correo") %>
                        </div>
                        <div class="form-group col-md-3">
                            <label for="celular">Celular <small class="text-muted">(opcional)</small></label>
                            <input type="tel" class="form-control <%= invalido(errores, "celular") %>" id="celular" name="celular" data-solo-numeros
                                   maxlength="9" inputmode="numeric" value="<%= EscapeHtml.texto(u.getCelular()) %>" placeholder="9XXXXXXXX">
                            <%= error(errores, "celular") %>
                        </div>
                    </div>
                </div>
            </div>

            <%-- ---------- Cuenta de acceso ---------- --%>
            <div class="card card-outline card-primary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-user-shield mr-2"></i>Cuenta de acceso</h3></div>
                <div class="card-body">
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="nombreUsuario">Nombre de usuario <span class="text-danger" aria-hidden="true">*</span></label>
                            <div class="input-group">
                                <div class="input-group-prepend"><span class="input-group-text">@</span></div>
                                <input type="text" class="form-control <%= invalido(errores, "nombreUsuario") %>" id="nombreUsuario" name="nombreUsuario"
                                       maxlength="30" required autocomplete="off" autocapitalize="none" spellcheck="false"
                                       value="<%= EscapeHtml.texto(u.getNombreUsuario()) %>" placeholder="Ej: jramirez">
                            </div>
                            <small class="form-text text-muted">Con esto inicia sesión (también puede usar su DNI). Para pacientes se recomienda usar el DNI.</small>
                            <%= error(errores, "nombreUsuario") %>
                        </div>
                        <div class="form-group col-md-6">
                            <label for="idRol">Rol <span class="text-danger" aria-hidden="true">*</span></label>
                            <select class="form-control <%= invalido(errores, "idRol") %>" id="idRol" name="idRol" required <%= esYo ? "disabled" : "" %>>
                                <% for (Rol r : roles) { %>
                                <option value="<%= r.getIdRol() %>" data-descripcion="<%= EscapeHtml.texto(r.getDescripcion()) %>"
                                        <%= r.getIdRol() == u.getIdRol() ? "selected" : "" %>><%= EscapeHtml.texto(r.getNombre()) %></option>
                                <% } %>
                            </select>
                            <% if (esYo) { %>
                                <input type="hidden" name="idRol" value="<%= u.getIdRol() %>">
                                <small class="form-text text-muted">No puedes cambiar tu propio rol.</small>
                            <% } else { %>
                                <small class="form-text text-muted" id="descripcionRol"></small>
                            <% } %>
                            <%= error(errores, "idRol") %>
                        </div>
                    </div>
                    <div class="form-group mb-0">
                        <span class="d-block mb-1 font-weight-bold">Estado</span>
                        <div class="custom-control custom-radio custom-control-inline">
                            <input type="radio" id="estadoActivo" name="estado" value="Activo" class="custom-control-input" <%= u.isActivo() ? "checked" : "" %>>
                            <label class="custom-control-label font-weight-normal" for="estadoActivo">Activo (puede iniciar sesión)</label>
                        </div>
                        <div class="custom-control custom-radio custom-control-inline">
                            <input type="radio" id="estadoInactivo" name="estado" value="Inactivo" class="custom-control-input" <%= u.isActivo() ? "" : "checked" %> <%= esYo ? "disabled" : "" %>>
                            <label class="custom-control-label font-weight-normal" for="estadoInactivo">Desactivado</label>
                        </div>
                        <%= error(errores, "estado") %>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            <%-- ---------- Contraseña ---------- --%>
            <div class="card card-outline card-secondary">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-key mr-2"></i><%= nuevo ? "Contraseña" : "Restablecer contraseña" %></h3></div>
                <div class="card-body">
                    <% if (!nuevo) { %>
                    <p class="small text-muted">Déjalo vacío para mantener la contraseña actual.</p>
                    <% } %>
                    <div class="form-group">
                        <label for="password"><%= nuevo ? "Contraseña" : "Nueva contraseña" %> <% if (nuevo) { %><span class="text-danger" aria-hidden="true">*</span><% } %></label>
                        <div class="input-group">
                            <input type="password" class="form-control <%= invalido(errores, "password") %>" id="password" name="password"
                                   maxlength="64" autocomplete="new-password" <%= nuevo ? "required" : "" %>>
                            <div class="input-group-append">
                                <button type="button" class="btn btn-outline-secondary" data-toggle-password="password" title="Mostrar u ocultar" aria-label="Mostrar contraseña"><i class="fas fa-eye"></i></button>
                            </div>
                        </div>
                        <ul class="lista-requisitos small mt-2 mb-0" aria-label="Requisitos de la contraseña">
                            <li data-regla="longitud">Mínimo 8 caracteres</li>
                            <li data-regla="mayuscula">Una mayúscula</li>
                            <li data-regla="minuscula">Una minúscula</li>
                            <li data-regla="numero">Un número</li>
                        </ul>
                        <%= error(errores, "password") %>
                    </div>
                    <div class="form-group mb-0">
                        <label for="confirmarPassword">Repetir contraseña</label>
                        <input type="password" class="form-control <%= invalido(errores, "confirmarPassword") %>" id="confirmarPassword"
                               name="confirmarPassword" maxlength="64" autocomplete="new-password">
                        <%= error(errores, "confirmarPassword") %>
                    </div>
                </div>
            </div>

            <% if (!nuevo) { %>
            <%-- ---------- Información de la cuenta (solo lectura) ---------- --%>
            <div class="card">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-info-circle mr-2"></i>Información</h3></div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0 small">
                        <tr><th class="pl-3">Creado</th><td><%= FormatoVista.fecha(u.getFechaCreacion()) %><% if (u.getUsuarioRegistro() != null) { %> por <%= EscapeHtml.texto(u.getUsuarioRegistro()) %><% } %></td></tr>
                        <tr><th class="pl-3">Último acceso</th><td><%= FormatoVista.fechaHora(u.getUltimoAcceso()) %></td></tr>
                        <tr><th class="pl-3">Bloqueo</th><td>
                            <% if (u.isBloqueado()) { %><span class="text-danger"><i class="fas fa-lock mr-1"></i><%= EscapeHtml.texto(FormatoVista.motivoBloqueo(u.getMotivoBloqueo())) %></span>
                            <% } else { %>No está bloqueado<% } %></td></tr>
                    </table>
                </div>
            </div>
            <% } %>
        </div>
    </div>

    <div class="barra-acciones">
        <a href="<%= ctx %>/admin/usuarios" class="btn btn-outline-secondary">Cancelar</a>
        <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> <%= nuevo ? "Crear usuario" : "Guardar cambios" %></button>
    </div>
</form>

<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
