package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;
import modelo.dto.PagoDTO;
import modelo.dto.ResumenCajaDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.repositorio.CierreCajaRepository;
import modelo.repositorio.CierreCajaRepositoryImpl;
import modelo.repositorio.OperacionRechazadaException;
import modelo.repositorio.PagoRepository;
import modelo.repositorio.PagoRepositoryImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * RF-17: cierre de caja diario. Cada cajero cierra SU caja una vez al día:
 * el sistema suma lo cobrado por medio de pago, el cajero declara el efectivo
 * que contó y queda registrada la diferencia (sobrante o faltante).
 * Después del cierre ese cajero ya no puede cobrar ese día (lo valida PagoRepository).
 */
public class CierreCajaService {

    private static final BigDecimal MAXIMO_CONTADO = new BigDecimal("100000");

    private final PagoRepository pagos;
    private final CierreCajaRepository cierres;
    private final Supplier<LocalDate> hoy;

    public CierreCajaService() {
        this(new PagoRepositoryImpl(), new CierreCajaRepositoryImpl(), LocalDate::now);
    }

    public CierreCajaService(PagoRepository pagos, CierreCajaRepository cierres, Supplier<LocalDate> hoy) {
        this.pagos = pagos;
        this.cierres = cierres;
        this.hoy = hoy;
    }

    /** Caja de hoy del usuario: lo cobrado hasta ahora y, si ya cerró, los datos del cierre. */
    public ResumenCajaDTO resumenDeHoy(Usuario cajero) {
        return resumen(cajero.getIdUsuario(), cajero.getNombreUsuario(), hoy.get());
    }

    public ResumenCajaDTO resumen(int idUsuario, String nombre, LocalDate fecha) {
        ResumenCajaDTO r = new ResumenCajaDTO();
        r.setIdUsuarioCajero(idUsuario);
        r.setNombreCajero(nombre);
        r.setFecha(fecha);
        List<PagoDTO> lista = pagos.pagosDelCajero(idUsuario, fecha);
        r.setPagos(lista);
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        BigDecimal billetera = BigDecimal.ZERO;
        int aprobados = 0;
        int rechazados = 0;
        int anulados = 0;
        for (PagoDTO p : lista) {
            if (p.isRechazado()) {
                rechazados++;
            } else if (!p.isAprobado()) {
                anulados++;
            } else {
                aprobados++;
                switch (p.getCodigoMetodo()) {
                    case "EFECTIVO": efectivo = efectivo.add(p.getMonto()); break;
                    case "TARJETA": tarjeta = tarjeta.add(p.getMonto()); break;
                    default: billetera = billetera.add(p.getMonto());
                }
            }
        }
        r.setCantidadPagos(aprobados);
        r.setCantidadRechazados(rechazados);
        r.setCantidadAnulados(anulados);
        r.setTotalEfectivo(efectivo.setScale(2, RoundingMode.HALF_UP));
        r.setTotalTarjeta(tarjeta.setScale(2, RoundingMode.HALF_UP));
        r.setTotalBilletera(billetera.setScale(2, RoundingMode.HALF_UP));
        Optional<ResumenCajaDTO> cierre = cierres.buscar(idUsuario, fecha);
        if (cierre.isPresent()) {
            r.setCerrada(true);
            r.setEfectivoContado(cierre.get().getEfectivoContado());
            r.setDiferencia(cierre.get().getDiferencia());
            r.setObservacion(cierre.get().getObservacion());
            r.setFechaCierre(cierre.get().getFechaCierre());
        }
        return r;
    }

    /** @return null si se cerró; si no, el motivo */
    public String cerrar(Usuario cajero, BigDecimal efectivoContado, String observacion) {
        String obs = observacion == null ? "" : observacion.trim();
        if (efectivoContado == null || efectivoContado.signum() < 0 || efectivoContado.stripTrailingZeros().scale() > 2) {
            return "Escribe cuánto efectivo contaste en la caja (puede ser 0.00).";
        }
        if (efectivoContado.compareTo(MAXIMO_CONTADO) > 0) {
            return "Revisa el efectivo contado: no puede pasar de S/ 100,000.00.";
        }
        if (obs.length() > 250) {
            return "La observación no puede pasar de 250 caracteres.";
        }
        ResumenCajaDTO r = resumenDeHoy(cajero);
        if (r.isCerrada()) {
            return "Tu caja de hoy ya estaba cerrada.";
        }
        BigDecimal diferencia = efectivoContado.setScale(2, RoundingMode.HALF_UP).subtract(r.getTotalEfectivo());
        if (diferencia.signum() != 0 && obs.length() < 5) {
            return "Hay una diferencia de S/ " + diferencia.toPlainString() + " entre lo contado y lo cobrado en efectivo. "
                    + "Explica el motivo en la observación.";
        }
        r.setEfectivoContado(efectivoContado.setScale(2, RoundingMode.HALF_UP));
        r.setDiferencia(diferencia);
        r.setObservacion(obs);
        try {
            cierres.registrar(r, cajero.getNombreUsuario());
        } catch (OperacionRechazadaException e) {
            return e.getMessage();
        }
        EventoSubject.getInstancia().publicar(TipoEvento.CAJA_CERRADA, cajero, cajero.getIdUsuario(),
                r.getFecha() + " · " + r.getCantidadPagos() + " cobros · S/ " + r.getTotalGeneral()
                        + (diferencia.signum() == 0 ? " · sin diferencias" : " · diferencia S/ " + diferencia));
        return null;
    }

    /** Últimos cierres: el administrador ve los de todos; el cajero, los suyos. */
    public List<ResumenCajaDTO> historial(Usuario u) {
        return cierres.historial(u.getRolId() == Rol.ADMINISTRADOR ? null : u.getIdUsuario(), 30);
    }
}
