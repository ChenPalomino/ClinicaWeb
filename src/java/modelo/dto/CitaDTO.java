package modelo.dto;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Una cita médica (PP-01). Estados y su significado para el usuario:
 *   RESERVADA  -> separada, el paciente aún no llega
 *   CONFIRMADA -> RF-08: recepción confirmó que llegó (está en sala de espera)
 *   ATENDIDA   -> RF-10: el médico la atendió
 *   NO_ASISTIO -> RF-10: el paciente no vino
 *   CANCELADA  -> RF-07: se liberó el turno
 */
public class CitaDTO {

    public static final String RESERVADA = "RESERVADA";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String ATENDIDA = "ATENDIDA";
    public static final String NO_ASISTIO = "NO_ASISTIO";
    public static final String CANCELADA = "CANCELADA";
    public static final String CANAL_RECEPCION = "RECEPCION";
    public static final String CANAL_PORTAL = "PORTAL";

    private int idCita;
    private String codigo = "";
    private int idPaciente;
    private String nombrePaciente = "";
    private String dniPaciente = "";
    private String numeroHistoria = "";
    private int idUsuarioPaciente;
    private String correoPaciente = "";
    private String celularPaciente = "";
    private String observacionesPaciente = "";
    private LocalDate fechaNacimientoPaciente;
    private String coberturaPaciente = "";
    private int idMedico;
    private int idUsuarioMedico;
    private String nombreMedico = "";
    private int idEspecialidad;
    private String especialidad = "";
    private String consultorio = "";
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String motivo = "";
    private String estadoCita = RESERVADA;
    private String canal = CANAL_RECEPCION;
    private int reprogramaciones;
    private Timestamp fechaLlegada;
    private Timestamp fechaAtencion;
    private String notaAtencion = "";
    private String motivoCancelacion = "";
    private Timestamp fechaCancelacion;
    private String canceladaPor = "";
    private int idCuentaAtencion;
    private String estadoCuenta = "";
    private String numeroCuenta = "";
    private java.math.BigDecimal totalCuenta = java.math.BigDecimal.ZERO;
    private String estadoReportePago = "";
    private String motivoReportePago = "";
    private PrecioConsultaDTO precioConsulta = PrecioConsultaDTO.SIN_PRECIO;
    private Timestamp fechaCreacion;
    private String usuarioRegistro = "";

    private static String limpio(String v) { return v == null ? "" : v; }

    // ---------- estado ----------
    public boolean isReservada() { return RESERVADA.equals(estadoCita); }
    public boolean isConfirmada() { return CONFIRMADA.equals(estadoCita); }
    public boolean isAtendida() { return ATENDIDA.equals(estadoCita); }
    public boolean isNoAsistio() { return NO_ASISTIO.equals(estadoCita); }
    public boolean isCancelada() { return CANCELADA.equals(estadoCita); }
    /** Aún ocupa el turno y puede pasar algo (llegar, atenderse, cancelarse). */
    public boolean isActiva() { return isReservada() || isConfirmada(); }
    public boolean isDelPortal() { return CANAL_PORTAL.equals(canal); }
    public boolean isTieneCuenta() { return idCuentaAtencion > 0 && !"ANULADA".equals(estadoCuenta); }
    public boolean isCuentaPagada() { return "PAGADA".equals(estadoCuenta); }
    public boolean isPagoEnRevision() { return "PENDIENTE".equals(estadoReportePago) && !isCuentaPagada(); }
    public boolean isPagoRechazado() { return "RECHAZADO".equals(estadoReportePago) && !isCuentaPagada(); }
    public boolean isPuedePagarEnLinea() { return isActiva() && !isCuentaPagada() && !isPagoEnRevision() && getPrecioConsulta() != null && getPrecioConsulta().isDefinido(); }
    public String getEstadoReportePago() { return estadoReportePago; }
    public void setEstadoReportePago(String v) { estadoReportePago = limpio(v); }
    public String getMotivoReportePago() { return motivoReportePago; }
    public void setMotivoReportePago(String v) { motivoReportePago = limpio(v); }

    public LocalDateTime getInicio() { return fecha.atTime(horaInicio); }
    public LocalDateTime getFin() { return fecha.atTime(horaFin); }

    /** Texto del estado para el usuario. */
    public String getEstadoLegible() { return estadoLegible(estadoCita); }

    public static String estadoLegible(String estado) {
        switch (estado == null ? "" : estado) {
            case RESERVADA: return "Reservada";
            case CONFIRMADA: return "Llegó";
            case ATENDIDA: return "Atendida";
            case NO_ASISTIO: return "No asistió";
            case CANCELADA: return "Cancelada";
            default: return estado;
        }
    }

    /** Clase de Bootstrap del estado (mismo color en toda la aplicación: consistencia, Nielsen #4). */
    public String getClaseEstado() { return claseEstado(estadoCita); }

