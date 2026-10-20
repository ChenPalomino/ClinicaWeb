package modelo.cobranza.comprobante;

/** PATRÓN: Factory Method  ->  PRODUCTO CONCRETO. Factura: para empresas (RUC); permite usar el IGV como crédito fiscal. */
public class Factura extends Comprobante {

    @Override
    public String getTipo() {
        return "FACTURA";
    }

    @Override
    public String getTitulo() {
        return "FACTURA ELECTRÓNICA";
    }
}
