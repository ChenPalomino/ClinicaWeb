package modelo.indicadores;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** PATRÓN: Factory -> catálogo de estrategias de indicadores; agregar uno = una clase + una línea aquí. */
public final class IndicadorFactory {

    private static final Map<String, IndicadorStrategy> CATALOGO = new LinkedHashMap<>();

    static {
        Stream.of(
                new IngresosDiariosStrategy(),
                new IngresosPorMetodoStrategy(),
                new IngresosPorCoberturaStrategy(),
                new MasVendidosStrategy(),
                new CitasPorEstadoStrategy(),
                new CitasPorEspecialidadStrategy(),
                new AtencionesPorMedicoStrategy(),
                new HorasPicoStrategy(),
                new CanalReservaStrategy(),
                new UsuariosNuevosStrategy(),
                new UsuariosPorRolStrategy(),
                new StockBajoStrategy()
        ).forEach(i -> CATALOGO.put(i.codigo(), i));
    }

    private IndicadorFactory() { }

    public static List<IndicadorStrategy> todos() { return List.copyOf(CATALOGO.values()); }

    public static Optional<IndicadorStrategy> obtener(String codigo) {
        return Optional.ofNullable(codigo == null ? null : CATALOGO.get(codigo));
    }
}
