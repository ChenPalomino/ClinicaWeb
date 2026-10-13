package menu;

/** Una opción del menú lateral del dashboard. */
public class OpcionMenu {

    private final String seccion;     // título del grupo (ej. "ADMINISTRACIÓN")
    private final String titulo;      // texto visible
    private final String icono;       // clase de Font Awesome (ej. "fas fa-users")
    private final String url;         // ruta dentro de la aplicación (ej. "/admin/usuarios")
    private final String clave;       // identificador para marcar la opción activa
    private final boolean disponible; // false = módulo aún en desarrollo ("Próximamente")

    public OpcionMenu(String seccion, String titulo, String icono, String url, String clave, boolean disponible) {
        this.seccion = seccion;
        this.titulo = titulo;
        this.icono = icono;
        this.url = url;
        this.clave = clave;
        this.disponible = disponible;
    }

    /** Opción de un módulo que todavía no está construido. */
    public static OpcionMenu proximamente(String seccion, String titulo, String icono) {
        return new OpcionMenu(seccion, titulo, icono, null, null, false);
    }

    public String getSeccion() { return seccion; }
    public String getTitulo() { return titulo; }
    public String getIcono() { return icono; }
    public String getUrl() { return url; }
    public String getClave() { return clave; }
    public boolean isDisponible() { return disponible; }
}
