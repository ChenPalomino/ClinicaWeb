package controlador.admin;

import controlador.ControladorUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.FiltroAuditoriaDTO;
import modelo.servicios.AuditoriaService;

import java.io.IOException;

/**
 * Controlador (MVC) de la consulta de AUDITORÍA — solo Administrador.
 *   GET /admin/auditoria?q=&entidad=&desde=&hasta=&id=&pagina=
 * Solo lectura: la auditoría no se edita ni se borra (lo impide también la BD).
 */
@WebServlet(name = "AuditoriaController", urlPatterns = {"/admin/auditoria"})
public class AuditoriaController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (ControladorUtil.busquedaSolicitada(request)) {
                java.time.LocalDate desde = ControladorUtil.fecha(request.getParameter("desde"));
                java.time.LocalDate hasta = ControladorUtil.fecha(request.getParameter("hasta"));
                Integer id = ControladorUtil.enteroONulo(request.getParameter("id"));
                String q = request.getParameter("q");
                boolean ultimos30 = desde == null && hasta == null && id == null && (q == null || q.isBlank());
                if (ultimos30) {
                    desde = java.time.LocalDate.now().minusDays(modelo.dto.RangoFechasDTO.DIAS_POR_DEFECTO - 1L);
                }
                FiltroAuditoriaDTO filtro = new FiltroAuditoriaDTO(q, request.getParameter("entidad"), desde, hasta, id,
                        ControladorUtil.entero(request.getParameter("pagina"), 1));
                request.setAttribute("filtro", filtro);
                request.setAttribute("ultimos30", ultimos30);
                request.setAttribute("pagina", new AuditoriaService().listar(filtro));
            }
        } catch (RuntimeException e) {
            System.err.println("AuditoriaController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("menuActivo", "auditoria");
        request.setAttribute("tituloPagina", "Auditoría");
        request.getRequestDispatcher("/WEB-INF/vistas/admin/auditoria/listar.jsp").forward(request, response);
    }
}
