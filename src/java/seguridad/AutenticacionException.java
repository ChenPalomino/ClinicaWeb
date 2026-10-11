package seguridad;

/**
 * Excepción propia del proceso de inicio de sesión.
 * Reemplaza los textos "NO_EXISTE", "CREDANCIALES_INVALIDAS:2", etc.
 * por un motivo tipado y datos extra (intentos, minutos de bloqueo).
 */
public class AutenticacionException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        DATOS_INCOMPLETOS,
        NO_EXISTE,
        CREDENCIALES_INVALIDAS,
        BLOQUEADO,              // bloqueo temporal por 3 intentos (24 h o código al correo)
        BLOQUEADO_SEGURIDAD     // bloqueo sin fecha: solo lo levanta el administrador
    }

    private final Motivo motivo;
    private final int intentos;
    private final long minutosRestantes;
    private final int idUsuario;
    private final boolean recienBloqueado;

    public AutenticacionException(Motivo motivo) {
        this(motivo, 0, 0, 0, false);
    }

    public AutenticacionException(Motivo motivo, int intentos, long minutosRestantes) {
        this(motivo, intentos, minutosRestantes, 0, false);
    }

    public AutenticacionException(Motivo motivo, int intentos, long minutosRestantes,
                                  int idUsuario, boolean recienBloqueado) {
        super(motivo.name());
        this.motivo = motivo;
        this.intentos = intentos;
        this.minutosRestantes = minutosRestantes;
        this.idUsuario = idUsuario;
        this.recienBloqueado = recienBloqueado;
    }

    public Motivo getMotivo() { return motivo; }
    public int getIntentos() { return intentos; }
    public long getMinutosRestantes() { return minutosRestantes; }
    public int getIdUsuario() { return idUsuario; }
    /** true solo en el intento que provocó el bloqueo (para avisar por correo una vez). */
    public boolean isRecienBloqueado() { return recienBloqueado; }
}
