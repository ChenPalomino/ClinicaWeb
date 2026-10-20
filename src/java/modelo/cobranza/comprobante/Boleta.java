package modelo.cobranza.comprobante;

/** PATRÓN: Factory Method  ->  PRODUCTO CONCRETO. Boleta de venta: para personas (DNI). */
public class Boleta extends Comprobante {

    @Override
    public String getTipo() {
        return "BOLETA";
    }

    @Override
    public String getTitulo() {
        return "BOLETA DE VENTA ELECTRÓNICA";
    }
}
