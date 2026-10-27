package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: asistencia a las citas. */
public class CitasPorEstadoStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "CITAS_ESTADO"; }
    @Override public String titulo() { return "Asistencia a las citas"; }
    @Override public String descripcion() { return "Atendidas, canceladas y pacientes que no llegaron."; }
    @Override public String icono() { return "fas fa-user-check"; }
    @Override public String grupo() { return "Citas"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.citasPorEstado(p.desde(), p.hasta());
        BigDecimal total = Calculo.suma(filas);
        BigDecimal atendidas = Calculo.de(filas, "ATENDIDA"), faltaron = Calculo.de(filas, "NO_ASISTIO");
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.DONA, IndicadorDTO.NUMERO)
                .etiquetas(filas.stream().map(f -> Calculo.estadoCita(f.etiqueta())).toList())
                .serie("Citas", Calculo.valores(filas))
                .dato("Citas", Calculo.entero(total), p.texto())
                .dato("Asistencia", Calculo.porcentaje(atendidas, atendidas.add(faltaron)), "de las citas que llegaron a su hora")
                .dato("Cancelación", Calculo.porcentaje(Calculo.de(filas, "CANCELADA"), total), "del total reservado")
                .enlace("/recepcion/citas");
    }
}
