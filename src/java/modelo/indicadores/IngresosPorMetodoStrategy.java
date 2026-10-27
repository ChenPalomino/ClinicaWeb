package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: ingresos por medio de pago. */
public class IngresosPorMetodoStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "INGRESOS_METODO"; }
    @Override public String titulo() { return "Ingresos por medio de pago"; }
    @Override public String descripcion() { return "Efectivo, tarjeta o billetera digital: qué prefieren los pacientes."; }
    @Override public String icono() { return "fas fa-wallet"; }
    @Override public String grupo() { return "Caja"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.ingresosPorMetodo(p.desde(), p.hasta());
        BigDecimal total = Calculo.suma(filas);
        IndicadorDTO r = new IndicadorDTO(codigo(), titulo(), IndicadorDTO.DONA, IndicadorDTO.SOLES)
                .etiquetas(Calculo.etiquetas(filas)).serie("Cobrado", Calculo.valores(filas))
                .dato("Total", Calculo.soles(total), p.texto());
        filas.forEach(f -> r.dato(f.etiqueta(), Calculo.porcentaje(f.valor(), total), Calculo.soles(f.valor()) + " · " + Calculo.entero(f.valor2()) + " pagos"));
        return r;
    }
}
