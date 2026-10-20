package controlador.caja;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import configuracion.ConfiguracionClinicaSingleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.ComprobanteDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.servicios.CobranzaService;

import java.io.IOException;
import java.util.Optional;

/**
 * Representación impresa de una boleta o factura (RF-15).
 *   /caja/comprobante?id=      -> personal de caja y administrador (cualquier comprobante)
 *   /paciente/comprobante?id=  -> el paciente, SOLO los suyos (lo verifica el servicio)
 * Si el comprobante no existe o no le pertenece, se responde igual ("no existe"):
 * así nadie puede averiguar qué números de comprobante existen.
 */
@WebServlet(name = "ComprobanteController", urlPatterns = {"/caja/comprobante", "/paciente/comprobante"})
public class ComprobanteController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        boolean paciente = usuario.getRolId() == Rol.PACIENTE;
        String volver = request.getContextPath() + (paciente ? "/paciente/pagos" : "/caja/cuentas");
        try {
            Optional<ComprobanteDTO> c = new CobranzaService().comprobanteVisiblePara(
                    ControladorUtil.entero(request.getParameter("id"), 0), usuario);
            if (c.isEmpty()) {
                MensajeFlash.error(request.getSession(), "El comprobante que buscas no existe.");
                response.sendRedirect(volver);
                return;
            }
            request.setAttribute("comprobante", c.get());
            request.setAttribute("parametros", ConfiguracionClinicaSingleton.getInstancia().getParametros());
            request.setAttribute("volver", paciente ? volver
                    : request.getContextPath() + "/caja/cuentas?accion=ver&id=" + c.get().getIdCuentaAtencion());
            request.getRequestDispatcher("/WEB-INF/vistas/caja/comprobante.jsp").forward(request, response);
        } catch (RuntimeException e) {
            System.err.println("ComprobanteController: " + e.getMessage());
            MensajeFlash.error(request.getSession(), "No pudimos cargar el comprobante. Inténtalo de nuevo en unos minutos.");
            response.sendRedirect(volver);
        }
    }
}
