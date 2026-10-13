package modelo.dto;

/**
 * Una opción de una lista desplegable que viene de una tabla maestra (tm_).
 * "extra" guarda un dato adicional útil para la vista: el tipo de la
 * cobertura (PARTICULAR, EPS...), el tipo de la categoría (SERVICIO/PRODUCTO)
 * o la abreviatura de la unidad de medida.
 */
public class OpcionDTO {

    private final int id;
    private final String nombre;
    private final String extra;

    public OpcionDTO(int id, String nombre, String extra) {
        this.id = id;
        this.nombre = nombre;
        this.extra = extra == null ? "" : extra;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getExtra() { return extra; }
}
