package modelo.cobranza.tarifa;

import java.math.BigDecimal;

/**
 * Resultado de aplicar una política tarifaria a UNA línea de la cuenta.
 * Inmutable: lo calcula la estrategia y el servicio lo guarda en el detalle.
 */
public final class LineaTarifada {

    private final BigDecimal bruto;               // precio de lista x cantidad (con IGV)
    private final BigDecimal porcentajeCubierto;  // % que no paga el paciente
    private final BigDecimal montoAseguradora;    // a cargo de la EPS o del seguro
    private final BigDecimal montoDescuento;      // descuento por convenio
    private final BigDecimal importe;             // lo que paga el paciente

    public LineaTarifada(BigDecimal bruto, BigDecimal porcentajeCubierto, BigDecimal montoAseguradora,
                         BigDecimal montoDescuento) {
        this.bruto = bruto;
        this.porcentajeCubierto = porcentajeCubierto;
        this.montoAseguradora = montoAseguradora;
        this.montoDescuento = montoDescuento;
        this.importe = bruto.subtract(montoAseguradora).subtract(montoDescuento);
    }

    public BigDecimal getBruto() { return bruto; }
    public BigDecimal getPorcentajeCubierto() { return porcentajeCubierto; }
    public BigDecimal getMontoAseguradora() { return montoAseguradora; }
    public BigDecimal getMontoDescuento() { return montoDescuento; }
    public BigDecimal getImporte() { return importe; }
}
