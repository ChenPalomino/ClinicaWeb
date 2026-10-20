package modelo.cobranza.comprobante;

import modelo.cobranza.TotalesCuenta;
import modelo.dto.DatosClienteDTO;
import modelo.dto.ParametroClinicaDTO;
import modelo.servicios.Validaciones;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATRÓN: Factory Method  ->  CREADOR CONCRETO de facturas.
 * La factura exige RUC válido (dígito verificador SUNAT), razón social y dirección fiscal.
 */
public class FacturaFactory extends ComprobanteFactory {

    @Override
    protected Comprobante crearComprobante() {
        return new Factura();
    }

    @Override
    public String getSerie() {
        return "F001";
    }

    @Override
    public Map<String, String> validarCliente(DatosClienteDTO c, TotalesCuenta totales, ParametroClinicaDTO p) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!Validaciones.ruc(c.getNumeroDocumento())) {
            errores.put("numeroDocumento", "Escribe un RUC válido de 11 dígitos (empieza con 10, 15, 17 o 20).");
        }
        if (c.getNombre().length() < 3 || c.getNombre().length() > 150) {
            errores.put("nombre", "Escribe la razón social de la empresa (de 3 a 150 caracteres).");
        }
        if (c.getDireccion().length() < 5 || c.getDireccion().length() > 200) {
            errores.put("direccion", "La factura necesita la dirección fiscal de la empresa.");
        }
        return errores;
    }

    @Override
    protected String tipoDocumento(DatosClienteDTO c) {
        return "RUC";
    }
}
