<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String error = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Recuperar acceso - Clínica Modelo San Martín</title>
    <script src="${pageContext.request.contextPath}/js/pestana.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css?v=<%= configuracion.VersionAplicacion.RECURSOS %>">
</head>
<body>
<div class="main-wrapper" style="width: 520px;">
    <div class="form-panel">
        <ol class="pasos" aria-label="Progreso">
            <li class="actual" aria-current="step">1. Identifícate</li>
            <li>2. Código</li>
            <li>3. Nueva contraseña</li>
        </ol>

        <h2>Recupera tu acceso</h2>
        <p class="subtitulo">
            Te enviaremos un código de 6 dígitos al correo registrado en tu cuenta.
            También sirve para desbloquear la cuenta.
        </p>

        <div aria-live="polite">
        <% if ("vacio".equals(error)) { %>
            <div class="alert-error">Escribe tu DNI o tu usuario para continuar.</div>
        <% } else if ("envio".equals(error)) { %>
            <div class="alert-error">No pudimos enviar el correo en este momento. Inténtalo de nuevo en unos minutos.</div>
        <% } else if ("sesion".equals(error)) { %>
            <div class="alert-info">Por seguridad, el proceso venció. Empieza de nuevo: solo toma un minuto.</div>
        <% } else if ("sistema".equals(error)) { %>
            <div class="alert-error">No pudimos conectar con el sistema. Inténtalo de nuevo en unos minutos.</div>
        <% } %>
        </div>

        <form id="formRecuperar" action="${pageContext.request.contextPath}/RecuperacionController" method="POST">
            <input type="hidden" name="accion" value="solicitar">
            <label for="identificador">DNI o usuario</label>
            <input type="text" id="identificador" name="identificador" maxlength="100" required autofocus
                   autocomplete="username" autocapitalize="none" spellcheck="false"
                   placeholder="Ej: 74839201">
            <span class="ayuda">Pacientes: su DNI. Personal de la clínica: su usuario o DNI.</span>

            <button type="submit">Enviar código</button>
        </form>

        <div class="acciones-secundarias">
            <a href="${pageContext.request.contextPath}/jsp/login.jsp">&larr; Volver a iniciar sesión</a>
        </div>
        <p class="ayuda" style="margin-top: 16px;">
            ¿Ya no tienes acceso a tu correo? Acércate a recepción con tu DNI y te ayudaremos.
        </p>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/Validaciones.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
</body>
</html>
