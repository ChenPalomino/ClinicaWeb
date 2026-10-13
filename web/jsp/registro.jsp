<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map, java.util.Collections, modelo.dto.RegistroPacienteDTO, seguridad.EscapeHtml" %>
<%
    @SuppressWarnings("unchecked")
    Map<String, String> errores = (Map<String, String>) request.getAttribute("errores");
    if (errores == null) errores = Collections.emptyMap();
    RegistroPacienteDTO d = (RegistroPacienteDTO) request.getAttribute("datos");
    String vDni = d == null ? "" : EscapeHtml.texto(d.getDni());
    String vNom = d == null ? "" : EscapeHtml.texto(d.getNombres());
    String vApe = d == null ? "" : EscapeHtml.texto(d.getApellidos());
    String vCel = d == null ? "" : EscapeHtml.texto(d.getCelular());
    String vCor = d == null ? "" : EscapeHtml.texto(d.getCorreo());
%>
<%!
    // Devuelve la clase CSS y el mensaje de error de un campo
    private String clase(Map<String, String> e, String campo) { return e.containsKey(campo) ? "invalido" : ""; }
    private String msg(Map<String, String> e, String campo) { return e.containsKey(campo) ? EscapeHtml.texto(e.get(campo)) : ""; }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Crear cuenta - Clínica Modelo San Martín</title>
    <script src="${pageContext.request.contextPath}/js/pestana.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css?v=<%= configuracion.VersionAplicacion.RECURSOS %>">
</head>
<body>
<div class="main-wrapper">
    <div class="image-panel">
        <img src="https://images.unsplash.com/photo-1532938911079-1b06ac7ceec7?auto=format&fit=crop&w=600&q=80" alt="Atención médica">
        <h3>Portal de Pacientes</h3>
        <p>Crea tu cuenta en un minuto para reservar y revisar tus citas médicas.</p>
    </div>

    <div class="form-panel">
        <h2>Crear cuenta de paciente</h2>
        <p class="subtitulo">Tu DNI será tu usuario para iniciar sesión. Todos los campos son obligatorios.</p>

        <% if (errores.containsKey("general")) { %>
            <div class="alert-error"><%= msg(errores, "general") %></div>
        <% } else if (!errores.isEmpty()) { %>
            <div class="alert-error">Revisa los campos marcados en rojo.</div>
        <% } %>

        <form id="formRegistro" action="${pageContext.request.contextPath}/RegistroController" method="POST" novalidate>
            <label for="dni">DNI</label>
            <input type="text" id="dni" name="dni" maxlength="8" inputmode="numeric" placeholder="8 números, ej: 74839201"
                   autocomplete="username" class="<%= clase(errores, "dni") %>" value="<%= vDni %>">
            <span class="error-campo" id="error-dni"><%= msg(errores, "dni") %></span>

            <div class="fila">
                <div>
                    <label for="nombres">Nombres</label>
                    <input type="text" id="nombres" name="nombres" maxlength="80" placeholder="Ej: Juan Carlos"
                           autocomplete="given-name" class="<%= clase(errores, "nombres") %>" value="<%= vNom %>">
                    <span class="error-campo" id="error-nombres"><%= msg(errores, "nombres") %></span>
                </div>
                <div>
                    <label for="apellidos">Apellidos</label>
                    <input type="text" id="apellidos" name="apellidos" maxlength="80" placeholder="Ej: Ramírez Torres"
                           autocomplete="family-name" class="<%= clase(errores, "apellidos") %>" value="<%= vApe %>">
                    <span class="error-campo" id="error-apellidos"><%= msg(errores, "apellidos") %></span>
                </div>
            </div>

            <div class="fila">
                <div>
                    <label for="celular">Celular</label>
                    <input type="tel" id="celular" name="celular" maxlength="9" inputmode="numeric" placeholder="Ej: 912345678"
                           autocomplete="tel-national" class="<%= clase(errores, "celular") %>" value="<%= vCel %>">
                    <span class="error-campo" id="error-celular"><%= msg(errores, "celular") %></span>
                </div>
                <div>
                    <label for="correo">Correo electrónico</label>
                    <input type="email" id="correo" name="correo" maxlength="80" placeholder="nombre@gmail.com"
                           autocomplete="email" class="<%= clase(errores, "correo") %>" value="<%= vCor %>">
                    <span class="error-campo" id="error-correo"><%= msg(errores, "correo") %></span>
                </div>
            </div>

            <label for="password">Contraseña</label>
            <div class="password-wrapper">
                <input type="password" id="password" name="password" maxlength="64" placeholder="Crea una contraseña segura"
                       autocomplete="new-password" class="<%= clase(errores, "password") %>">
                <button type="button" class="toggle-password" data-target="password">Mostrar</button>
            </div>
            <ul class="requisitos" aria-label="Requisitos de la contraseña">
                <li id="req-longitud">Mínimo 8 caracteres</li>
                <li id="req-mayuscula">Una letra mayúscula</li>
                <li id="req-minuscula">Una letra minúscula</li>
                <li id="req-numero">Un número</li>
            </ul>
            <span class="error-campo" id="error-password"><%= msg(errores, "password") %></span>

            <label for="confirmarPassword">Repite la contraseña</label>
            <div class="password-wrapper">
                <input type="password" id="confirmarPassword" name="confirmarPassword" maxlength="64" placeholder="Escríbela otra vez"
                       autocomplete="new-password" class="<%= clase(errores, "confirmarPassword") %>">
                <button type="button" class="toggle-password" data-target="confirmarPassword">Mostrar</button>
            </div>
            <span class="error-campo" id="error-confirmarPassword"><%= msg(errores, "confirmarPassword") %></span>

            <button type="submit">Crear mi cuenta</button>
        </form>

        <p class="redirect-text">¿Ya tienes cuenta? <a href="${pageContext.request.contextPath}/jsp/login.jsp">Inicia sesión</a></p>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/Validaciones.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
</body>
</html>
