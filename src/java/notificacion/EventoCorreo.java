package notificacion;

/** Un evento de entrega informado por Brevo (entregado, bloqueado, rebotado...). */
public final class EventoCorreo {

    private final String fecha;
    private final String destinatario;
    private final String asunto;
    private final String evento;
    private final String motivo;

    public EventoCorreo(String fecha, String destinatario, String asunto, String evento, String motivo) {
        this.fecha = fecha;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.evento = evento;
        this.motivo = motivo;
    }

    public String getFecha() { return fecha; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getEvento() { return evento; }
    public String getMotivo() { return motivo; }

    /** Nombre del evento en español y si es un problema (para colorearlo). */
    public String getEventoLegible() {
        switch (evento) {
            case "requests": return "Solicitado";
            case "delivered": return "Entregado";
            case "opened": case "uniqueOpened": return "Abierto";
            case "clicks": return "Clic en enlace";
            case "deferred": return "Diferido (reintentando)";
            case "softBounces": return "Rebote temporal";
            case "hardBounces": return "Rebote definitivo (no existe)";
            case "bounces": return "Rebotado";
            case "blocked": return "Bloqueado por Brevo";
            case "invalid": return "Correo inválido";
            case "spam": return "Marcado como spam";
            case "error": return "Error";
            case "unsubscribed": return "Desuscrito";
            default: return evento;
        }
    }

    public boolean isProblema() {
        return evento.contains("ounce") || "blocked".equals(evento) || "invalid".equals(evento)
                || "spam".equals(evento) || "error".equals(evento) || "deferred".equals(evento);
    }
}
