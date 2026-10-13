package notificacion;

/** Resultado detallado de un envío: si se aceptó, el código HTTP y la explicación. */
public final class ResultadoEnvio {

    private final boolean aceptado;
    private final int codigoHttp;
    private final String mensaje;

    public ResultadoEnvio(boolean aceptado, int codigoHttp, String mensaje) {
        this.aceptado = aceptado;
        this.codigoHttp = codigoHttp;
        this.mensaje = mensaje == null ? "" : mensaje;
    }

    public boolean isAceptado() { return aceptado; }
    public int getCodigoHttp() { return codigoHttp; }
    public String getMensaje() { return mensaje; }
}
