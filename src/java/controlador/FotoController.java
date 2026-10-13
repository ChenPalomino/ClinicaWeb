package controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.FotoDTO;
import modelo.servicios.PerfilService;

import java.io.IOException;
import java.util.Optional;

/**
 * Entrega la foto de perfil guardada en la base de datos: /foto?u=ID&v=VERSION
 * Solo para usuarios con sesión (AutenticacionFilter). El parámetro v cambia
 * cuando se actualiza la foto, así el navegador puede guardarla en caché.
 */
@WebServlet(name = "FotoController", urlPatterns = {"/foto"})
public class FotoController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int idUsuario;
        try {
            idUsuario = Integer.parseInt(request.getParameter("u"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Optional<FotoDTO> foto = new PerfilService().obtenerFoto(idUsuario);
            if (foto.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType(foto.get().getTipoMime());
            response.setHeader("Cache-Control", "private, max-age=86400");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLength(foto.get().getImagen().length);
            response.getOutputStream().write(foto.get().getImagen());
        } catch (RuntimeException e) {
            System.err.println("FotoController: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
