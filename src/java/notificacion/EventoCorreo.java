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
        return NOMBRES.getOrDefault(evento, evento);
    }

    private static final java.util.Map<String, String> NOMBRES = java.util.Map.ofEntries(
            java.util.Map.entry("requests", "Solicitado"),
            java.util.Map.entry("delivered", "Entregado"),
            java.util.Map.entry("opened", "Abierto"),
            java.util.Map.entry("uniqueOpened", "Abierto"),
            java.util.Map.entry("clicks", "Clic en enlace"),
            java.util.Map.entry("deferred", "Diferido (reintentando)"),
            java.util.Map.entry("softBounces", "Rebote temporal"),
            java.util.Map.entry("hardBounces", "Rebote definitivo (no existe)"),
            java.util.Map.entry("bounces", "Rebotado"),
            java.util.Map.entry("blocked", "Bloqueado por Brevo"),
            java.util.Map.entry("invalid", "Correo inválido"),
            java.util.Map.entry("spam", "Marcado como spam"),
            java.util.Map.entry("error", "Error"),
            java.util.Map.entry("unsubscribed", "Desuscrito"));

    public boolean isProblema() {
        return evento.contains("ounce") || "blocked".equals(evento) || "invalid".equals(evento)
                || "spam".equals(evento) || "error".equals(evento) || "deferred".equals(evento);
    }
}
