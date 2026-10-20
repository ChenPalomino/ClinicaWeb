package modelo.cobranza.comprobante;

import modelo.cobranza.TotalesCuenta;
import modelo.dto.DatosClienteDTO;
import modelo.dto.ParametroClinicaDTO;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATRÓN: Factory Method  ->  CREADOR CONCRETO de boletas.
 * Regla SUNAT: desde cierto monto (parámetro de la clínica, S/ 700 por defecto)
 * la boleta debe llevar el DNI del cliente; por debajo es opcional.
 */
public class BoletaFactory extends ComprobanteFactory {

    @Override
    protected Comprobante crearComprobante() {
        return new Boleta();
    }

    @Override
    public String getSerie() {
        return "B001";
    }

    @Override
    public Map<String, String> validarCliente(DatosClienteDTO c, TotalesCuenta totales, ParametroClinicaDTO p) {
        Map<String, String> errores = new LinkedHashMap<>();
        String dni = c.getNumeroDocumento();
        if (dni.isEmpty()) {
            if (totales.getTotal().compareTo(p.getMontoBoletaDni()) >= 0) {
                errores.put("numeroDocumento", "Desde S/ " + p.getMontoBoletaDni().setScale(2)
                        + " la boleta debe llevar el DNI del cliente.");
            }
        } else if (!dni.matches("\\d{8}")) {
            errores.put("numeroDocumento", "El DNI tiene 8 dígitos.");
        }
        if (c.getNombre().length() < 3 || c.getNombre().length() > 150) {
            errores.put("nombre", "Escribe el nombre del cliente (de 3 a 150 caracteres).");
        }
        if (c.getDireccion().length() > 200) {
            errores.put("direccion", "La dirección no puede pasar de 200 caracteres.");
        }
        return errores;
    }

    @Override
    protected String tipoDocumento(DatosClienteDTO c) {
        return c.getNumeroDocumento().isEmpty() ? "-" : "DNI";
    }
}
