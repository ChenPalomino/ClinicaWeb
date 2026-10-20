package modelo.dto;

import java.time.LocalDate;
import java.util.Set;

/** Filtros del registro de auditoría: texto, entidad y rango de fechas. */
public class FiltroAuditoriaDTO {

    public static final int TAMANIO_PAGINA = 20;
    public static final Set<String> ENTIDADES = Set.of("SESION", "USUARIO", "PACIENTE", "PROVEEDOR", "TARIFARIO",
            "COBRANZA", "CITA", "MEDICO", "CONFIGURACION");

    private final String texto;
    private final String entidad;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final Integer idEntidad;
    private final int pagina;

    public FiltroAuditoriaDTO(String texto, String entidad, LocalDate desde, LocalDate hasta, Integer idEntidad, int pagina) {
        this.texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        this.entidad = entidad != null && ENTIDADES.contains(entidad) ? entidad : null;   // Set.of().contains(null) lanza excepción
        this.desde = desde;
        this.hasta = hasta;
        this.idEntidad = this.entidad == null ? null : idEntidad;
        this.pagina = Math.max(pagina, 1);
    }

    public String getTexto() { return texto; }
    public String getPatron() { return texto == null ? null : "%" + texto.replace("%", "").replace("_", "") + "%"; }
    public String getEntidad() { return entidad; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public Integer getIdEntidad() { return idEntidad; }
    public int getPagina() { return pagina; }
    public int getTamanio() { return TAMANIO_PAGINA; }
    public int getOffset() { return (pagina - 1) * TAMANIO_PAGINA; }
    public boolean hayFiltros() { return texto != null || entidad != null || desde != null || hasta != null; }
}
