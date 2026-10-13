package evento;

import java.time.LocalDateTime;

/**
 * Lo que pasó: quién, qué, sobre qué registro y cuándo. Objeto inmutable que
 * el Sujeto (EventoSubject) entrega a cada Observador.
 */
public final class EventoSistema {

    private final TipoEvento tipo;
    private final Integer idUsuario;
    private final String nombreUsuario;
    private final Integer idEntidad;
    private final String detalle;
    private final String direccionIp;
    private final LocalDateTime fecha = LocalDateTime.now();

    public EventoSistema(TipoEvento tipo, Integer idUsuario, String nombreUsuario, Integer idEntidad, String detalle) {
        this.tipo = tipo;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.idEntidad = idEntidad;
        this.detalle = detalle == null ? null : (detalle.length() > 500 ? detalle.substring(0, 497) + "..." : detalle);
        this.direccionIp = ContextoPeticion.ip();
    }

    public TipoEvento getTipo() { return tipo; }
    public Integer getIdUsuario() { return idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public Integer getIdEntidad() { return idEntidad; }
    public String getDetalle() { return detalle; }
    public String getDireccionIp() { return direccionIp; }
    public LocalDateTime getFecha() { return fecha; }
}
