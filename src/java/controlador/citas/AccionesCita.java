package controlador.citas;

import modelo.citas.ReglasCita;
import modelo.dto.CitaDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;

import java.time.LocalDateTime;

/**
 * Qué botones ve cada rol para UNA cita. La vista solo pregunta "¿muestro
 * este botón?"; las reglas están en ReglasCita (las mismas que valida el
 * servicio), así nunca se ofrece una acción que luego se rechace (Nielsen #5).
 */
public final class AccionesCita {

    private final boolean llegada;
    private final boolean cobrar;
    private final boolean verCuenta;
    private final boolean reprogramar;
    private final boolean cancelar;
    private final boolean atender;
    private final boolean noAsistio;
    private final boolean verNota;
    private final String bloqueoPaciente;

    public AccionesCita(CitaDTO c, Usuario u, LocalDateTime ahora, int horasCancelacion) {
        boolean personal = u.getRolId() == Rol.ADMINISTRADOR || u.getRolId() == Rol.RECEPCIONISTA;
        boolean paciente = u.getRolId() == Rol.PACIENTE;
        boolean suMedico = u.getRolId() == Rol.MEDICO && c.getIdUsuarioMedico() == u.getIdUsuario();
        boolean medicoOAdmin = suMedico || u.getRolId() == Rol.ADMINISTRADOR;
        this.llegada = personal && ReglasCita.puedeRegistrarLlegada(c, ahora.toLocalDate());
        this.cobrar = personal && ReglasCita.puedeCobrar(c);
        this.verCuenta = personal && c.isTieneCuenta();
        this.reprogramar = personal ? ReglasCita.personalPuedeReprogramar(c, ahora)
                         : paciente && ReglasCita.pacientePuedeReprogramar(c, ahora, horasCancelacion);
        this.cancelar = personal ? ReglasCita.personalPuedeCancelar(c, ahora.toLocalDate())
                      : paciente && ReglasCita.pacientePuedeCambiar(c, ahora, horasCancelacion);
        this.atender = medicoOAdmin && ReglasCita.puedeMarcarAtendida(c, ahora.toLocalDate());
        this.noAsistio = medicoOAdmin && ReglasCita.puedeMarcarNoAsistio(c, ahora);
        this.verNota = medicoOAdmin;
        String bloqueo = paciente ? ReglasCita.motivoBloqueoPaciente(c, ahora, horasCancelacion) : null;
        if (paciente && bloqueo == null && c.isReservada() && !reprogramar) {
            bloqueo = "Ya cambiaste esta cita " + ReglasCita.MAXIMO_REPROGRAMACIONES_PACIENTE
                    + " veces: si necesitas otro horario, llama a recepción (o cancélala y reserva otra).";
        }
        this.bloqueoPaciente = bloqueo;
    }

    public boolean isLlegada() { return llegada; }
    public boolean isCobrar() { return cobrar; }
    public boolean isVerCuenta() { return verCuenta; }
    public boolean isReprogramar() { return reprogramar; }
    public boolean isCancelar() { return cancelar; }
    public boolean isAtender() { return atender; }
    public boolean isNoAsistio() { return noAsistio; }
    public boolean isVerNota() { return verNota; }
    public String getBloqueoPaciente() { return bloqueoPaciente; }
    public boolean isAlguna() { return llegada || cobrar || verCuenta || reprogramar || cancelar || atender || noAsistio; }
}
