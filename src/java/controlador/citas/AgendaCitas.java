package controlador.citas;

import controlador.ControladorUtil;
import jakarta.servlet.http.HttpServletRequest;
import modelo.citas.vista.VistaAgendaFactory;
import modelo.citas.vista.VistaAgendaStrategy;
import modelo.dto.FiltroCitaDTO;
import modelo.servicios.CitaService;

import java.time.LocalDate;

/**
 * Prepara la AGENDA (citas/agenda.jsp) para recepción y para el médico.
 * La vista (día, semana o mes) es una Strategy: define el rango de fechas que
 * se consulta, así la agenda NUNCA trae más de un mes de citas (RNF-05).
 * Por defecto: HOY, para que nadie piense "no tengo citas" (Nielsen #1).
 */
public final class AgendaCitas {

    private AgendaCitas() {
    }

    /** @param medicoFijo el médico de la sesión (agenda del médico) o null (recepción: todos o el elegido) */
    public static void cargar(HttpServletRequest request, CitaService servicio, Integer medicoFijo) {
        VistaAgendaStrategy vista = VistaAgendaFactory.obtener(request.getParameter("vista"));
        LocalDate fecha = ControladorUtil.fecha(request.getParameter("fecha"));
        if (fecha == null) {
            fecha = servicio.hoy();
        }
        Integer medico = medicoFijo != null ? medicoFijo : ControladorUtil.enteroONulo(request.getParameter("medico"));
        FiltroCitaDTO filtro = new FiltroCitaDTO(vista.desde(fecha), vista.hasta(fecha), medico,
                ControladorUtil.enteroONulo(request.getParameter("esp")), request.getParameter("estado"),
                request.getParameter("q"));
        request.setAttribute("vista", vista);
        request.setAttribute("vistas", VistaAgendaFactory.todas());
        request.setAttribute("fecha", fecha);
        request.setAttribute("hoy", servicio.hoy());
        request.setAttribute("ahora", servicio.ahora());
        request.setAttribute("filtroCita", filtro);
        // El resumen por estado se calcula SIN el filtro de estado (para que los contadores sirvan de filtro)
        FiltroCitaDTO sinEstado = new FiltroCitaDTO(filtro.getDesde(), filtro.getHasta(), filtro.getIdMedico(),
                filtro.getIdEspecialidad(), null, filtro.getTexto());
        if ("mes".equals(vista.getCodigo())) {
            request.setAttribute("porDia", servicio.resumenPorDia(filtro));
        } else {
            request.setAttribute("citas", servicio.agenda(filtro));
        }
        request.setAttribute("resumen", servicio.resumen(sinEstado));
        if (medicoFijo == null) {
            request.setAttribute("medicosFiltro", servicio.medicos(null));
        }
    }

    /** Parámetros que conservan la vista al volver de una acción (para "regreso"). */
    public static String parametros(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        for (String p : new String[]{"vista", "fecha", "medico", "esp", "estado", "q"}) {
            String v = request.getParameter(p);
            if (v != null && !v.isBlank()) {
                sb.append(sb.length() == 0 ? "" : "&").append(p).append('=')
                  .append(java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
