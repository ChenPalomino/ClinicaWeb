package modelo.dto;

import java.util.ArrayList;
import java.util.List;

/** Números para las tarjetas del dashboard del administrador. */
public class EstadisticasUsuariosDTO {

    private final int total;
    private final int activos;
    private final int inactivos;
    private final int bloqueados;
    private final int pacientes;
    private final int personal;

    // Datos para los gráficos del inicio
    private final List<String> rolesEtiquetas = new ArrayList<>();
    private final List<Integer> rolesValores = new ArrayList<>();
    private final List<String> diasEtiquetas = new ArrayList<>();   // últimos 14 días (dd/MM)
    private final List<Integer> diasValores = new ArrayList<>();    // usuarios nuevos por día

    public EstadisticasUsuariosDTO(int total, int activos, int inactivos, int bloqueados, int pacientes, int personal) {
        this.total = total;
        this.activos = activos;
        this.inactivos = inactivos;
        this.bloqueados = bloqueados;
        this.pacientes = pacientes;
        this.personal = personal;
    }

    public int getTotal() { return total; }
    public int getActivos() { return activos; }
    public int getInactivos() { return inactivos; }
    public int getBloqueados() { return bloqueados; }
    public int getPacientes() { return pacientes; }
    public int getPersonal() { return personal; }

    public List<String> getRolesEtiquetas() { return rolesEtiquetas; }
    public List<Integer> getRolesValores() { return rolesValores; }
    public List<String> getDiasEtiquetas() { return diasEtiquetas; }
    public List<Integer> getDiasValores() { return diasValores; }
    public int getNuevosUltimos14Dias() { return diasValores.stream().mapToInt(Integer::intValue).sum(); }
}