    public static String claseEstado(String estado) {
        switch (estado == null ? "" : estado) {
            case RESERVADA: return "badge-info";
            case CONFIRMADA: return "badge-warning";
            case ATENDIDA: return "badge-success";
            case NO_ASISTIO: return "badge-secondary";
            case CANCELADA: return "badge-danger";
            default: return "badge-light";
        }
    }

    public String getIconoEstado() { return iconoEstado(estadoCita); }

    public static String iconoEstado(String estado) {
        switch (estado == null ? "" : estado) {
            case RESERVADA: return "fas fa-calendar-check";
            case CONFIRMADA: return "fas fa-user-clock";
            case ATENDIDA: return "fas fa-check-circle";
            case NO_ASISTIO: return "fas fa-user-times";
            case CANCELADA: return "fas fa-ban";
            default: return "fas fa-circle";
        }
    }

    /** Edad del paciente en años (o null). */
    public Integer getEdadPaciente() {
        return fechaNacimientoPaciente == null ? null
                : java.time.Period.between(fechaNacimientoPaciente, fecha == null ? LocalDate.now() : fecha).getYears();
    }

    // ---------- getters / setters ----------
    public int getIdCita() { return idCita; }
    public void setIdCita(int v) { idCita = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { codigo = limpio(v); }
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int v) { idPaciente = v; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String v) { nombrePaciente = limpio(v); }
    public String getDniPaciente() { return dniPaciente; }
    public void setDniPaciente(String v) { dniPaciente = limpio(v); }
    public String getNumeroHistoria() { return numeroHistoria; }
    public void setNumeroHistoria(String v) { numeroHistoria = limpio(v); }
    public int getIdUsuarioPaciente() { return idUsuarioPaciente; }
    public void setIdUsuarioPaciente(int v) { idUsuarioPaciente = v; }
    public String getCorreoPaciente() { return correoPaciente; }
    public void setCorreoPaciente(String v) { correoPaciente = limpio(v); }
    public String getCelularPaciente() { return celularPaciente; }
    public void setCelularPaciente(String v) { celularPaciente = limpio(v); }
    public String getObservacionesPaciente() { return observacionesPaciente; }
    public void setObservacionesPaciente(String v) { observacionesPaciente = limpio(v); }
    public LocalDate getFechaNacimientoPaciente() { return fechaNacimientoPaciente; }
    public void setFechaNacimientoPaciente(LocalDate v) { fechaNacimientoPaciente = v; }
    public String getCoberturaPaciente() { return coberturaPaciente; }
    public void setCoberturaPaciente(String v) { coberturaPaciente = limpio(v); }
    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int v) { idMedico = v; }
    public int getIdUsuarioMedico() { return idUsuarioMedico; }
    public void setIdUsuarioMedico(int v) { idUsuarioMedico = v; }
    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String v) { nombreMedico = limpio(v); }
    public int getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(int v) { idEspecialidad = v; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String v) { especialidad = limpio(v); }
    public String getConsultorio() { return consultorio; }
    public void setConsultorio(String v) { consultorio = limpio(v); }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { fecha = v; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime v) { horaInicio = v; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime v) { horaFin = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { motivo = limpio(v); }
    public String getEstadoCita() { return estadoCita; }
    public void setEstadoCita(String v) { estadoCita = v; }
    public String getCanal() { return canal; }
    public void setCanal(String v) { canal = v; }
    public int getReprogramaciones() { return reprogramaciones; }
    public void setReprogramaciones(int v) { reprogramaciones = v; }
    public Timestamp getFechaLlegada() { return fechaLlegada; }
    public void setFechaLlegada(Timestamp v) { fechaLlegada = v; }
    public Timestamp getFechaAtencion() { return fechaAtencion; }
    public void setFechaAtencion(Timestamp v) { fechaAtencion = v; }
    public String getNotaAtencion() { return notaAtencion; }
    public void setNotaAtencion(String v) { notaAtencion = limpio(v); }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String v) { motivoCancelacion = limpio(v); }
    public Timestamp getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(Timestamp v) { fechaCancelacion = v; }
    public String getCanceladaPor() { return canceladaPor; }
    public void setCanceladaPor(String v) { canceladaPor = limpio(v); }
    public int getIdCuentaAtencion() { return idCuentaAtencion; }
    public void setIdCuentaAtencion(int v) { idCuentaAtencion = v; }
    public String getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(String v) { estadoCuenta = limpio(v); }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String v) { numeroCuenta = limpio(v); }
    public java.math.BigDecimal getTotalCuenta() { return totalCuenta; }
    public void setTotalCuenta(java.math.BigDecimal v) { totalCuenta = v == null ? java.math.BigDecimal.ZERO : v; }
    public PrecioConsultaDTO getPrecioConsulta() { return precioConsulta; }
    public void setPrecioConsulta(PrecioConsultaDTO v) { precioConsulta = v == null ? PrecioConsultaDTO.SIN_PRECIO : v; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = limpio(v); }
}
