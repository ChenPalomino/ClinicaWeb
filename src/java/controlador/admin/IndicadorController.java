package controlador.admin;

import controlador.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.IndicadorDTO;
import modelo.servicios.IndicadorService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Indicadores del inicio del administrador, calculados solo cuando se piden: GET /admin/indicadores?codigo=&periodo= (JSON). */
@WebServlet(name = "IndicadorController", urlPatterns = {"/admin/indicadores"})
public class IndicadorController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String codigo = request.getParameter("codigo");
        String periodo = request.getParameter("periodo");
        try {
            Optional<IndicadorDTO> r = new IndicadorService().calcular(codigo, periodo);
            if (r.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Ese indicador no existe.\"}");
                return;
            }
            String texto = IndicadorService.textoPeriodo(codigo, periodo, LocalDate.now(ZoneId.of("America/Lima")));
            response.getWriter().write(json(r.get(), texto, request.getContextPath()));
        } catch (RuntimeException e) {
            System.err.println("IndicadorController: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"No pudimos calcular el indicador. Inténtalo en unos minutos.\"}");
        }
    }

    static String json(IndicadorDTO r, String periodo, String ctx) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"codigo\":").append(JsonUtil.texto(r.getCodigo()))
          .append(",\"titulo\":").append(JsonUtil.texto(r.getTitulo()))
          .append(",\"tipo\":").append(JsonUtil.texto(r.getTipoGrafico()))
          .append(",\"formato\":").append(JsonUtil.texto(r.getFormato()))
          .append(",\"periodo\":").append(JsonUtil.texto(periodo))
          .append(",\"nota\":").append(JsonUtil.texto(r.getNota()))
          .append(",\"enlace\":").append(JsonUtil.texto(r.getEnlace().isEmpty() ? "" : ctx + r.getEnlace()))
          .append(",\"vacio\":").append(r.isVacio())
          .append(",\"etiquetas\":[");
        lista(sb, r.getEtiquetas());
        sb.append("],\"series\":[");
        for (int i = 0; i < r.getSeries().size(); i++) {
            IndicadorDTO.Serie s = r.getSeries().get(i);
            sb.append(i > 0 ? "," : "").append("{\"nombre\":").append(JsonUtil.texto(s.nombre())).append(",\"valores\":[");
            for (int k = 0; k < s.valores().size(); k++) {
                BigDecimal v = s.valores().get(k);
                sb.append(k > 0 ? "," : "").append(v == null ? "0" : v.stripTrailingZeros().toPlainString());
            }
            sb.append("]}");
        }
        sb.append("],\"resumen\":[");
        for (int i = 0; i < r.getResumen().size(); i++) {
            IndicadorDTO.Dato d = r.getResumen().get(i);
            sb.append(i > 0 ? "," : "").append("{\"etiqueta\":").append(JsonUtil.texto(d.etiqueta()))
              .append(",\"valor\":").append(JsonUtil.texto(d.valor()))
              .append(",\"detalle\":").append(JsonUtil.texto(d.detalle())).append('}');
        }
        return sb.append("]}").toString();
    }

    private static void lista(StringBuilder sb, List<String> textos) {
        for (int i = 0; i < textos.size(); i++) {
            sb.append(i > 0 ? "," : "").append(JsonUtil.texto(textos.get(i)));
        }
    }
}
