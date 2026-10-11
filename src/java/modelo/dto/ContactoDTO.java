package modelo.dto;

/** Datos mínimos para escribirle un correo a un usuario. */
public class ContactoDTO {

    private final String nombres;
    private final String correo;

    public ContactoDTO(String nombres, String correo) {
        this.nombres = nombres;
        this.correo = correo;
    }

    public String getNombres() { return nombres; }
    public String getCorreo() { return correo; }

    /** Correo parcialmente oculto para mostrar en pantalla: ju***@gmail.com */
    public String getCorreoOculto() {
        int arroba = correo.indexOf('@');
        if (arroba <= 1) {
            return "***" + correo.substring(Math.max(arroba, 0));
        }
        return correo.substring(0, Math.min(2, arroba)) + "***" + correo.substring(arroba);
    }
}
