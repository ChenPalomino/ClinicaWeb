package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: ingresos por cobertura. */
public class IngresosPorCoberturaStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "INGRESOS_COBERTURA"; }
    @Override public String titulo() { return "Ingresos por cobertura"; }
    @Override public String descripcion() { return "Lo que pagó el paciente frente a lo que cubrió su seguro o convenio."; }
    @Override public String icono() { return "fas fa-shield-alt"; }
    @Override public String grupo() { return "Caja"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.ingresosPorCobertura(p.desde(), p.hasta());
        BigDecimal paciente = Calculo.suma(filas), cubierto = Calculo.suma2(filas);
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS, IndicadorDTO.SOLES)
                .etiquetas(Calculo.etiquetas(filas))
                .serie("Pagó el paciente", Calculo.valores(filas))
                .serie("Cubrió seguro o convenio", Calculo.valores2(filas))
                .dato("Pagado por pacientes", Calculo.soles(paciente), p.texto())
                .dato("Cubierto por terceros", Calculo.soles(cubierto), "por cobrar a aseguradoras y convenios")
                .dato("Particulares", Calculo.porcentaje(Calculo.de(filas, "Particular"), paciente), "de lo cobrado en caja");
    }
}
