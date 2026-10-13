package modelo.entidades;

/** Fila de la tabla maestra tm_rol. */
public class Rol {

    public static final int ADMINISTRADOR = 1;
    public static final int RECEPCIONISTA = 2;
    public static final int MEDICO = 3;
    public static final int PACIENTE = 4;

    private final int idRol;
    private final String nombre;
    private final String descripcion;

    public Rol(int idRol, String nombre, String descripcion) {
        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getIdRol() { return idRol; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
