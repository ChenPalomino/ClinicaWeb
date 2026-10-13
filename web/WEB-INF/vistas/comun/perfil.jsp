<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map, java.util.List, modelo.dto.OpcionDTO" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    private String invalido(Map<String, String> e, String campo) { return e.containsKey(campo) ? "is-invalid" : ""; }
    private String error(Map<String, String> e, String campo) {
        return e.containsKey(campo) ? "<div class=\"invalid-feedback d-block\">" + EscapeHtml.texto(e.get(campo)) + "</div>" : "";
    }
%>
<%
    PerfilDTO p = (PerfilDTO) request.getAttribute("perfilForm");
    if (p == null) p = perfilSesion;
    @SuppressWarnings("unchecked")
    Map<String, String> errores = (Map<String, String>) request.getAttribute("errores");
    String tab = (String) request.getAttribute("tab");
    @SuppressWarnings("unchecked")
    List<OpcionDTO> sexos = (List<OpcionDTO>) request.getAttribute("sexos");
    if (sexos == null) sexos = List.of();
    String[][] pestanas = {
        {"datos", "fas fa-id-card", "Datos personales"},
        {"foto", "fas fa-camera", "Foto de perfil"},
        {"seguridad", "fas fa-key", "Contraseña"},
        {"apariencia", "fas fa-palette", "Apariencia"}
    };
%>

