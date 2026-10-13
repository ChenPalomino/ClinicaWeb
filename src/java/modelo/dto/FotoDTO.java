package modelo.dto;

/** Imagen de la foto de perfil tal como se guarda en tt_foto_perfil. */
public class FotoDTO {

    private final byte[] imagen;
    private final String tipoMime;

    public FotoDTO(byte[] imagen, String tipoMime) {
        this.imagen = imagen;
        this.tipoMime = tipoMime;
    }

    public byte[] getImagen() { return imagen; }
    public String getTipoMime() { return tipoMime; }
}
