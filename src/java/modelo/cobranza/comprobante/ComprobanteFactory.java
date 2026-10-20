package modelo.cobranza.comprobante;

import modelo.cobranza.TotalesCuenta;
import modelo.dto.DatosClienteDTO;
import modelo.dto.ParametroClinicaDTO;

import java.util.Map;

/**
 * PATRÓN: Factory Method (creacional)  ->  CREADOR (abstracto)
 *
 * emitir() es el algoritmo común para cualquier comprobante: valida los datos
 * del cliente, CREA el comprobante y lo llena. El paso "crear" es el método
 * fábrica crearComprobante(): cada subclase decide qué producto concreto
 * fabrica (BoletaFactory -> Boleta, FacturaFactory -> Factura).
 * CobranzaService solo conoce ComprobanteFactory y Comprobante: agregar una
 * "nota de venta" = un producto y una fábrica nuevos (abierto/cerrado).
 */
public abstract class ComprobanteFactory {

    /** EL MÉTODO FÁBRICA. */
    protected abstract Comprobante crearComprobante();

    /** Serie con la que se numera este tipo de comprobante (B001 o F001). */
    public abstract String getSerie();

    /** Reglas del cliente propias de cada comprobante. @return campo -> mensaje (vacío si todo está bien) */
    public abstract Map<String, String> validarCliente(DatosClienteDTO cliente, TotalesCuenta totales,
                                                       ParametroClinicaDTO parametros);

    /** Tipo de documento que se imprime ("DNI", "RUC" o "-") según lo que el cajero ingresó. */
    protected abstract String tipoDocumento(DatosClienteDTO cliente);

    /**
     * Operación plantilla: valida, fabrica (factory method) y llena el comprobante.
     * El número correlativo se asigna después, al guardar (PagoRepository).
     * @throws IllegalArgumentException si los datos del cliente no son válidos
     */
    public final Comprobante emitir(DatosClienteDTO cliente, TotalesCuenta totales, ParametroClinicaDTO parametros) {
        Map<String, String> errores = validarCliente(cliente, totales, parametros);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errores.values()));
        }
        Comprobante comprobante = crearComprobante();
        String tipoDoc = tipoDocumento(cliente);
        comprobante.llenar(tipoDoc, "-".equals(tipoDoc) ? null : cliente.getNumeroDocumento(),
                cliente.getNombre(), cliente.getDireccion().isEmpty() ? null : cliente.getDireccion(),
                totales, getSerie());
        return comprobante;
    }

    /** Elige la fábrica concreta según lo que pidió el cliente. */
    public static ComprobanteFactory para(String tipoComprobante) {
        if ("FACTURA".equals(tipoComprobante)) {
            return new FacturaFactory();
        }
        if ("BOLETA".equals(tipoComprobante)) {
            return new BoletaFactory();
        }
        throw new IllegalArgumentException("Tipo de comprobante desconocido: " + tipoComprobante);
    }
}
