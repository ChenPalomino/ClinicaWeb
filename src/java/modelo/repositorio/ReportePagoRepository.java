package modelo.repositorio;

import modelo.dto.ReportePagoDTO;

import java.util.List;
import java.util.Optional;

public interface ReportePagoRepository {

    int registrar(ReportePagoDTO r);

    Optional<ReportePagoDTO> buscarPorId(int idReporte);

    Optional<ReportePagoDTO> ultimoDeCuenta(int idCuenta);

    List<ReportePagoDTO> pendientes(int limite);

    int contarPendientes();

    boolean marcarVerificado(int idReporte, int idUsuario, int idPago);

    boolean marcarRechazado(int idReporte, int idUsuario, String motivo);
}