<div class="row">
    <%-- ---------- Tarjeta de presentación ---------- --%>
    <div class="col-lg-4 animar">
        <div class="card card-perfil">
            <div class="card-perfil-portada"></div>
            <div class="card-body text-center pt-0">
                <div class="card-perfil-avatar">
                    <%= avatar(ctx, usuarioSesion.getIdUsuario(), perfilSesion.isTieneFoto(), perfilSesion.getFotoVersion(), perfilSesion.getIniciales(), "avatar-xxl") %>
                </div>
                <h4 class="mb-0 mt-2"><%= EscapeHtml.texto(perfilSesion.getNombreCompleto()) %></h4>
                <p class="text-muted mb-2">@<%= EscapeHtml.texto(usuarioSesion.getNombreUsuario()) %></p>
                <span class="badge badge-primary px-2 py-1"><%= usuarioSesion.getNombreRol() %></span>
                <% if (!perfilSesion.getDescripcion().isBlank()) { %>
                <p class="descripcion-perfil mt-3 mb-0"><%= EscapeHtml.texto(perfilSesion.getDescripcion()) %></p>
                <% } else { %>
                <p class="text-muted small mt-3 mb-0"><i class="fas fa-info-circle mr-1"></i>Agrega una descripción corta para que los demás te conozcan.</p>
                <% } %>
            </div>
            <ul class="list-group list-group-flush small">
                <li class="list-group-item"><i class="fas fa-id-badge fa-fw text-muted mr-2"></i>DNI <span class="float-right"><%= perfilSesion.getDni().isBlank() ? "—" : EscapeHtml.texto(perfilSesion.getDni()) %></span></li>
                <li class="list-group-item"><i class="fas fa-envelope fa-fw text-muted mr-2"></i><span class="text-break"><%= perfilSesion.getCorreo().isBlank() ? "Sin correo" : EscapeHtml.texto(perfilSesion.getCorreo()) %></span></li>
                <li class="list-group-item"><i class="fas fa-mobile-alt fa-fw text-muted mr-2"></i><%= perfilSesion.getCelular().isBlank() ? "Sin celular" : EscapeHtml.texto(perfilSesion.getCelular()) %></li>
                <li class="list-group-item"><i class="fas fa-birthday-cake fa-fw text-muted mr-2"></i>Edad <span class="float-right"><%= perfilSesion.getEdad() == null ? "Sin registrar" : perfilSesion.getEdad() + " años" %></span></li>
                <% if (perfilSesion.isPaciente()) { %>
                <li class="list-group-item"><i class="fas fa-folder-open fa-fw text-muted mr-2"></i>Historia clínica <span class="float-right"><%= EscapeHtml.texto(perfilSesion.getNumeroHistoria()) %></span></li>
                <li class="list-group-item"><i class="fas fa-shield-alt fa-fw text-muted mr-2"></i>Cobertura <span class="float-right"><%= EscapeHtml.texto(perfilSesion.getNombreCobertura()) %></span></li>
                <% } %>
            </ul>
        </div>
    </div>

    <%-- ---------- Pestañas ---------- --%>
    <div class="col-lg-8 animar">
        <div class="card card-primary card-outline card-outline-tabs">
            <div class="card-header p-0 border-bottom-0">
                <ul class="nav nav-tabs" role="tablist">
                    <% for (String[] t : pestanas) { boolean activa = t[0].equals(tab); %>
                    <li class="nav-item">
                        <a class="nav-link <%= activa ? "active" : "" %>" id="tab-<%= t[0] %>" data-toggle="pill" href="#panel-<%= t[0] %>"
                           role="tab" aria-controls="panel-<%= t[0] %>" aria-selected="<%= activa %>">
                            <i class="<%= t[1] %> mr-1"></i><span class="d-none d-sm-inline"><%= t[2] %></span>
                        </a>
                    </li>
                    <% } %>
                </ul>
            </div>
            <div class="card-body">
                <div class="tab-content">

                    <%-- ===== Datos personales ===== --%>
                    <div class="tab-pane fade <%= "datos".equals(tab) ? "show active" : "" %>" id="panel-datos" role="tabpanel" aria-labelledby="tab-datos">
                        <% if (!errores.isEmpty() && "datos".equals(tab)) { %>
                        <div class="alert alert-danger"><i class="fas fa-exclamation-triangle mr-2"></i>No se guardó: revisa los campos marcados en rojo.</div>
                        <% } %>
                        <form method="POST" action="<%= ctx %>/perfil" id="formPerfilDatos" novalidate>
                            <input type="hidden" name="accion" value="datos">
                            <div class="form-row">
                                <div class="form-group col-md-6">
                                    <label for="nombres">Nombres <span class="text-danger" aria-hidden="true">*</span></label>
                                    <input type="text" class="form-control <%= invalido(errores, "nombres") %>" id="nombres" name="nombres" maxlength="100" value="<%= EscapeHtml.texto(p.getNombres()) %>" autocomplete="given-name">
                                    <%= error(errores, "nombres") %>
                                </div>
                                <div class="form-group col-md-6">
                                    <label for="apellidos">Apellidos <span class="text-danger" aria-hidden="true">*</span></label>
                                    <input type="text" class="form-control <%= invalido(errores, "apellidos") %>" id="apellidos" name="apellidos" maxlength="100" value="<%= EscapeHtml.texto(p.getApellidos()) %>" autocomplete="family-name">
                                    <%= error(errores, "apellidos") %>
                                </div>
                            </div>
                            <div class="form-row">
                                <div class="form-group col-md-4">
                                    <label for="dni">DNI</label>
                                    <% if (perfilSesion.isTieneDatosPersonales()) { %>
                                    <input type="text" class="form-control" id="dni" value="<%= EscapeHtml.texto(perfilSesion.getDni()) %>" readonly>
                                    <small class="form-text text-muted">Para corregirlo, pídeselo al administrador.</small>
                                    <% } else { %>
                                    <input type="text" class="form-control <%= invalido(errores, "dni") %>" id="dni" name="dni" maxlength="8" inputmode="numeric" data-solo-numeros value="<%= EscapeHtml.texto(p.getDni()) %>" placeholder="8 números">
                                    <%= error(errores, "dni") %>
                                    <% } %>
                                </div>
                                <div class="form-group col-md-5">
                                    <label for="correo">Correo electrónico <span class="text-danger" aria-hidden="true">*</span></label>
                                    <input type="email" class="form-control <%= invalido(errores, "correo") %>" id="correo" name="correo" maxlength="100" value="<%= EscapeHtml.texto(p.getCorreo()) %>" autocomplete="email">
                                    <small class="form-text text-muted">Si lo cambias, avisaremos a tu correo anterior por seguridad.</small>
                                    <%= error(errores, "correo") %>
                                </div>
                                <div class="form-group col-md-3">
                                    <label for="celular">Celular</label>
                                    <input type="tel" class="form-control <%= invalido(errores, "celular") %>" id="celular" name="celular" maxlength="9" inputmode="numeric" data-solo-numeros value="<%= EscapeHtml.texto(p.getCelular()) %>" placeholder="9XXXXXXXX" autocomplete="tel-national">
                                    <%= error(errores, "celular") %>
                                </div>
                            </div>
                            <div class="form-row">
                                <div class="form-group col-md-4">
                                    <label for="idSexo">Sexo <small class="text-muted">(opcional)</small></label>
                                    <select class="form-control" id="idSexo" name="idSexo">
                                        <option value="">— Prefiero no indicarlo —</option>
                                        <% for (OpcionDTO s : sexos) { %>
                                        <option value="<%= s.getId() %>" <%= p.getIdSexo() != null && p.getIdSexo() == s.getId() ? "selected" : "" %>><%= EscapeHtml.texto(s.getNombre()) %></option>
                                        <% } %>
                                    </select>
                                </div>
                                <div class="form-group col-md-5">
                                    <label for="fechaNacimiento">Fecha de nacimiento <small class="text-muted">(opcional)</small></label>
                                    <input type="date" class="form-control <%= invalido(errores, "fechaNacimiento") %>" id="fechaNacimiento" name="fechaNacimiento"
                                           max="<%= java.time.LocalDate.now() %>" value="<%= p.getFechaNacimiento() == null ? "" : p.getFechaNacimiento() %>" autocomplete="bday">
                                    <small class="form-text text-muted">Edad: <strong data-edad-de="fechaNacimiento"></strong><%= perfilSesion.isPaciente() ? " · La ve admisión al atenderte." : "" %></small>
                                    <%= error(errores, "fechaNacimiento") %>
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="descripcion">Sobre mí <small class="text-muted">(opcional)</small></label>
                                <textarea class="form-control <%= invalido(errores, "descripcion") %>" id="descripcion" name="descripcion" rows="3" maxlength="250"
                                          data-contador-de="contadorDescripcion"
                                          placeholder="<%= usuarioSesion.getRolId() == 3 ? "Ej: Médico internista con 10 años de experiencia. Atiendo lunes a viernes." : "Ej: Me gusta que me recuerden mis citas un día antes." %>"><%= EscapeHtml.texto(p.getDescripcion()) %></textarea>
                                <small class="form-text text-muted text-right"><span id="contadorDescripcion">0</span>/250</small>
                                <%= error(errores, "descripcion") %>
                            </div>
                            <div class="text-right">
                                <button type="submit" class="btn btn-primary"><i class="fas fa-save mr-1"></i> Guardar cambios</button>
                            </div>
                        </form>
                    </div>

                    <%-- ===== Foto de perfil ===== --%>
                    <div class="tab-pane fade <%= "foto".equals(tab) ? "show active" : "" %>" id="panel-foto" role="tabpanel" aria-labelledby="tab-foto">
                        <div class="row align-items-center">
                            <div class="col-md-4 text-center mb-3">
                                <div class="vista-foto" id="vistaFoto">
                                    <%= avatar(ctx, usuarioSesion.getIdUsuario(), perfilSesion.isTieneFoto(), perfilSesion.getFotoVersion(), perfilSesion.getIniciales(), "avatar-xxl") %>
                                </div>
                                <small class="text-muted d-block mt-2">Vista previa</small>
                            </div>
                            <div class="col-md-8">
                                <form method="POST" action="<%= ctx %>/perfil" id="formFoto">
                                    <input type="hidden" name="accion" value="foto">
                                    <input type="hidden" name="foto" id="fotoDatos">
                                    <label class="zona-archivo" for="archivoFoto" id="zonaArchivo">
                                        <i class="fas fa-cloud-upload-alt"></i>
                                        <strong>Elige una imagen</strong> o arrástrala aquí
                                        <small class="d-block text-muted">JPG, PNG o WEBP · máximo 5 MB. Se recorta en cuadrado automáticamente.</small>
                                    </label>
                                    <input type="file" id="archivoFoto" accept="image/jpeg,image/png,image/webp" class="d-none">
                                    <small class="text-danger d-block mt-1" id="errorFoto" role="alert"></small>
                                    <div class="mt-3">
                                        <button type="submit" class="btn btn-primary" id="btnGuardarFoto" disabled><i class="fas fa-save mr-1"></i> Guardar foto</button>
                                        <% if (perfilSesion.isTieneFoto()) { %>
                                        <button type="button" class="btn btn-outline-danger" data-confirmar="quitarFoto" data-id="0" data-usuario="<%= EscapeHtml.texto(usuarioSesion.getNombreUsuario()) %>"
                                                data-titulo="Quitar foto de perfil" data-boton="Sí, quitar" data-color="btn-danger"
                                                data-mensaje="¿Quitar tu foto de perfil? En su lugar se mostrarán tus iniciales.">
                                            <i class="fas fa-trash-alt mr-1"></i> Quitar foto
                                        </button>
                                        <% } %>
                                    </div>
                                </form>
                                <p class="small text-muted mt-3 mb-0"><i class="fas fa-shield-alt mr-1"></i>Una foto clara de tu rostro genera confianza en pacientes y colegas.</p>
                            </div>
                        </div>
                    </div>

                    <%-- ===== Contraseña ===== --%>
                    <div class="tab-pane fade <%= "seguridad".equals(tab) ? "show active" : "" %>" id="panel-seguridad" role="tabpanel" aria-labelledby="tab-seguridad">
                        <form method="POST" action="<%= ctx %>/perfil" id="formPassword" novalidate class="form-estrecho">
                            <input type="hidden" name="accion" value="password">
                            <div class="form-group">
                                <label for="actual">Contraseña actual</label>
                                <input type="password" class="form-control <%= invalido(errores, "actual") %>" id="actual" name="actual" autocomplete="current-password" maxlength="64">
                                <%= error(errores, "actual") %>
                            </div>
                            <div class="form-group">
                                <label for="password">Nueva contraseña</label>
                                <div class="input-group">
                                    <input type="password" class="form-control <%= invalido(errores, "nueva") %>" id="password" name="nueva" autocomplete="new-password" maxlength="64">
                                    <div class="input-group-append">
                                        <button type="button" class="btn btn-outline-secondary" data-toggle-password="password" aria-label="Mostrar contraseña"><i class="fas fa-eye"></i></button>
                                    </div>
                                </div>
                                <ul class="lista-requisitos small mt-2 mb-0">
                                    <li data-regla="longitud">Mínimo 8 caracteres</li>
                                    <li data-regla="mayuscula">Una mayúscula</li>
                                    <li data-regla="minuscula">Una minúscula</li>
                                    <li data-regla="numero">Un número</li>
                                </ul>
                                <%= error(errores, "nueva") %>
                            </div>
                            <div class="form-group">
                                <label for="confirmar">Repetir nueva contraseña</label>
                                <input type="password" class="form-control <%= invalido(errores, "confirmar") %>" id="confirmar" name="confirmar" autocomplete="new-password" maxlength="64">
                                <%= error(errores, "confirmar") %>
                            </div>
                            <p class="small text-muted"><i class="fas fa-envelope mr-1"></i>Te enviaremos un aviso al correo, con un enlace por si no fuiste tú.</p>
                            <button type="submit" class="btn btn-primary"><i class="fas fa-key mr-1"></i> Cambiar contraseña</button>
                        </form>
                    </div>

                    <%-- ===== Apariencia ===== --%>
                    <div class="tab-pane fade <%= "apariencia".equals(tab) ? "show active" : "" %>" id="panel-apariencia" role="tabpanel" aria-labelledby="tab-apariencia">
                        <p class="mb-3">Elige cómo quieres ver el sistema. Se guarda en tu cuenta: lo verás igual en cualquier computadora.</p>
                        <div class="row" role="radiogroup" aria-label="Tema">
                            <div class="col-sm-4 mb-3">
                                <label class="opcion-tema" for="temaClaro">
                                    <input type="radio" name="tema" id="temaClaro" value="claro">
                                    <span class="muestra-tema muestra-claro"><span></span><span></span></span>
                                    <strong>Claro</strong><small>Ideal de día y en ambientes iluminados</small>
                                </label>
                            </div>
                            <div class="col-sm-4 mb-3">
                                <label class="opcion-tema" for="temaOscuro">
                                    <input type="radio" name="tema" id="temaOscuro" value="oscuro">
                                    <span class="muestra-tema muestra-oscuro"><span></span><span></span></span>
                                    <strong>Oscuro</strong><small>Descansa la vista de noche</small>
                                </label>
                            </div>
                            <div class="col-sm-4 mb-3">
                                <label class="opcion-tema" for="temaSistema">
                                    <input type="radio" name="tema" id="temaSistema" value="sistema">
                                    <span class="muestra-tema muestra-sistema"><span></span><span></span></span>
                                    <strong>Automático</strong><small>Igual que tu dispositivo</small>
                                </label>
                            </div>
                        </div>
                        <p class="small text-muted mb-0"><i class="fas fa-lightbulb mr-1"></i>También puedes cambiarlo rápido con el botón <i class="fas fa-moon"></i> de la barra superior.</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- La ventana de confirmación (Quitar foto) envía a /perfil --%>
<div id="datosConfirmar" data-action="<%= ctx %>/perfil" data-regreso="" hidden></div>

<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
