<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="controlador.RecuperacionController, modelo.servicios.RecuperacionService" %>
<%
    // Sin el paso 1 hecho no tiene sentido esta pantalla
    if (session.getAttribute(RecuperacionController.SESION_IDENTIFICADOR) == null) {
        response.sendRedirect(request.getContextPath() + "/jsp/recuperar.jsp?error=sesion");
        return;
    }
    String error = request.getParameter("error");
    String aviso = request.getParameter("aviso");
    int restantes = 0;
    try { restantes = Integer.parseInt(request.getParameter("restantes")); } catch (Exception ignorado) { }
    // El botón "Reenviar" espera 60 s solo justo después de un envío
    boolean recienEnviado = "enviado".equals(aviso) || "reenviado".equals(aviso) || "esperar".equals(aviso);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ingresa el código - Clínica Modelo San Martín</title>
    <script src="${pageContext.request.contextPath}/js/pestana.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css?v=<%= configuracion.VersionAplicacion.RECURSOS %>">
</head>
<body>
<div class="main-wrapper" style="width: 520px;">
    <div class="form-panel">
        <ol class="pasos" aria-label="Progreso">
            <li class="hecho">1. Identifícate</li>
            <li class="actual" aria-current="step">2. Código</li>
            <li>3. Nueva contraseña</li>
        </ol>

        <h2>Revisa tu correo</h2>
        <p class="subtitulo">
            Si los datos corresponden a una cuenta con correo registrado, te enviamos un código de
            6 dígitos. Vence en <%= RecuperacionService.MINUTOS_CODIGO %> minutos.
            Si no lo ves, revisa la carpeta de spam o correo no deseado.
        </p>

        <div aria-live="polite">
        <% if ("reenviado".equals(aviso)) { %>
            <div class="alert-success">Te enviamos un código nuevo. El anterior ya no sirve.</div>
        <% } else if ("esperar".equals(aviso)) { %>
            <div class="alert-info">Ya te enviamos un código hace menos de un minuto. Revisa tu correo o espera para pedir otro.</div>
        <% } %>

        <% if ("incorrecto".equals(error)) { %>
            <div class="alert-error">
                El código no es correcto.
                <% if (restantes > 0) { %>Te quedan <%= restantes %> intento<%= restantes == 1 ? "" : "s" %>.<% } %>
            </div>
        <% } else if ("intentos".equals(error)) { %>
            <div class="alert-error">Superaste los intentos para este código. Pide uno nuevo con el botón de abajo.</div>
        <% } else if ("expirado".equals(error)) { %>
            <div class="alert-error">El código venció o ya se usó. Pide uno nuevo con el botón de abajo.</div>
        <% } %>
        </div>

        <form id="formCodigo" action="${pageContext.request.contextPath}/RecuperacionController" method="POST">
            <input type="hidden" name="accion" value="verificar">
            <label for="codigo">Código de verificación</label>
            <input type="text" id="codigo" name="codigo" class="input-codigo" required autofocus
                   inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="\d{6}"
                   placeholder="••••••" aria-describedby="ayuda-codigo">
            <span class="ayuda" id="ayuda-codigo">Escribe los 6 números del correo.</span>
            <button type="submit">Verificar código</button>
        </form>

        <form action="${pageContext.request.contextPath}/RecuperacionController" method="POST">
            <input type="hidden" name="accion" value="reenviar">
            <button type="submit" class="boton-secundario" id="btnReenviar"
                    data-espera="<%= recienEnviado ? RecuperacionService.SEGUNDOS_ENTRE_ENVIOS : 0 %>">
                Reenviar código
            </button>
        </form>

        <div class="acciones-secundarias">
            <a href="${pageContext.request.contextPath}/jsp/recuperar.jsp">Usar otro DNI o usuario</a>
            <a href="${pageContext.request.contextPath}/jsp/login.jsp">Cancelar</a>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/Validaciones.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
</body>
</html>
