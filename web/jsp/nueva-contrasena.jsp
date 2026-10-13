<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map, java.util.Collections, controlador.RecuperacionController, seguridad.EscapeHtml" %>
<%
    // Solo se llega aquí después de validar el código (y dentro de 10 minutos)
    if (RecuperacionController.idVerificadoVigente(session) == null) {
        response.sendRedirect(request.getContextPath() + "/jsp/recuperar.jsp?error=sesion");
        return;
    }
    @SuppressWarnings("unchecked")
    Map<String, String> errores = (Map<String, String>) request.getAttribute("errores");
    if (errores == null) errores = Collections.emptyMap();
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nueva contraseña - Clínica Modelo San Martín</title>
    <script src="${pageContext.request.contextPath}/js/pestana.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css?v=<%= configuracion.VersionAplicacion.RECURSOS %>">
</head>
<body>
<div class="main-wrapper" style="width: 520px;">
    <div class="form-panel">
        <ol class="pasos" aria-label="Progreso">
            <li class="hecho">1. Identifícate</li>
            <li class="hecho">2. Código</li>
            <li class="actual" aria-current="step">3. Nueva contraseña</li>
        </ol>

        <h2>Crea tu nueva contraseña</h2>
        <p class="subtitulo">Código verificado. Al guardar, tu cuenta quedará desbloqueada y te avisaremos por correo.</p>

        <% if (!errores.isEmpty()) { %>
            <div class="alert-error" aria-live="polite">Revisa los campos marcados en rojo.</div>
        <% } %>

        <form id="formNuevaContrasena" action="${pageContext.request.contextPath}/RecuperacionController" method="POST" novalidate>
            <input type="hidden" name="accion" value="cambiar">

            <label for="password">Nueva contraseña</label>
            <div class="password-wrapper">
                <input type="password" id="password" name="password" maxlength="64" autofocus
                       autocomplete="new-password" placeholder="Crea una contraseña segura"
                       class="<%= errores.containsKey("password") ? "invalido" : "" %>">
                <button type="button" class="toggle-password" data-target="password" aria-label="Mostrar contraseña">Mostrar</button>
            </div>
            <ul class="requisitos" aria-label="Requisitos de la contraseña">
                <li id="req-longitud">Mínimo 8 caracteres</li>
                <li id="req-mayuscula">Una letra mayúscula</li>
                <li id="req-minuscula">Una letra minúscula</li>
                <li id="req-numero">Un número</li>
            </ul>
            <span class="error-campo" id="error-password"><%= EscapeHtml.texto(errores.get("password")) %></span>

            <label for="confirmarPassword">Repite la nueva contraseña</label>
            <div class="password-wrapper">
                <input type="password" id="confirmarPassword" name="confirmarPassword" maxlength="64"
                       autocomplete="new-password" placeholder="Escríbela otra vez"
                       class="<%= errores.containsKey("confirmarPassword") ? "invalido" : "" %>">
                <button type="button" class="toggle-password" data-target="confirmarPassword" aria-label="Mostrar contraseña">Mostrar</button>
            </div>
            <span class="error-campo" id="error-confirmarPassword"><%= EscapeHtml.texto(errores.get("confirmarPassword")) %></span>

            <button type="submit">Guardar y entrar</button>
        </form>

        <div class="acciones-secundarias">
            <a href="${pageContext.request.contextPath}/jsp/login.jsp">Cancelar</a>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/Validaciones.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
</body>
</html>
