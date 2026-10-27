package fakes;

import modelo.cobranza.TotalesCuenta;
import modelo.cobranza.comprobante.Comprobante;
import modelo.dto.*;
import modelo.repositorio.*;

import java.time.LocalDate;
import java.util.*;

import static fakes.Datos.bd;

/** Repositorio de pagos en memoria; "fallar" simula un conflicto en la transacción. */
public class PagosFalsos implements PagoRepository {
    public List<PagoDTO> rechazos = new ArrayList<>();
    public List<PagoDTO> aprobados = new ArrayList<>();
    public CuentasFalsas cuentas; public int numero = 0; public RuntimeException fallar;
    public List<OpcionDTO> billeteras = List.of(new OpcionDTO(1, "Yape", null), new OpcionDTO(2, "Plin", null));
    public List<PagoDTO> delCajero = new ArrayList<>();
    public List<OpcionDTO> metodosActivos() { return List.of(); }
    public int registrarRechazo(PagoDTO p) { rechazos.add(p); return rechazos.size(); }
    public int registrarPagoAprobado(PagoDTO p, Comprobante c, List<DetalleCuentaDTO> d, LocalDate hoy) {
        if (fallar != null) throw fallar;
        aprobados.add(p); c.setNumero(++numero); cuentas.cuentas.get(p.getIdCuentaAtencion()).setEstadoCuenta("PAGADA"); return 100 + numero;
    }
    public void anular(int id, int u, String m) {
        if ("ANULADA".equals(cuentas.cuentas.get(id).getEstadoCuenta())) throw new OperacionRechazadaException("La cuenta ya está anulada.");
        cuentas.cuentas.get(id).setEstadoCuenta("ANULADA");
    }
    public Optional<ComprobanteDTO> buscarComprobante(int id) { return Optional.empty(); }
    public List<PagoDTO> pagosDelCajero(int u, LocalDate f) { return delCajero; }
    public List<OpcionDTO> billeterasActivas() { return billeteras; }
}
