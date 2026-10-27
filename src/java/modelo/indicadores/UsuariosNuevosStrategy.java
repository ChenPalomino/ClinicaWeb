package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: usuarios nuevos. */
public class UsuariosNuevosStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "USUARIOS_NUEVOS"; }
    @Override public String titulo() { return "Usuarios nuevos"; }
    @Override public String descripcion() { return "Cuentas creadas por día (pacientes que se registran y personal)."; }
    @Override public String icono() { return "fas fa-user-plus"; }
    @Override public String grupo() { return "Usuarios"; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.usuariosNuevosPorDia(p.desde(), p.hasta());
        Map<String, BigDecimal[]> serie = Calculo.porFecha(p, filas);
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS, IndicadorDTO.NUMERO)
                .etiquetas(List.copyOf(serie.keySet()))
                .serie("Usuarios nuevos", Calculo.columna(serie, 0))
                .dato("Nuevos", Calculo.entero(Calculo.suma(filas)), p.texto())
                .enlace("/admin/usuarios");
    }
}
