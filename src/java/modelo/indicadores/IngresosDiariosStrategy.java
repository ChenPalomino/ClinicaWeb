package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: ingresos por día. */
public class IngresosDiariosStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "INGRESOS_DIA"; }
    @Override public String titulo() { return "Ingresos por día"; }
    @Override public String descripcion() { return "Cuánto se cobró cada día (pagos aprobados)."; }
    @Override public String icono() { return "fas fa-chart-line"; }
    @Override public String grupo() { return "Caja"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.ingresosPorDia(p.desde(), p.hasta());
        Map<String, BigDecimal[]> serie = Calculo.porFecha(p, filas);
        BigDecimal total = Calculo.suma(filas), pagos = Calculo.suma2(filas);
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.LINEA, IndicadorDTO.SOLES)
                .etiquetas(List.copyOf(serie.keySet()))
                .serie("Cobrado", Calculo.columna(serie, 0))
                .dato("Total cobrado", Calculo.soles(total), p.texto())
                .dato("Pagos", Calculo.entero(pagos), "aprobados")
                .dato("Ticket promedio", Calculo.soles(Calculo.dividir(total, pagos)), "por pago")
                .dato("Promedio diario", Calculo.soles(Calculo.dividir(total, BigDecimal.valueOf(p.dias()))), p.dias() + " días")
                .nota(p.dias() > Calculo.DIAS_DETALLE_DIARIO ? "Periodo largo: se agrupa por mes." : "")
                .enlace("/caja/cierre");
    }
}
