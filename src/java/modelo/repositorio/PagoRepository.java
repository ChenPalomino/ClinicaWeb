package modelo.repositorio;

import modelo.cobranza.comprobante.Comprobante;
import modelo.dto.ComprobanteDTO;
import modelo.dto.DetalleCuentaDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PagoDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** PATRÓN: Repository  ->  CONTRATO de pagos, comprobantes y anulaciones (RF-13 a RF-16). */
public interface PagoRepository {

    /** Métodos de pago activos: id, nombre y código (en "extra"). */
    List<OpcionDTO> metodosActivos();

    /** RF-14: guarda un intento rechazado (la cuenta sigue pendiente y se puede reintentar). */
    int registrarRechazo(PagoDTO pago);

    /**
     * RNF-08: en UNA transacción: bloquea la cuenta, comprueba que siga pendiente y
     * con el mismo total, que la caja del cajero no esté cerrada y que haya stock;
     * descuenta el stock, registra el pago, numera y guarda el comprobante y
     * marca la cuenta como PAGADA. Si algo falla no se guarda nada.
     * @return id del comprobante (y le asigna su número correlativo)
     * @throws OperacionRechazadaException con un mensaje para el cajero
     */
    int registrarPagoAprobado(PagoDTO pago, Comprobante comprobante, List<DetalleCuentaDTO> detalles, LocalDate hoy);

    /**
     * RF-16: anula la cuenta. Si estaba pagada también anula el pago y el
     * comprobante y devuelve el stock, todo en una transacción.
     * @throws OperacionRechazadaException si ya estaba anulada o la caja de ese día ya se cerró
     */
    void anular(int idCuenta, int idUsuarioAnula, String motivo);

    Optional<ComprobanteDTO> buscarComprobante(int idComprobante);

    /** Todos los intentos de pago de un cajero en un día (para el cierre de caja). */
    List<PagoDTO> pagosDelCajero(int idUsuarioCajero, LocalDate fecha);
}
