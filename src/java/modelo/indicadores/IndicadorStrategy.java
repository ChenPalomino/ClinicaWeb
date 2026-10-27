package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.repositorio.IndicadorRepository;

/** PATRÓN: Strategy -> cada indicador del inicio del administrador sabe cómo calcularse y dibujarse. */
public interface IndicadorStrategy {

    String codigo();
    String titulo();
    String descripcion();
    String icono();
    String grupo();

    default boolean usaPeriodo() { return true; }

    IndicadorDTO calcular(Periodo periodo, IndicadorRepository datos);
}
