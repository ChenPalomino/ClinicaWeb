<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="seguridad.EscapeHtml, seguridad.AutenticacionFacade" %>
<%
    String error = request.getParameter("error");
    String usuarioPrevio = request.getParameter("usuario");
    // Pestaña inicial: la que indique el servidor (tras un error) o "paciente"
    String tipo = "personal".equals(request.getParameter("tipo")) ? "personal" : "paciente";
    boolean esPersonal = "personal".equals(tipo);
    int intentos = 0;
    try { intentos = Integer.parseInt(request.getParameter("intentos")); } catch (Exception ignorado) { }
    int restantes = Math.max(AutenticacionFacade.MAX_INTENTOS - intentos, 0);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Iniciar sesión - Clínica Modelo San Martín</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css?v=<%= configuracion.VersionAplicacion.RECURSOS %>">
</head>
<body>
<div class="main-wrapper">
    <div class="image-panel">
        <img src="https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=600&q=80" alt="">
        <h3>Clínica Modelo San Martín</h3>
        <p>Reserva tus citas, revisa tus atenciones y gestiona la clínica desde un solo lugar.</p>
    </div>

    <div class="form-panel">
        <h2>Iniciar sesión</h2>

        <%-- Pestañas: el usuario elige quién es (lenguaje del mundo real, sin tecnicismos) --%>
        <div class="tabs-acceso" role="tablist" aria-label="Tipo de acceso">
            <button type="button" class="tab-acceso" role="tab" id="tab-paciente" data-tipo="paciente"
                    aria-selected="<%= !esPersonal %>">Soy paciente</button>
            <button type="button" class="tab-acceso" role="tab" id="tab-personal" data-tipo="personal"
                    aria-selected="<%= esPersonal %>">Personal de la clínica</button>
        </div>
        <p class="ayuda-tab" id="ayuda-tab">
            <%= esPersonal ? "Médicos, recepción y administración." : "Ingresa con el DNI con el que te registraste." %>
        </p>

        <div aria-live="polite">
        <% if ("exito".equals(request.getParameter("registro"))) { %>
            <div class="alert-success">¡Cuenta creada! Ingresa con tu DNI y la contraseña que elegiste.</div>
        <% } else if ("ok".equals(request.getParameter("recuperacion"))) { %>
            <div class="alert-success">Tu contraseña se actualizó y tu cuenta está activa. Ya puedes ingresar.</div>
        <% } else if ("ok".equals(request.getParameter("salida"))) { %>
            <div class="alert-success">Cerraste sesión correctamente.</div>
        <% } %>

        <% if ("credenciales".equals(error)) { %>
            <div class="alert-error">
                La contraseña no es correcta.
                <% if (restantes == 1) { %>
                    <strong>Te queda 1 intento</strong> antes de que la cuenta se bloquee.
                    Si no la recuerdas, mejor <a href="${pageContext.request.contextPath}/jsp/recuperar.jsp">recupérala aquí</a>.
                <% } else { %>
                    Te quedan <%= restantes %> intentos.
                <% } %>
            </div>
        <% } else if ("noexiste".equals(error)) { %>
            <div class="alert-error">
                No encontramos una cuenta activa con ese <%= esPersonal ? "usuario o DNI" : "DNI" %>.
                Revisa que esté bien escrito<%= esPersonal ? "" : " o crea tu cuenta" %>.
            </div>
        <% } else if ("vacio".equals(error)) { %>
            <div class="alert-error">Completa tu <%= esPersonal ? "usuario" : "DNI" %> y tu contraseña.</div>
        <% } else if ("sesion".equals(error)) { %>
            <div class="alert-info">Tu sesión terminó por inactividad o aún no has ingresado. Vuelve a iniciar sesión.</div>
        <% } else if ("enlace".equals(error)) { %>
            <div class="alert-error">El enlace no es válido o ya venció. Si necesitas ayuda, acércate a recepción.</div>
        <% } else if ("sistema".equals(error)) { %>
            <div class="alert-error">No pudimos conectar con el sistema. Inténtalo de nuevo en unos minutos.</div>
        <% } %>
        </div>

        <form id="formLogin" action="${pageContext.request.contextPath}/AutenticacionController" method="POST">
            <input type="hidden" name="tipoAcceso" id="tipoAcceso" value="<%= tipo %>">

            <label for="nombreUsuario" id="etiqueta-usuario"><%= esPersonal ? "Usuario o DNI" : "DNI" %></label>
            <input type="text" id="nombreUsuario" name="nombreUsuario" maxlength="100" required autofocus
                   autocomplete="username" autocapitalize="none" spellcheck="false"
                   inputmode="<%= esPersonal ? "text" : "numeric" %>"
                   placeholder="<%= esPersonal ? "Ej: jperez" : "8 dígitos, ej: 74839201" %>"
                   value="<%= EscapeHtml.texto(usuarioPrevio) %>">

            <div class="fila-etiqueta">
                <label for="password">Contraseña</label>
                <a class="enlace-secundario" href="${pageContext.request.contextPath}/jsp/recuperar.jsp">¿Olvidaste tu contraseña?</a>
            </div>
            <div class="password-wrapper">
                <input type="password" id="password" name="password" maxlength="100" required
                       autocomplete="current-password" placeholder="Tu contraseña">
                <button type="button" class="toggle-password" data-target="password" aria-label="Mostrar contraseña">Mostrar</button>
            </div>
            <span class="aviso-mayus" id="aviso-mayus" role="status">Bloq Mayús está activado.</span>

            <button type="submit">Ingresar</button>
        </form>

        <p class="redirect-text" id="enlace-registro" <%= esPersonal ? "hidden" : "" %>>
            ¿Es tu primera vez? <a href="${pageContext.request.contextPath}/jsp/registro.jsp">Crea tu cuenta de paciente</a>
        </p>
        <p class="redirect-text" id="ayuda-personal" <%= esPersonal ? "" : "hidden" %>>
            ¿Eres nuevo en la clínica? Tu usuario te lo entrega el administrador.
        </p>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/Validaciones.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
</body>
</html>
