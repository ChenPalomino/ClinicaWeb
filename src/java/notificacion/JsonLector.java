package notificacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector de JSON mínimo (sin librerías externas) para leer las respuestas de
 * la API de Brevo: objetos -> Map, arreglos -> List, textos, números, true/false/null.
 */
final class JsonLector {

    private final String texto;
    private int pos;

    private JsonLector(String texto) {
        this.texto = texto;
    }

    static Object leer(String json) {
        JsonLector l = new JsonLector(json == null ? "" : json);
        l.espacios();
        Object valor = l.valor();
        l.espacios();
        if (l.pos != l.texto.length()) {
            throw new IllegalArgumentException("JSON con datos de más en la posición " + l.pos);
        }
        return valor;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> objeto(Object v) { return v instanceof Map ? (Map<String, Object>) v : Map.of(); }

    @SuppressWarnings("unchecked")
    static List<Object> lista(Object v) { return v instanceof List ? (List<Object>) v : List.of(); }

    static String texto(Object v) { return v == null ? "" : String.valueOf(v); }

    private Object valor() {
        if (pos >= texto.length()) throw new IllegalArgumentException("JSON incompleto");
        char c = texto.charAt(pos);
        switch (c) {
            case '{': return leerObjeto();
            case '[': return leerLista();
            case '"': return leerTexto();
            case 't': esperar("true"); return Boolean.TRUE;
            case 'f': esperar("false"); return Boolean.FALSE;
            case 'n': esperar("null"); return null;
            default: return leerNumero();
        }
    }

    private Map<String, Object> leerObjeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        pos++;
        espacios();
        if (siguiente('}')) return mapa;
        do {
            espacios();
            String clave = leerTexto();
            espacios();
            if (!siguiente(':')) throw new IllegalArgumentException("Falta ':' en " + pos);
            espacios();
            mapa.put(clave, valor());
            espacios();
        } while (siguiente(','));
        if (!siguiente('}')) throw new IllegalArgumentException("Falta '}' en " + pos);
        return mapa;
    }

    private List<Object> leerLista() {
        List<Object> lista = new ArrayList<>();
        pos++;
        espacios();
        if (siguiente(']')) return lista;
        do {
            espacios();
            lista.add(valor());
            espacios();
        } while (siguiente(','));
        if (!siguiente(']')) throw new IllegalArgumentException("Falta ']' en " + pos);
        return lista;
    }

    private String leerTexto() {
        if (!siguiente('"')) throw new IllegalArgumentException("Se esperaba texto en " + pos);
        StringBuilder sb = new StringBuilder();
        while (pos < texto.length()) {
            char c = texto.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            sb.append(escape(texto.charAt(pos++)));
        }
        throw new IllegalArgumentException("Texto sin cerrar");
    }

    private char escape(char e) {
        switch (e) {
            case 'n': return '\n';
            case 't': return '\t';
            case 'r': return '\r';
            case 'b': return '\b';
            case 'f': return '\f';
            case 'u': {
                char c = (char) Integer.parseInt(texto.substring(pos, pos + 4), 16);
                pos += 4;
                return c;
            }
            default: return e;
        }
    }

    private Object leerNumero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) pos++;
        String n = texto.substring(inicio, pos);
        if (n.isEmpty()) throw new IllegalArgumentException("Valor inesperado en " + inicio);
        return n.contains(".") || n.contains("e") || n.contains("E") ? (Object) Double.valueOf(n) : (Object) Long.valueOf(n);
    }

    private void esperar(String palabra) {
        if (!texto.startsWith(palabra, pos)) throw new IllegalArgumentException("Se esperaba " + palabra);
        pos += palabra.length();
    }

    private boolean siguiente(char c) {
        if (pos < texto.length() && texto.charAt(pos) == c) { pos++; return true; }
        return false;
    }

    private void espacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) pos++;
    }
}
