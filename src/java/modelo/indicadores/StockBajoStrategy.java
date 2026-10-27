package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: productos por reponer. */
public class StockBajoStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "STOCK_BAJO"; }
    @Override public String titulo() { return "Productos por reponer"; }
    @Override public String descripcion() { return "Productos con stock igual o menor al mínimo (estado actual)."; }
    @Override public String icono() { return "fas fa-boxes"; }
    @Override public String grupo() { return "Inventario"; }

    @Override public boolean usaPeriodo() { return false; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.stockBajo(15);
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS_HORIZONTALES, IndicadorDTO.NUMERO)
                .etiquetas(Calculo.etiquetas(filas))
                .serie("Stock", Calculo.valores(filas))
                .serie("Mínimo", Calculo.valores2(filas))
                .dato("Por reponer", Calculo.entero(BigDecimal.valueOf(filas.size())), filas.size() >= 15 ? "se muestran los 15 más urgentes" : "productos")
                .nota(filas.isEmpty() ? "Todo el inventario está sobre su mínimo." : "")
                .enlace("/admin/servicios?tipo=PRODUCTO&estado=REPONER");
    }
}
