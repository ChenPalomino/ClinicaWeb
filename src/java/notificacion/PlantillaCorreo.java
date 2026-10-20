package notificacion;

/** Marco visual común de los correos de la clínica (cabecera azul, cuerpo y pie). */
public final class PlantillaCorreo {

    private PlantillaCorreo() {
    }

    public static String envolver(String titulo, String cuerpo) {
        return "<div style=\"font-family:Segoe UI,Arial,sans-serif;background:#f4f6f9;padding:24px;\">"
                + "<div style=\"max-width:520px;margin:auto;background:#ffffff;border-radius:10px;overflow:hidden;\">"
                + "<div style=\"background:#002244;color:#ffffff;padding:18px 24px;font-size:18px;font-weight:bold;\">"
                + "Clínica Modelo San Martín</div>"
                + "<div style=\"padding:24px;color:#1e293b;font-size:15px;line-height:1.5;\">"
                + "<h2 style=\"margin-top:0;color:#002244;\">" + titulo + "</h2>" + cuerpo + "</div>"
                + "<div style=\"padding:14px 24px;background:#f8fafc;color:#94a3b8;font-size:12px;\">"
                + "Este es un mensaje automático, por favor no respondas.</div></div></div>";
    }
}
