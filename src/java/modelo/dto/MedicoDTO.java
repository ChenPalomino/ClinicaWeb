package modelo.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * RF-03: un médico = una cuenta de rol Médico + sus datos profesionales
 * (especialidad, CMP, consultorio, duración de la cita) + su horario semanal.
 * Si idMedico es 0, la cuenta existe pero el administrador aún no la configuró.
 */
public class MedicoDTO {

    public static final List<Integer> DURACIONES = List.of(15, 20, 30, 40, 45, 60);

    private int idMedico;
    private int idUsuario;
    private String nombreUsuario = "";
    private String nombres = "";
    private String apellidos = "";
    private String descripcion = "";
    private boolean usuarioActivo = true;
    private boolean tieneFoto;
    private int idEspecialidad;
    private String especialidad = "";
    private String cmp = "";
    private String consultorio = "";
    private int duracionCita = 30;
    private String estado = "Activo";
    private List<HorarioDTO> horarios = new ArrayList<>();

    private static String limpio(String v) { return v == null ? "" : v.trim(); }

    public boolean isConfigurado() { return idMedico > 0; }
    public boolean isActivo() { return "Activo".equals(estado); }

    /** Puede recibir citas: configurado, activo, con cuenta activa y con al menos un bloque de horario. */
    public boolean isAtiende() {
        return isConfigurado() && isActivo() && usuarioActivo && !horarios.isEmpty();
    }

    public String getNombreCompleto() { return (nombres + " " + apellidos).trim(); }

    /** "Dr(a). Marco Médico Demo" para pacientes y agenda. */
    public String getNombreConTitulo() { return "Dr(a). " + getNombreCompleto(); }

    public String getIniciales() {
        String a = nombres.isEmpty() ? "" : nombres.substring(0, 1);
        String b = apellidos.isEmpty() ? "" : apellidos.substring(0, 1);
        return (a + b).toUpperCase();
    }

    /** Bloques de un día (1 = lunes). */
    public List<HorarioDTO> horariosDelDia(int dia) {
        List<HorarioDTO> lista = new ArrayList<>();
        for (HorarioDTO h : horarios) {
            if (h.getDiaSemana() == dia) {
                lista.add(h);
            }
        }
        return lista;
    }

    /** "Lun a Vie 08:00-13:00 · Sáb 08:00-12:00" (resumen corto). */
    public String getResumenHorario() {
        if (horarios.isEmpty()) {
            return "Sin horario";
        }
        StringBuilder sb = new StringBuilder();
        for (int d = 1; d <= 7; d++) {
            List<HorarioDTO> bloques = horariosDelDia(d);
            if (bloques.isEmpty()) {
                continue;
            }
            sb.append(sb.length() == 0 ? "" : " · ").append(HorarioDTO.nombreDia(d), 0, 3).append(' ');
            for (int i = 0; i < bloques.size(); i++) {
                sb.append(i == 0 ? "" : ", ").append(bloques.get(i).getHoraInicio()).append('-').append(bloques.get(i).getHoraFin());
            }
        }
        return sb.toString();
    }

    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int v) { idMedico = v; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int v) { idUsuario = v; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String v) { nombreUsuario = limpio(v); }
    public String getNombres() { return nombres; }
    public void setNombres(String v) { nombres = limpio(v); }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String v) { apellidos = limpio(v); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = limpio(v); }
    public boolean isUsuarioActivo() { return usuarioActivo; }
    public void setUsuarioActivo(boolean v) { usuarioActivo = v; }
    public boolean isTieneFoto() { return tieneFoto; }
    public void setTieneFoto(boolean v) { tieneFoto = v; }
    public int getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(int v) { idEspecialidad = v; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String v) { especialidad = limpio(v); }
    public String getCmp() { return cmp; }
    public void setCmp(String v) { cmp = limpio(v); }
    public String getConsultorio() { return consultorio; }
    public void setConsultorio(String v) { consultorio = limpio(v); }
    public int getDuracionCita() { return duracionCita; }
    public void setDuracionCita(int v) { duracionCita = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public List<HorarioDTO> getHorarios() { return horarios; }
    public void setHorarios(List<HorarioDTO> v) { horarios = v == null ? new ArrayList<>() : v; }
}
