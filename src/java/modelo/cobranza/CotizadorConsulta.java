package modelo.cobranza;

import modelo.cobranza.tarifa.LineaTarifada;
import modelo.cobranza.tarifa.PoliticaTarifariaFactory;
import modelo.cobranza.tarifa.PoliticaTarifariaStrategy;
import modelo.dto.CoberturaDTO;
import modelo.dto.PrecioConsultaDTO;
import modelo.dto.ProductoServicioDTO;

import java.math.BigDecimal;

public final class CotizadorConsulta {

    private CotizadorConsulta() {
    }

    public static PrecioConsultaDTO cotizar(BigDecimal precioLista, CoberturaDTO cobertura) {
        if (precioLista == null) {
            return PrecioConsultaDTO.SIN_PRECIO;
        }
        String tipo = cobertura == null || cobertura.getTipo() == null ? "PARTICULAR" : cobertura.getTipo();
        BigDecimal porcentaje = cobertura == null || cobertura.getPorcentaje() == null ? BigDecimal.ZERO : cobertura.getPorcentaje();
        PoliticaTarifariaStrategy politica;
        try {
            politica = PoliticaTarifariaFactory.obtener(tipo);
        } catch (IllegalArgumentException e) {
            politica = PoliticaTarifariaFactory.obtener("PARTICULAR");
            tipo = "PARTICULAR";
        }
        LineaTarifada linea = politica.tarifar(ProductoServicioDTO.SERVICIO, precioLista, 1, porcentaje);
        return new PrecioConsultaDTO(linea.getBruto(), linea.getImporte(),
                linea.getMontoAseguradora().add(linea.getMontoDescuento()),
                cobertura == null ? "" : cobertura.getNombre(), tipo);
    }
}
