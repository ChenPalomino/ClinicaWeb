package modelo.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.repositorio.IndicadorRepository;

import java.util.List;

/** PATRÓN: Strategy -> ESTRATEGIA CONCRETA: usuarios por rol. */
public class UsuariosPorRolStrategy implements IndicadorStrategy {

    @Override public String codigo() { return "USUARIOS_ROL"; }
    @Override public String titulo() { return "Usuarios por rol"; }
    @Override public String descripcion() { return "Cuentas activas y bloqueadas de cada rol (estado actual)."; }
    @Override public String icono() { return "fas fa-users-cog"; }
    @Override public String grupo() { return "Usuarios"; }

    @Override public boolean usaPeriodo() { return false; }

    @Override
    public IndicadorDTO calcular(Periodo p, IndicadorRepository datos) {
        List<ParValorDTO> filas = datos.usuariosActivosPorRol();
        return new IndicadorDTO(codigo(), titulo(), IndicadorDTO.BARRAS, IndicadorDTO.NUMERO)
                .etiquetas(Calculo.etiquetas(filas))
                .serie("Activos", Calculo.valores(filas))
                .serie("Bloqueados", Calculo.valores2(filas))
                .dato("Activos", Calculo.entero(Calculo.suma(filas)), "hoy")
                .dato("Bloqueados", Calculo.entero(Calculo.suma2(filas)), "por intentos fallidos o por el administrador")
                .enlace("/admin/usuarios?estado=BLOQUEADO");
    }
}
