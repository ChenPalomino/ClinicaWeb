package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: horas con más citas. */
public class HorasPicoStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "HORAS_PICO"; }
    @Override public String titulo() { return "Horas con más citas"; }
    @Override public String descripcion() { return "A qué hora llegan más pacientes: útil para reforzar recepción."; }
    @Override public String icono() { return "fas fa-clock"; }
    @Override public String grupo() { return "Citas"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.citasPorHora(p.desde(), p.hasta());
        IndicadorDTO r = new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS, IndicadorDTO.NUMERO)
                .etiquetas(filas.stream().map(f -> f.etiqueta() + ":00").toList())
                .serie("Citas", Calculo.valores(filas))
                .dato("Citas", Calculo.entero(Calculo.suma(filas)), p.texto());
        filas.stream().max((a, b) -> a.valor().compareTo(b.valor()))
                .ifPresent(m -> r.dato("Hora pico", m.etiqueta() + ":00", Calculo.entero(m.valor()) + " citas"));
        return r;
    }
}
