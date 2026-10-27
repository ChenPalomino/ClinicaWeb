package fakes;

import modelo.dto.ReportePagoDTO;
import modelo.repositorio.ReportePagoRepository;

import java.util.*;
import java.util.stream.Collectors;

public class ReportesFalsos implements ReportePagoRepository {
    public Map<Integer, ReportePagoDTO> reportes = new LinkedHashMap<>();
    int sec = 0;
    public CuentasFalsas cuentas;

    public int registrar(ReportePagoDTO r) {
        r.setIdReportePago(++sec);
        r.setEstadoReporte(ReportePagoDTO.PENDIENTE);
        reportes.put(sec, r);
        return sec;
    }
    public Optional<ReportePagoDTO> buscarPorId(int id) {
        ReportePagoDTO r = reportes.get(id);
        if (r != null && cuentas != null && cuentas.cuentas.containsKey(r.getIdCuentaAtencion())) {
            modelo.dto.CuentaAtencionDTO c = cuentas.cuentas.get(r.getIdCuentaAtencion());
            r.setEstadoCuenta(c.getEstadoCuenta());
            r.setTotalCuenta(c.getTotal());
            r.setNumeroCuenta(c.getNumeroCuenta());
        }
        return Optional.ofNullable(r);
    }
    public Optional<ReportePagoDTO> ultimoDeCuenta(int idCuenta) {
        return reportes.values().stream().filter(r -> r.getIdCuentaAtencion() == idCuenta).reduce((a, b) -> b);
    }
    public List<ReportePagoDTO> pendientes(int limite) {
        return reportes.values().stream().filter(ReportePagoDTO::isPendiente).limit(limite).collect(Collectors.toList());
    }
    public int contarPendientes() { return pendientes(1000).size(); }
    public boolean marcarVerificado(int id, int u, int idPago) {
        ReportePagoDTO r = reportes.get(id);
        if (r == null || !r.isPendiente()) return false;
        r.setEstadoReporte(ReportePagoDTO.VERIFICADO); r.setIdPago(idPago); return true;
    }
    public boolean marcarRechazado(int id, int u, String motivo) {
        ReportePagoDTO r = reportes.get(id);
        if (r == null || !r.isPendiente()) return false;
        r.setEstadoReporte(ReportePagoDTO.RECHAZADO); r.setMotivoRechazo(motivo); return true;
    }
}
