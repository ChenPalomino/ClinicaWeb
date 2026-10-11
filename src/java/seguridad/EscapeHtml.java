package seguridad;

/**
 * Escapa texto antes de mostrarlo en un JSP para evitar ataques XSS
 * (que alguien escriba código HTML/JavaScript en un campo del formulario).
 * Uso en JSP: <%= seguridad.EscapeHtml.texto(valor) %>
 */
public final class EscapeHtml {

    private EscapeHtml() {
    }

    public static String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        String s = valor.toString();
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
