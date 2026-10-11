package modelo.dto;

/**
 * Una fila vigente de tt_reseteo_contrasena: un código de 6 dígitos
 * (tipo CODIGO) o un enlace "No fui yo" (tipo ALERTA).
 * Los tiempos se calculan en la base de datos para evitar problemas de zona horaria.
 */
public class TokenRecuperacionDTO {

    public static final String TIPO_CODIGO = "CODIGO";
    public static final String TIPO_ALERTA = "ALERTA";

    private final int idReseteo;
    private final String hash;
    private final int intentos;
    private final long segundosRestantes;
    private final long segundosDesdeCreacion;

    public TokenRecuperacionDTO(int idReseteo, String hash, int intentos,
                                long segundosRestantes, long segundosDesdeCreacion) {
        this.idReseteo = idReseteo;
        this.hash = hash;
        this.intentos = intentos;
        this.segundosRestantes = segundosRestantes;
        this.segundosDesdeCreacion = segundosDesdeCreacion;
    }

    public int getIdReseteo() { return idReseteo; }
    public String getHash() { return hash; }
    public int getIntentos() { return intentos; }
    public long getSegundosRestantes() { return segundosRestantes; }
    public long getSegundosDesdeCreacion() { return segundosDesdeCreacion; }
    public boolean isVigente() { return segundosRestantes > 0; }
}
