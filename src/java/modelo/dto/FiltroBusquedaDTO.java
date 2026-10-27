package modelo.dto;

/**
 * Criterios de búsqueda y paginación reutilizables por los listados de la
 * Etapa 5 (pacientes, proveedores, servicios y productos).
 *
 *   texto  -> lo que el usuario escribe en "Buscar"
 *   estado -> null (todos) | ACTIVO | INACTIVO  (cobros: PENDIENTE | PAGADA | ANULADA)
 *   grupo  -> filtro por catálogo (cobertura, categoría...) o null
 *   tipo   -> SERVICIO | PRODUCTO | null (solo en el tarifario)
 *   rango  -> fechas de registro (por defecto: últimos 30 días, ver RangoFechasDTO)
 */
public class FiltroBusquedaDTO {

    public static final int TAMANIO_PAGINA = 10;

    private final String texto;
    private final String estado;
    private final Integer grupo;
    private final String tipo;
    private final int pagina;
    private RangoFechasDTO rango = RangoFechasDTO.SIN_RANGO;

    private static final java.util.Set<String> ESTADOS = java.util.Set.of("ACTIVO", "INACTIVO", "PENDIENTE", "PAGADA", "ANULADA", "REPONER");

    public FiltroBusquedaDTO(String texto, String estado, Integer grupo, String tipo, int pagina) {
        this.texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        this.estado = estado != null && ESTADOS.contains(estado) ? estado : null;   // Set.of().contains(null) lanza excepción
        this.grupo = (grupo == null || grupo <= 0) ? null : grupo;
        this.tipo = ("SERVICIO".equals(tipo) || "PRODUCTO".equals(tipo)) ? tipo : null;
        this.pagina = Math.max(pagina, 1);
    }

    public String getTexto() { return texto; }
    public String getEstado() { return estado; }
    public Integer getGrupo() { return grupo; }
    public String getTipo() { return tipo; }
    public int getPagina() { return pagina; }
    public int getTamanio() { return TAMANIO_PAGINA; }
    public int getOffset() { return (pagina - 1) * TAMANIO_PAGINA; }
    public RangoFechasDTO getRango() { return rango; }
    public boolean hayFiltros() { return texto != null || estado != null || grupo != null || tipo != null || rango.isElegido(); }

    /** Agrega el rango de fechas de registro (devuelve el mismo filtro para encadenar). */
    public FiltroBusquedaDTO conRango(RangoFechasDTO r) {
        this.rango = r == null ? RangoFechasDTO.SIN_RANGO : r;
        return this;
    }

    /** Patrón para ILIKE: quita los comodines que escriba el usuario. */
    public String getPatron() {
        return texto == null ? null : "%" + texto.replace("%", "").replace("_", "") + "%";
    }
}
