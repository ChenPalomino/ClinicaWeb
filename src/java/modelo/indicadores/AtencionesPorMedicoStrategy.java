package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: atenciones por médico. */
public class AtencionesPorMedicoStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "ATENCIONES_MEDICO"; }
    @Override public String titulo() { return "Atenciones por médico"; }
    @Override public String descripcion() { return "Pacientes atendidos e inasistencias por médico (los 10 con más atenciones)."; }
    @Override public String icono() { return "fas fa-user-md"; }
    @Override public String grupo() { return "Citas"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.atencionesPorMedico(p.desde(), p.hasta(), 10);
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS_HORIZONTALES, IndicadorDTO.NUMERO)
                .etiquetas(Calculo.etiquetas(filas))
                .serie("Atendidas", Calculo.valores(filas))
                .serie("No asistieron", Calculo.valores2(filas))
                .dato("Atenciones", Calculo.entero(Calculo.suma(filas)), p.texto())
                .dato("Inasistencias", Calculo.entero(Calculo.suma2(filas)), "turnos perdidos")
                .enlace("/admin/medicos");
    }
}
