package controlador;

import java.util.Map;

public final class JsonUtil {

    private static final Map<Character, String> ESCAPES = Map.of(
            '"', "\\\"", '\\', "\\\\", '\n', "\\n", '\r', "\\r", '\t', "\\t",
            '<', "\\u003c", '>', "\\u003e", '&', "\\u0026");

    private JsonUtil() {
    }

    public static String texto(String v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : v.toCharArray()) {
            String escape = ESCAPES.get(c);
            if (escape != null) {
                sb.append(escape);
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
