package modelo.cobranza.comprobante;

import modelo.cobranza.TotalesCuenta;

import java.math.BigDecimal;

/**
 * PATRÓN: Factory Method  ->  PRODUCTO (abstracto)
 *
 * Comprobante de pago electrónico (RF-15). Boleta y Factura comparten los
 * montos y la numeración "SERIE-NÚMERO", pero cada una tiene su título,
 * su serie y su forma de identificar al cliente (DNI o RUC).
 * La serie y el número correlativo los asigna la base de datos dentro de la
 * misma transacción del pago (RNF-08), así nunca se repiten.
 */
public abstract class Comprobante {

    private String serie;
    private int numero;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombreCliente;
    private String direccionCliente;
    private BigDecimal opGravada;
    private BigDecimal opExonerada;
    private BigDecimal igv;
    private BigDecimal total;

    /** BOLETA o FACTURA (se guarda en tt_comprobante.tipo_comprobante). */
    public abstract String getTipo();

    /** Título impreso, ej. "BOLETA DE VENTA ELECTRÓNICA". */
    public abstract String getTitulo();

    void llenar(String tipoDocumento, String numeroDocumento, String nombreCliente, String direccionCliente,
                TotalesCuenta totales, String serie) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombreCliente = nombreCliente;
        this.direccionCliente = direccionCliente;
        this.opGravada = totales.getOpGravada();
        this.opExonerada = totales.getOpExonerada();
        this.igv = totales.getIgv();
        this.total = totales.getTotal();
        this.serie = serie;
    }

    /** B001-00000012 */
    public static String numeroCompleto(String serie, int numero) {
        return serie + "-" + String.format("%08d", numero);
    }

    public String getNumeroCompleto() { return numeroCompleto(serie, numero); }

    public String getSerie() { return serie; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombreCliente() { return nombreCliente; }
    public String getDireccionCliente() { return direccionCliente; }
    public BigDecimal getOpGravada() { return opGravada; }
    public BigDecimal getOpExonerada() { return opExonerada; }
    public BigDecimal getIgv() { return igv; }
    public BigDecimal getTotal() { return total; }
}
