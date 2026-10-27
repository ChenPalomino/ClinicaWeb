package modelo.dto;

import java.math.BigDecimal;

public final class PrecioConsultaDTO {

    public static final PrecioConsultaDTO SIN_PRECIO = new PrecioConsultaDTO(null, null, BigDecimal.ZERO, "", "PARTICULAR");

    private final BigDecimal precioLista;
    private final BigDecimal aPagar;
    private final BigDecimal cubre;
    private final String cobertura;
    private final String tipoCobertura;

    public PrecioConsultaDTO(BigDecimal precioLista, BigDecimal aPagar, BigDecimal cubre, String cobertura, String tipoCobertura) {
        this.precioLista = precioLista;
        this.aPagar = aPagar;
        this.cubre = cubre == null ? BigDecimal.ZERO : cubre;
        this.cobertura = cobertura == null ? "" : cobertura;
        this.tipoCobertura = tipoCobertura == null ? "PARTICULAR" : tipoCobertura;
    }

    public boolean isDefinido() { return precioLista != null; }
    public boolean isConCobertura() { return isDefinido() && cubre.signum() > 0; }
    public boolean isConvenio() { return "CONVENIO".equals(tipoCobertura); }
    public BigDecimal getPrecioLista() { return precioLista; }
    public BigDecimal getAPagar() { return aPagar; }
    public BigDecimal getCubre() { return cubre; }
    public String getCobertura() { return cobertura; }
    public String getTipoCobertura() { return tipoCobertura; }

    public String getDetalleCobertura() {
        if (!isConCobertura()) {
            return "";
        }
        String quien = cobertura.isEmpty() ? "Tu cobertura" : cobertura;
        return isConvenio() ? "Descuento " + quien + ": S/ " + soles(cubre)
                            : quien + " cubre S/ " + soles(cubre);
    }

    public static String soles(BigDecimal monto) {
        return monto == null ? "" : String.format(java.util.Locale.US, "%,.2f", monto);
    }
}
