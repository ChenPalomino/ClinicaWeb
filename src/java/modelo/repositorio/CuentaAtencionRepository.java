package modelo.repositorio;

import modelo.cobranza.TotalesCuenta;
import modelo.dto.CoberturaDTO;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DetalleCuentaDTO;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProductoServicioDTO;

import java.util.List;
import java.util.Optional;

/**
 * PATRÓN: Repository  ->  CONTRATO de las cuentas de atención (RF-11).
 * El pago, el comprobante y la anulación están en PagoRepository.
 */
public interface CuentaAtencionRepository {

    /** filtro.texto: paciente, DNI, N.º de cuenta o de comprobante. filtro.estado: PENDIENTE | PAGADA | ANULADA. */
    PaginaDTO<CuentaAtencionDTO> listar(FiltroBusquedaDTO filtro);

    /** Cabecera + detalle + pago + intentos rechazados + comprobante. */
    Optional<CuentaAtencionDTO> buscarPorId(int idCuenta);

    /** Crea la cabecera vacía (PENDIENTE) y devuelve su id. */
    int crear(int idPaciente, int idCobertura, String observacion, String usuario);

    /** Cuenta PENDIENTE que el paciente ya tenga abierta (para no duplicar). */
    Optional<Integer> pendienteDelPaciente(int idPaciente);

    /**
     * Reemplaza TODAS las líneas de una cuenta pendiente y sus totales, en una transacción.
     * @return false si la cuenta ya no está pendiente (otro cajero la cobró o anuló)
     */
    boolean guardarDetalles(int idCuenta, List<DetalleCuentaDTO> detalles, TotalesCuenta totales, String usuario);

    /** Cobertura vigente del paciente (activo). Vacío si el paciente no existe o está desactivado. */
    Optional<CoberturaDTO> coberturaDelPaciente(int idPaciente);

    /** Servicios y productos activos que se pueden agregar a una cuenta. */
    List<ProductoServicioDTO> itemsVendibles();

    /** Cuentas pagadas (o anuladas después de pagarse) del paciente con esta cuenta de usuario (portal). */
    List<CuentaAtencionDTO> listarDelUsuarioPaciente(int idUsuario);

    int contarPendientes();
}
