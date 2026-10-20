package modelo.dto;

import java.time.LocalDate;
import java.util.Set;

/**
 * Filtros de la agenda de citas. SIEMPRE lleva un rango de fechas (un día,
 * una semana o un mes): la agenda nunca trae todo el historial (RNF-05).
 */
public class FiltroCitaDTO {

    public static final Set<String> ESTADOS = Set.of(CitaDTO.RESERVADA, CitaDTO.CONFIRMADA, CitaDTO.ATENDIDA,
            CitaDTO.NO_ASISTIO, CitaDTO.CANCELADA);
    /** Tope de seguridad de filas por consulta de agenda (un mes muy cargado). */
    public static final int MAXIMO_FILAS = 1000;

    private final LocalDate desde;
    private final LocalDate hasta;
    private final Integer idMedico;
    private final Integer idEspecialidad;
    private final String estado;
    private final String texto;

    public FiltroCitaDTO(LocalDate desde, LocalDate hasta, Integer idMedico, Integer idEspecialidad, String estado, String texto) {
        this.desde = desde;
        this.hasta = hasta;
        this.idMedico = idMedico == null || idMedico <= 0 ? null : idMedico;
        this.idEspecialidad = idEspecialidad == null || idEspecialidad <= 0 ? null : idEspecialidad;
        this.estado = estado != null && ESTADOS.contains(estado) ? estado : null;
        String t = texto == null ? "" : texto.trim();
        this.texto = t.isEmpty() ? null : (t.length() > 60 ? t.substring(0, 60) : t);
    }

    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public Integer getIdMedico() { return idMedico; }
    public Integer getIdEspecialidad() { return idEspecialidad; }
    public String getEstado() { return estado; }
    public String getTexto() { return texto; }

    public boolean hayFiltros() { return idMedico != null || idEspecialidad != null || estado != null || texto != null; }

    /** Patrón para ILIKE con los comodines escapados. */
    public String getPatron() {
        return texto == null ? null
                : "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
