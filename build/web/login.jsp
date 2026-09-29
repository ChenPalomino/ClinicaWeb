<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Acceso al Sistema - Clínica Modelo San Martín</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body>
    <div class="main-wrapper">
        <div class="image-panel">
            <img src="https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=600&q=80" alt="Clinica">
            <h3>Clínica Modelo San Martín</h3>
            <p>Sistema Integrado de Gestión y Citas Médicas (Arquitectura MVC)</p>
        </div>

        <div class="form-panel">
            <h2>Iniciar Sesión</h2>
            
            <% 
                String error = request.getParameter("error");
                String registro = request.getParameter("registro");
                if(error != null) { 
            %>
                <div class="alert-error">Credenciales incorrectas o DNI no registrado.</div>
            <% } %>
            
            <% if(registro != null) { %>
                <div class="alert-success">¡Registro exitoso! Ya puedes iniciar sesión.</div>
            <% } %>
            
            <form action="LoginServlet" method="POST">
                <label>DNI de Usuario:</label>
                <input type="text" name="dni" maxlength="8" placeholder="Ingrese 8 dígitos" required>

                <label>Contraseña:</label>
                <input type="password" name="password" placeholder="Ingrese su contraseña" required>

                <button type="submit">Ingresar</button>
            </form>
            
            <p class="redirect-text">¿No tienes cuenta? <a href="registro.jsp">Regístrate aquí</a></p>
        </div>
    </div>
</body>
</html>