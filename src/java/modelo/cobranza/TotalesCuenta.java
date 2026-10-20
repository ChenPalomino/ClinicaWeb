package modelo.cobranza;

import java.math.BigDecimal;

/** RF-11: totales de la cuenta (lo que va en la cabecera y en el comprobante). Inmutable. */
public final class TotalesCuenta {

    public static final TotalesCuenta CERO = new TotalesCuenta(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO);

    private final BigDecimal opGravada;        // base imponible (sin IGV)
    private final BigDecimal opExonerada;      // líneas no afectas a IGV
    private final BigDecimal igv;
    private final BigDecimal montoAseguradora;
    private final BigDecimal montoDescuento;

    public TotalesCuenta(BigDecimal opGravada, BigDecimal opExonerada, BigDecimal igv,
                         BigDecimal montoAseguradora, BigDecimal montoDescuento) {
        this.opGravada = opGravada;
        this.opExonerada = opExonerada;
        this.igv = igv;
        this.montoAseguradora = montoAseguradora;
        this.montoDescuento = montoDescuento;
    }

    public BigDecimal getOpGravada() { return opGravada; }
    public BigDecimal getOpExonerada() { return opExonerada; }
    public BigDecimal getIgv() { return igv; }
    public BigDecimal getMontoAseguradora() { return montoAseguradora; }
    public BigDecimal getMontoDescuento() { return montoDescuento; }

    /** Lo que paga el paciente. */
    public BigDecimal getTotal() { return opGravada.add(opExonerada).add(igv); }
}
