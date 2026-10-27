package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: servicios y productos más vendidos. */
public class MasVendidosStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "MAS_VENDIDOS"; }
    @Override public String titulo() { return "Servicios y productos más vendidos"; }
    @Override public String descripcion() { return "Los 10 que más ingresos dejaron en cuentas pagadas."; }
    @Override public String icono() { return "fas fa-trophy"; }
    @Override public String grupo() { return "Caja"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.masVendidos(p.desde(), p.hasta(), 10);
        IndicadorDTO r = new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS_HORIZONTALES, IndicadorDTO.SOLES)
                .etiquetas(Calculo.etiquetas(filas)).serie("Ingresos", Calculo.valores(filas))
                .dato("Ingresos del top 10", Calculo.soles(Calculo.suma(filas)), p.texto())
                .dato("Unidades", Calculo.entero(Calculo.suma2(filas)), "vendidas o atendidas")
                .enlace("/admin/servicios");
        if (!filas.isEmpty()) {
            r.dato("El primero", filas.get(0).etiqueta(), Calculo.soles(filas.get(0).valor()));
        }
        return r;
    }
}
