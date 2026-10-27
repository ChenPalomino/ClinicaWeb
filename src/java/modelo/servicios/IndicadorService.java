package modelo.servicios;

import modelo.dto.IndicadorDTO;
import modelo.indicadores.IndicadorFactory;
import modelo.indicadores.IndicadorStrategy;
import modelo.indicadores.Periodo;
import modelo.repositorio.IndicadorRepository;
import modelo.repositorio.IndicadorRepositoryImpl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public class IndicadorService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final IndicadorRepository datos;

    public IndicadorService() {
        this(new IndicadorRepositoryImpl());
    }

    public IndicadorService(IndicadorRepository datos) {
        this.datos = datos;
    }

    public List<IndicadorStrategy> catalogo() { return IndicadorFactory.todos(); }

    public Optional<IndicadorDTO> calcular(String codigo, String clavePeriodo) {
        return calcular(codigo, clavePeriodo, LocalDate.now(LIMA));
    }

    public Optional<IndicadorDTO> calcular(String codigo, String clavePeriodo, LocalDate hoy) {
        Periodo periodo = Periodo.de(clavePeriodo, hoy);
        return IndicadorFactory.obtener(codigo).map(i -> i.calcular(periodo, datos));
    }

    public static String textoPeriodo(String codigo, String clavePeriodo, LocalDate hoy) {
        boolean usa = IndicadorFactory.obtener(codigo).map(IndicadorStrategy::usaPeriodo).orElse(true);
        return usa ? Periodo.de(clavePeriodo, hoy).texto() : "Estado actual";
    }
}
