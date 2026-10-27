package modelo.repositorio;

import modelo.dto.ParValorDTO;

import java.time.LocalDate;
import java.util.List;

/** PATRÓN: Repository -> consultas agregadas para los indicadores del administrador. */
public interface IndicadorRepository {

    List<ParValorDTO> ingresosPorDia(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> ingresosPorMetodo(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> ingresosPorCobertura(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> masVendidos(LocalDate desde, LocalDate hasta, int limite);
    List<ParValorDTO> citasPorEstado(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> citasPorEspecialidad(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> atencionesPorMedico(LocalDate desde, LocalDate hasta, int limite);
    List<ParValorDTO> citasPorHora(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> citasPorCanal(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> usuariosNuevosPorDia(LocalDate desde, LocalDate hasta);
    List<ParValorDTO> usuariosActivosPorRol();
    List<ParValorDTO> stockBajo(int limite);
}
