package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: demanda por especialidad. */
public class CitasPorEspecialidadStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "CITAS_ESPECIALIDAD"; }
    @Override public String titulo() { return "Demanda por especialidad"; }
    @Override public String descripcion() { return "Citas programadas y atendidas en cada especialidad."; }
    @Override public String icono() { return "fas fa-stethoscope"; }
    @Override public String grupo() { return "Citas"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.citasPorEspecialidad(p.desde(), p.hasta());
        IndicadorDTO r = new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS_HORIZONTALES, IndicadorDTO.NUMERO)
                .etiquetas(Calculo.etiquetas(filas))
                .serie("Programadas", Calculo.valores(filas))
                .serie("Atendidas", Calculo.valores2(filas))
                .dato("Citas programadas", Calculo.entero(Calculo.suma(filas)), p.texto())
                .enlace("/admin/especialidades");
        if (!filas.isEmpty()) {
            r.dato("Más pedida", filas.get(0).etiqueta(), Calculo.entero(filas.get(0).valor()) + " citas");
        }
        return r;
    }
}
