package modelo.dto;

/** Criterios de búsqueda y paginación del listado de usuarios. */
public class FiltroUsuarioDTO {

    public static final int TAMANIO_PAGINA = 10;

    private final String texto;      // busca en usuario, DNI, nombre o correo
    private final Integer idRol;     // null = todos
    private final boolean soloPersonal; // true = todos los roles excepto Paciente
    private final String estado;     // null | ACTIVO | INACTIVO | BLOQUEADO
    private final int pagina;
    private RangoFechasDTO rango = RangoFechasDTO.SIN_RANGO;   // fecha de registro (ver RangoFechasDTO)

    public FiltroUsuarioDTO(String texto, Integer idRol, String estado, int pagina) {
        this(texto, idRol, estado, pagina, false);
    }

    public FiltroUsuarioDTO(String texto, Integer idRol, String estado, int pagina, boolean soloPersonal) {
        this.soloPersonal = soloPersonal && idRol == null;
        this.texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        this.idRol = idRol;
        this.estado = ("ACTIVO".equals(estado) || "INACTIVO".equals(estado) || "BLOQUEADO".equals(estado)) ? estado : null;
        this.pagina = Math.max(pagina, 1);
    }

    public String getTexto() { return texto; }
    public Integer getIdRol() { return idRol; }
    public boolean isSoloPersonal() { return soloPersonal; }
    public String getEstado() { return estado; }
    public int getPagina() { return pagina; }
    public int getTamanio() { return TAMANIO_PAGINA; }
    public int getOffset() { return (pagina - 1) * TAMANIO_PAGINA; }
    public RangoFechasDTO getRango() { return rango; }
    public boolean hayFiltros() { return texto != null || idRol != null || estado != null || soloPersonal || rango.isElegido(); }

    /** Agrega el rango de fechas de registro (devuelve el mismo filtro para encadenar). */
    public FiltroUsuarioDTO conRango(RangoFechasDTO r) {
        this.rango = r == null ? RangoFechasDTO.SIN_RANGO : r;
        return this;
    }
}
