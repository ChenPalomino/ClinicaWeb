package modelo.dto;

/**
 * DTO (Data Transfer Object): transporta los datos del formulario de
 * registro desde el controlador hasta el servicio y el repositorio.
 * No contiene lógica; solo datos ya normalizados (sin espacios extra).
 */
public class RegistroPacienteDTO {

    private final String dni;
    private final String nombres;
    private final String apellidos;
    private final String celular;
    private final String correo;
    private final String password;
    private final String confirmarPassword;

    public RegistroPacienteDTO(String dni, String nombres, String apellidos, String celular,
                               String correo, String password, String confirmarPassword) {
        this.dni = limpiar(dni);
        this.nombres = limpiarNombre(nombres);
        this.apellidos = limpiarNombre(apellidos);
        this.celular = limpiar(celular);
        this.correo = limpiar(correo).toLowerCase();
        this.password = password == null ? "" : password;
        this.confirmarPassword = confirmarPassword == null ? "" : confirmarPassword;
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String limpiarNombre(String valor) {
        return limpiar(valor).replaceAll("\\s+", " ");
    }

    public String getDni() { return dni; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCelular() { return celular; }
    public String getCorreo() { return correo; }
    public String getPassword() { return password; }
    public String getConfirmarPassword() { return confirmarPassword; }
}
