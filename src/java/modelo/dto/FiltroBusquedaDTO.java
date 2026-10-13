package modelo.dto;

/**
 * Criterios de búsqueda y paginación reutilizables por los listados de la
 * Etapa 5 (pacientes, proveedores, servicios y productos).
 *
 *   texto  -> lo que el usuario escribe en "Buscar"
 *   estado -> null (todos) | ACTIVO | INACTIVO
 *   grupo  -> filtro por catálogo (cobertura, categoría...) o null
 *   tipo   -> SERVICIO | PRODUCTO | null (solo en el tarifario)
 */
public class FiltroBusquedaDTO {

    public static final int TAMANIO_PAGINA = 10;

    private final String texto;
    private final String estado;
    private final Integer grupo;
    private final String tipo;
    private final int pagina;

    public FiltroBusquedaDTO(String texto, String estado, Integer grupo, String tipo, int pagina) {
        this.texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        this.estado = ("ACTIVO".equals(estado) || "INACTIVO".equals(estado)) ? estado : null;
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
    public boolean hayFiltros() { return texto != null || estado != null || grupo != null || tipo != null; }

    /** Patrón para ILIKE: quita los comodines que escriba el usuario. */
    public String getPatron() {
        return texto == null ? null : "%" + texto.replace("%", "").replace("_", "") + "%";
    }
}
