<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Registro de Paciente - Clínica Modelo San Martín</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body>
    <div class="main-wrapper" style="width: 900px;">
        <div class="image-panel">
            <img src="https://images.unsplash.com/photo-1532938911079-1b06ac7ceec7?auto=format&fit=crop&w=600&q=80" alt="Atención Medica">
            <h3>Portal de Pacientes</h3>
            <p>Regístrese para gestionar sus citas médicas y acceder a los servicios de forma rápida y segura.</p>
        </div>

        <div class="form-panel">
            <h2>Registro de Nuevo Usuario</h2>
            
            <% 
                String err = request.getParameter("error");
                if(err != null) { 
            %>
                <div class="alert-error">Error: El DNI o correo ya se encuentran registrados.</div>
            <% } %>

            <form action="RegistroServlet" method="POST" id="formRegistro">
                <label>DNI (8 dígitos):</label>
                <input type="text" name="dni" id="dni" maxlength="8" placeholder="Ej: 74839201" required>

                <label>Nombres y Apellidos:</label>
                <input type="text" name="nombres" placeholder="Ej: Juan Pérez" required>

                <label>Celular (9 dígitos, comienza con 9):</label>
                <input type="text" name="celular" id="celular" maxlength="9" placeholder="Ej: 912345678" required>
                <small id="errorCelular" style="color: #b91c1c; display: none;">Debe tener 9 dígitos y empezar con 9.</small>

                <label>Correo Electrónico:</label>
                <input type="email" name="correo" placeholder="correo@dominio.com" required>

                <label>Contraseña (Mín. 8 caracteres, 1 mayúscula y 1 número):</label>
                <input type="password" name="password" id="password" placeholder="Mínimo 8 caracteres" required>
                <small id="errorPassword" style="color: #b91c1c; display: none;">Debe incluir mín. 8 caracteres, una mayúscula y un número.</small>

                <button type="submit" id="btnRegistrar">Registrarse</button>
            </form>
            
            <p class="redirect-text">¿Ya tienes cuenta? <a href="login.jsp">Ingresa aquí</a></p>
        </div>
    </div>

    <script src="js/validaciones.js"></script>
</body>
</html>