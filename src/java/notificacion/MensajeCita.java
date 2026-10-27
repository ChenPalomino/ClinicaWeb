package notificacion;

import evento.TipoEvento;
import modelo.citas.FechaTexto;
import modelo.dto.CitaDTO;
import modelo.dto.ParametroClinicaDTO;
import seguridad.EscapeHtml;

/**
 * RF-09: el contenido del aviso de una cita (reservada, reprogramada o cancelada),
 * en dos formatos: correo (HTML) y mensaje corto (SMS, máximo 160 caracteres).
 */
public final class MensajeCita {

    private final String asunto;
    private final String titulo;
    private final String html;
    private final String textoCorto;

    private MensajeCita(String asunto, String titulo, String html, String textoCorto) {
        this.asunto = asunto;
        this.titulo = titulo;
        this.html = html;
        this.textoCorto = textoCorto.length() > 160 ? textoCorto.substring(0, 157) + "..." : textoCorto;
    }

    public String getAsunto() { return asunto; }
    public String getTitulo() { return titulo; }
    public String getHtml() { return html; }
    public String getTextoCorto() { return textoCorto; }

    /** null si el tipo de evento no genera aviso al paciente. */
    public static MensajeCita de(TipoEvento tipo, CitaDTO c, ParametroClinicaDTO clinica, int horasCancelacion, String urlPortal) {
        String cuando = FechaTexto.largaTitulo(c.getFecha()) + " a las " + FechaTexto.hora(c.getHoraInicio());
        String clinicaNombre = clinica.getNombreComercial().isEmpty() ? "Clínica Modelo San Martín" : clinica.getNombreComercial();
        String nombre = EscapeHtml.texto(c.getNombrePaciente().split(" ")[0]);
        String ficha = ficha(c, cuando, clinica, tipo != TipoEvento.CITA_CANCELADA);
        String portal = urlPortal == null || urlPortal.isBlank() ? ""
                : "<p><a href=\"" + EscapeHtml.texto(urlPortal) + "\">Ver mis citas en el portal</a></p>";
        switch (tipo) {
            case CITA_RESERVADA:
                return new MensajeCita("Cita reservada: " + cuando, "Tu cita está reservada",
                        "<p>Hola " + nombre + ", tu cita quedó reservada:</p>" + ficha
                        + "<p>Llega <strong>15 minutos antes</strong> con tu DNI.</p>"
                        + "<p style=\"color:#64748b;\">Si no puedes asistir, cancélala o cámbiala desde el portal hasta "
                        + horasCancelacion + " horas antes, así otro paciente puede usar el turno.</p>" + portal,
                        clinicaNombre + ": cita " + c.getCodigo() + " reservada " + corto(c) + " con " + c.getEspecialidad()
                        + (c.getPrecioConsulta().isDefinido() ? " (S/ " + modelo.dto.PrecioConsultaDTO.soles(c.getPrecioConsulta().getAPagar()) + ")" : "")
                        + ". Llega 15 min antes con tu DNI.");
            case CITA_REPROGRAMADA:
                return new MensajeCita("Tu cita cambió de fecha: " + cuando, "Tu cita fue reprogramada",
                        "<p>Hola " + nombre + ", tu cita <strong>" + EscapeHtml.texto(c.getCodigo())
                        + "</strong> ahora es:</p>" + ficha + "<p>El horario anterior quedó libre.</p>" + portal,
                        clinicaNombre + ": tu cita " + c.getCodigo() + " ahora es " + corto(c) + ". El horario anterior quedó libre.");
            case CITA_CANCELADA:
                return new MensajeCita("Cita cancelada: " + cuando, "Tu cita fue cancelada",
                        "<p>Hola " + nombre + ", se canceló tu cita:</p>" + ficha
                        + (c.getMotivoCancelacion().isEmpty() ? "" : "<p><strong>Motivo:</strong> "
                           + EscapeHtml.texto(c.getMotivoCancelacion()) + "</p>")
                        + "<p>Si necesitas otra, puedes reservarla desde el portal o en recepción.</p>" + portal,
                        clinicaNombre + ": se cancelo tu cita " + c.getCodigo() + " del " + corto(c) + ".");
            default:
                return null;
        }
    }

    private static String corto(CitaDTO c) {
        return FechaTexto.dmy(c.getFecha()) + " " + FechaTexto.horaIso(c.getHoraInicio());
    }

    private static String ficha(CitaDTO c, String cuando, ParametroClinicaDTO clinica, boolean conPrecio) {
        return "<table style=\"border-collapse:collapse;margin:12px 0;font-size:15px;\">"
                + fila("Cuándo", EscapeHtml.texto(cuando))
                + fila("Especialidad", EscapeHtml.texto(c.getEspecialidad()))
                + fila("Médico", "Dr(a). " + EscapeHtml.texto(c.getNombreMedico()))
                + fila("Lugar", EscapeHtml.texto(c.getConsultorio())
                       + (clinica.getDireccion().isEmpty() ? "" : " · " + EscapeHtml.texto(clinica.getDireccion())))
                + fila("Código", EscapeHtml.texto(c.getCodigo()))
                + (conPrecio ? fila("Precio", precio(c)) : "")
                + "</table>";
    }

    private static String precio(CitaDTO c) {
        modelo.dto.PrecioConsultaDTO p = c.getPrecioConsulta();
        if (!p.isDefinido()) {
            return "Te lo indicarán en caja";
        }
        String detalle = p.isConCobertura()
                ? "<br><span style=\"color:#64748b;font-weight:normal;\">Precio normal S/ " + modelo.dto.PrecioConsultaDTO.soles(p.getPrecioLista())
                  + " · " + EscapeHtml.texto(p.getDetalleCobertura()) + "</span>"
                : "";
        return "S/ " + modelo.dto.PrecioConsultaDTO.soles(p.getAPagar()) + detalle
                + "<br><span style=\"color:#64748b;font-weight:normal;\">Incluye IGV. Se paga en caja el día de la consulta.</span>";
    }

    private static String fila(String etiqueta, String valor) {
        return "<tr><td style=\"padding:4px 14px 4px 0;color:#64748b;\">" + etiqueta + "</td>"
                + "<td style=\"padding:4px 0;\"><strong>" + valor + "</strong></td></tr>";
    }
}
