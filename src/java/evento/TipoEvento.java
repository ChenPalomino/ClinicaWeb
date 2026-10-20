package evento;

/**
 * Catálogo de eventos que el sistema publica. Cada uno sabe a qué entidad
 * pertenece y cómo se lee en pantalla (la vista de auditoría lo usa).
 */
public enum TipoEvento {

    // Sesión y seguridad
    INICIO_SESION("SESION", "Inició sesión"),
    INICIO_FALLIDO("SESION", "Contraseña incorrecta"),
    CUENTA_BLOQUEADA_INTENTOS("SESION", "Cuenta bloqueada por 3 intentos"),
    CIERRE_SESION("SESION", "Cerró sesión"),
    SESION_EXPULSADA("SESION", "Sesión cerrada: la cuenta cambió"),
    CONTRASENA_CAMBIADA("USUARIO", "Cambió su contraseña"),
    PERFIL_EDITADO("USUARIO", "Editó su perfil"),
    // Usuarios (Administrador)
    USUARIO_CREADO("USUARIO", "Creó un usuario"),
    USUARIO_EDITADO("USUARIO", "Editó un usuario"),
    USUARIO_DESACTIVADO("USUARIO", "Desactivó un usuario"),
    USUARIO_REACTIVADO("USUARIO", "Reactivó un usuario"),
    USUARIO_BLOQUEADO("USUARIO", "Bloqueó una cuenta"),
    USUARIO_DESBLOQUEADO("USUARIO", "Desbloqueó una cuenta"),
    // Pacientes
    PACIENTE_REGISTRADO("PACIENTE", "Registró un paciente"),
    PACIENTE_EDITADO("PACIENTE", "Editó un paciente"),
    PACIENTE_DESACTIVADO("PACIENTE", "Desactivó un paciente"),
    PACIENTE_REACTIVADO("PACIENTE", "Reactivó un paciente"),
    PACIENTE_AUTORREGISTRO("PACIENTE", "Creó su cuenta en el portal"),
    // Proveedores
    PROVEEDOR_REGISTRADO("PROVEEDOR", "Registró un proveedor"),
    PROVEEDOR_EDITADO("PROVEEDOR", "Editó un proveedor"),
    PROVEEDOR_DESACTIVADO("PROVEEDOR", "Desactivó un proveedor"),
    PROVEEDOR_REACTIVADO("PROVEEDOR", "Reactivó un proveedor"),
    // Tarifario
    ITEM_REGISTRADO("TARIFARIO", "Agregó al tarifario"),
    ITEM_EDITADO("TARIFARIO", "Editó el tarifario"),
    ITEM_DESACTIVADO("TARIFARIO", "Desactivó del tarifario"),
    ITEM_REACTIVADO("TARIFARIO", "Reactivó en el tarifario"),
    // Cobranza en caja (Etapa 6)
    CUENTA_ABIERTA("COBRANZA", "Abrió una cuenta de atención"),
    PAGO_REGISTRADO("COBRANZA", "Cobró y emitió comprobante"),
    PAGO_RECHAZADO("COBRANZA", "Pago rechazado"),
    CUENTA_ANULADA("COBRANZA", "Anuló una cuenta"),
    CAJA_CERRADA("COBRANZA", "Cerró su caja"),
    // Citas médicas (PP-01, RF-21)
    CITA_RESERVADA("CITA", "Reservó una cita"),
    CITA_REPROGRAMADA("CITA", "Reprogramó una cita"),
    CITA_CANCELADA("CITA", "Canceló una cita"),
    CITA_LLEGADA("CITA", "Registró la llegada del paciente"),
    CITA_ATENDIDA("CITA", "Registró la atención de una cita"),
    CITA_NO_ASISTIO("CITA", "Marcó que el paciente no asistió"),
    // Médicos y horarios (RF-03)
    MEDICO_EDITADO("MEDICO", "Editó los datos y el horario de un médico"),
    MEDICO_PAUSADO("MEDICO", "Pausó las citas de un médico"),
    MEDICO_REANUDADO("MEDICO", "Reanudó las citas de un médico"),
    // Configuración
    PARAMETROS_EDITADOS("CONFIGURACION", "Editó los datos de la clínica");

    private final String entidad;
    private final String descripcion;

    TipoEvento(String entidad, String descripcion) {
        this.entidad = entidad;
        this.descripcion = descripcion;
    }

    public String getEntidad() { return entidad; }
    public String getDescripcion() { return descripcion; }

    /** Descripción legible a partir del nombre guardado en la BD (o el mismo texto si no existe). */
    public static String describir(String accion) {
        for (TipoEvento t : values()) {
            if (t.name().equals(accion)) {
                return t.descripcion;
            }
        }
        return accion;
    }
}
