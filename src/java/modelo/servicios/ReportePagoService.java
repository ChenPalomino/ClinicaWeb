package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;
import modelo.cobranza.pago.PagoBilleteraStrategy;
import modelo.cobranza.pago.SolicitudPago;
import modelo.dto.CitaDTO;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DatosClienteDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.ReportePagoDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.ReportePagoRepository;
import modelo.repositorio.ReportePagoRepositoryImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ReportePagoService {

    public static final int LIMITE_PENDIENTES = 50;

    private final ReportePagoRepository reportes;
    private final Supplier<CobranzaService> cobranza;
    private final Supplier<CitaService> citas;

    public ReportePagoService() {
        this(new ReportePagoRepositoryImpl(), CobranzaService::new, CitaService::new);
    }

    public ReportePagoService(ReportePagoRepository reportes, Supplier<CobranzaService> cobranza, Supplier<CitaService> citas) {
        this.reportes = reportes;
        this.cobranza = cobranza;
        this.citas = citas;
    }

    public record Preparacion(CitaDTO cita, CuentaAtencionDTO cuenta, ReportePagoDTO ultimoReporte, String error) {
        public boolean isExito() { return error == null; }
    }

    public List<ReportePagoDTO> pendientes() { return reportes.pendientes(LIMITE_PENDIENTES); }
    public int contarPendientes() { return reportes.contarPendientes(); }
    public Optional<ReportePagoDTO> ultimoDeCuenta(int idCuenta) { return reportes.ultimoDeCuenta(idCuenta); }

    public Preparacion preparar(int idCita, Usuario paciente) {
        CitaService cs = citas.get();
        Optional<CitaDTO> cita = cs.obtener(idCita);
        if (cita.isPresent() && cita.get().isCuentaPagada()) {
            return new Preparacion(cita.get(), null, null, "Esta cita ya está pagada.");
        }
        CitaService.Cobro cobro = cs.prepararPagoEnLinea(idCita, paciente);
        if (!cobro.isExito()) {
            return new Preparacion(cita.orElse(null), null, null, cobro.error());
        }
        Optional<CuentaAtencionDTO> cuenta = cobranza.get().obtener(cobro.idCuenta());
        if (cuenta.isEmpty() || cuenta.get().getDetalles().isEmpty() || cuenta.get().getTotal().signum() <= 0) {
            return new Preparacion(cita.orElse(null), null, null,
                    "Aún no podemos calcular el monto de esta cita. Págala en caja el día de tu consulta.");
        }
        if (!cuenta.get().isPendiente()) {
            return new Preparacion(cita.orElse(null), cuenta.get(), null, "Esta cita ya está pagada.");
        }
        CitaDTO actualizada = cs.obtener(idCita).orElse(cita.orElse(null));
        return new Preparacion(actualizada, cuenta.get(), reportes.ultimoDeCuenta(cuenta.get().getIdCuentaAtencion()).orElse(null), null);
    }

    public Map<String, String> informar(int idCita, SolicitudPago datos, Usuario paciente) {
        Preparacion p = preparar(idCita, paciente);
        Map<String, String> errores = new LinkedHashMap<>();
        if (!p.isExito()) {
            errores.put("general", p.error());
            return errores;
        }
        if (p.ultimoReporte() != null && p.ultimoReporte().isPendiente()) {
            errores.put("general", "Ya nos informaste este pago. La caja lo está revisando.");
            return errores;
        }
        List<OpcionDTO> billeteras = cobranza.get().billeteras();
        datos.setNombreBilletera(datos.getIdBilletera() == null ? "" : billeteras.stream()
                .filter(o -> o.getId() == datos.getIdBilletera()).map(OpcionDTO::getNombre).findFirst().orElse(""));
        errores.putAll(new PagoBilleteraStrategy().validar(datos));
        if (!errores.isEmpty()) {
            return errores;
        }
        ReportePagoDTO r = new ReportePagoDTO();
        r.setIdCuentaAtencion(p.cuenta().getIdCuentaAtencion());
        r.setIdBilletera(datos.getIdBilletera());
        r.setMonto(p.cuenta().getTotal());
        r.setNumeroOperacion(datos.getNumeroOperacion());
        r.setCelular(datos.getCelular());
        r.setNombrePagador(datos.getNombrePagador());
        r.setUsuarioRegistro(paciente.getNombreUsuario());
        int id = reportes.registrar(r);
        EventoSubject.getInstancia().publicar(TipoEvento.PAGO_REPORTADO, paciente, p.cuenta().getIdCuentaAtencion(),
                p.cuenta().getNumeroCuenta() + " · " + datos.getNombreBilletera() + " · S/ " + p.cuenta().getTotal() + " · aviso #" + id);
        return errores;
    }

    public CobranzaService.ResultadoCobro verificar(int idReporte, Usuario cajero) {
        Optional<ReportePagoDTO> r = reportes.buscarPorId(idReporte);
        if (r.isEmpty() || !r.get().isPendiente()) {
            return CobranzaService.ResultadoCobro.fallido("Este pago ya fue revisado. Actualiza la lista.");
        }
        ReportePagoDTO rep = r.get();
        if (!CuentaAtencionDTO.PENDIENTE.equals(rep.getEstadoCuenta())) {
            return CobranzaService.ResultadoCobro.fallido("La cuenta ya no está por cobrar. Rechaza este aviso con el motivo.");
        }
        if (!rep.isMontoCoincide()) {
            return CobranzaService.ResultadoCobro.fallido("El paciente informó S/ " + rep.getMonto() + " pero la cuenta suma S/ "
                    + rep.getTotalCuenta() + ". Cobra la diferencia en caja o rechaza el aviso.");
        }
        SolicitudPago s = new SolicitudPago();
        s.setIdBilletera(rep.getIdBilletera());
        s.setNumeroOperacion(rep.getNumeroOperacion());
        s.setCelular(rep.getCelular());
        s.setNombrePagador(rep.getNombrePagador());
        s.setIdReportePago(rep.getIdReportePago());
        DatosClienteDTO cliente = new DatosClienteDTO();
        cliente.setTipoComprobante("BOLETA");
        cliente.setNumeroDocumento(rep.getDniPaciente().matches("\\d{8}") ? rep.getDniPaciente() : "");
        cliente.setNombre(rep.getNombrePaciente());
        cliente.setDireccion("");
        CobranzaService.ResultadoCobro resultado = cobranza.get().cobrar(rep.getIdCuentaAtencion(), "BILLETERA", s, cliente, cajero);
        if (resultado.isExito()) {
            EventoSubject.getInstancia().publicar(TipoEvento.PAGO_VERIFICADO, cajero, rep.getIdCuentaAtencion(),
                    rep.getNumeroCuenta() + " · " + rep.getReferencia() + " · " + resultado.comprobante());
        }
        return resultado;
    }

    public String rechazar(int idReporte, String motivo, Usuario cajero) {
        String m = motivo == null ? "" : motivo.trim();
        if (m.length() < 5 || m.length() > 200) {
            return "Explica al paciente por qué no se aceptó el pago (de 5 a 200 caracteres).";
        }
        Optional<ReportePagoDTO> r = reportes.buscarPorId(idReporte);
        if (r.isEmpty() || !reportes.marcarRechazado(idReporte, cajero.getIdUsuario(), m)) {
            return "Este pago ya fue revisado. Actualiza la lista.";
        }
        EventoSubject.getInstancia().publicar(TipoEvento.PAGO_REPORTE_RECHAZADO, cajero, r.get().getIdCuentaAtencion(),
                r.get().getNumeroCuenta() + " · " + r.get().getReferencia() + " · Motivo: " + m);
        return null;
    }
}
