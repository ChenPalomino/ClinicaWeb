package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: reservas por canal. */
public class CanalReservaStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "CANAL_RESERVA"; }
    @Override public String titulo() { return "Reservas por canal"; }
    @Override public String descripcion() { return "Cuántas citas reservan los pacientes solos desde el portal."; }
    @Override public String icono() { return "fas fa-globe"; }
    @Override public String grupo() { return "Citas"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.citasPorCanal(p.desde(), p.hasta());
        BigDecimal total = Calculo.suma(filas);
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.DONA, IndicadorDTO.NUMERO)
                .etiquetas(filas.stream().map(f -> "PORTAL".equals(f.etiqueta()) ? "Portal del paciente" : "Recepción").toList())
                .serie("Citas", Calculo.valores(filas))
                .dato("Citas", Calculo.entero(total), p.texto())
                .dato("Desde el portal", Calculo.porcentaje(Calculo.de(filas, "PORTAL"), total), "reservadas por el propio paciente");
    }
}
