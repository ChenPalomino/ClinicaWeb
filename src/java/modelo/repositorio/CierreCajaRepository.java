package modelo.repositorio;

import modelo.dto.ResumenCajaDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** PATRÓN: Repository  ->  CONTRATO del cierre de caja diario (RF-17). */
public interface CierreCajaRepository {

    /** Cierre ya registrado de ese cajero en esa fecha (solo cabecera, sin la lista de pagos). */
    Optional<ResumenCajaDTO> buscar(int idUsuarioCajero, LocalDate fecha);

    /** @throws OperacionRechazadaException si ese cajero ya cerró la caja de esa fecha */
    void registrar(ResumenCajaDTO cierre, String usuario);

    /** Últimos cierres (de un cajero, o de todos si idUsuarioCajero es null). */
    List<ResumenCajaDTO> historial(Integer idUsuarioCajero, int limite);
}
