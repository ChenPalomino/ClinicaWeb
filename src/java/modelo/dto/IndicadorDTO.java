package modelo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class IndicadorDTO {

    public static final String BARRAS = "bar";
    public static final String BARRAS_HORIZONTALES = "barh";
    public static final String LINEA = "line";
    public static final String DONA = "doughnut";
    public static final String SOLES = "soles";
    public static final String NUMERO = "numero";

    public record Serie(String nombre, List<BigDecimal> valores) { }
    public record Dato(String etiqueta, String valor, String detalle) { }

    private final String codigo;
    private final String titulo;
    private final String tipoGrafico;
    private final String formato;
    private final List<String> etiquetas = new ArrayList<>();
    private final List<Serie> series = new ArrayList<>();
    private final List<Dato> resumen = new ArrayList<>();
    private String nota = "";
    private String enlace = "";

    public IndicadorDTO(String codigo, String titulo, String tipoGrafico, String formato) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.tipoGrafico = tipoGrafico;
        this.formato = formato;
    }

    public IndicadorDTO etiquetas(List<String> v) { etiquetas.addAll(v); return this; }
    public IndicadorDTO serie(String nombre, List<BigDecimal> valores) { series.add(new Serie(nombre, valores)); return this; }
    public IndicadorDTO dato(String etiqueta, String valor, String detalle) { resumen.add(new Dato(etiqueta, valor, detalle == null ? "" : detalle)); return this; }
    public IndicadorDTO nota(String v) { nota = v == null ? "" : v; return this; }
    public IndicadorDTO enlace(String v) { enlace = v == null ? "" : v; return this; }

    public boolean isVacio() {
        return series.stream().allMatch(s -> s.valores().stream().allMatch(v -> v.signum() == 0));
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getTipoGrafico() { return tipoGrafico; }
    public String getFormato() { return formato; }
    public List<String> getEtiquetas() { return etiquetas; }
    public List<Serie> getSeries() { return series; }
    public List<Dato> getResumen() { return resumen; }
    public String getNota() { return nota; }
    public String getEnlace() { return enlace; }
}
