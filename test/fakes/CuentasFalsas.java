package fakes;

import modelo.cobranza.TotalesCuenta;
import modelo.cobranza.comprobante.Comprobante;
import modelo.dto.*;
import modelo.repositorio.*;

import java.time.LocalDate;
import java.util.*;

import static fakes.Datos.bd;

/** Repositorio de cuentas en memoria: cada lectura devuelve una copia, como la base de datos. */
public class CuentasFalsas implements CuentaAtencionRepository {
    public Map<Integer, CuentaAtencionDTO> cuentas = new HashMap<>();
    public List<ProductoServicioDTO> items = new ArrayList<>();
    public int sec = 0;
    public PaginaDTO<CuentaAtencionDTO> listar(FiltroBusquedaDTO f) { return null; }
    public Optional<CuentaAtencionDTO> buscarPorId(int id) {
        CuentaAtencionDTO c = cuentas.get(id);
        if (c == null) return Optional.empty();
        CuentaAtencionDTO copia = new CuentaAtencionDTO();
        copia.setIdCuentaAtencion(c.getIdCuentaAtencion()); copia.setNumeroCuenta(c.getNumeroCuenta());
        copia.setEstadoCuenta(c.getEstadoCuenta()); copia.setTipoCobertura(c.getTipoCobertura());
        copia.setPorcentajeCobertura(c.getPorcentajeCobertura()); copia.aplicarTotales(c.getTotales());
        List<DetalleCuentaDTO> ds = new ArrayList<>();
        for (DetalleCuentaDTO d : c.getDetalles()) { DetalleCuentaDTO x = new DetalleCuentaDTO();
            x.setIdProductoServicio(d.getIdProductoServicio()); x.setDescripcion(d.getDescripcion()); x.setTipo(d.getTipo());
            x.setCantidad(d.getCantidad()); x.setPrecioUnitario(d.getPrecioUnitario()); x.setAfectoIgv(d.isAfectoIgv());
            x.setImporte(d.getImporte()); x.setMontoAseguradora(d.getMontoAseguradora()); x.setMontoDescuento(d.getMontoDescuento()); ds.add(x); }
        copia.setDetalles(ds);
        return Optional.of(copia);
    }
    public int crear(int idPaciente, int idCobertura, String obs, String u) {
        CuentaAtencionDTO c = new CuentaAtencionDTO(); c.setIdCuentaAtencion(++sec); c.setNumeroCuenta("CA-00000" + sec);
        c.setIdPaciente(idPaciente); c.setIdCobertura(idCobertura);
        CoberturaDTO cb = coberturaDelPaciente(idPaciente).get(); c.setTipoCobertura(cb.getTipo()); c.setPorcentajeCobertura(cb.getPorcentaje());
        cuentas.put(sec, c); return sec;
    }
    public Optional<Integer> pendienteDelPaciente(int idPaciente) {
        return cuentas.values().stream().filter(c -> c.getIdPaciente() == idPaciente && c.isPendiente()).map(CuentaAtencionDTO::getIdCuentaAtencion).findFirst();
    }
    public boolean guardarDetalles(int id, List<DetalleCuentaDTO> d, TotalesCuenta t, String u) {
        CuentaAtencionDTO c = cuentas.get(id); if (!c.isPendiente()) return false;
        c.setDetalles(new ArrayList<>(d)); c.aplicarTotales(t); return true;
    }
    public Optional<CoberturaDTO> coberturaDelPaciente(int id) {
        switch (id) { case 1: return Optional.of(new CoberturaDTO(1, "Particular", "PARTICULAR", bd("0")));
                      case 2: return Optional.of(new CoberturaDTO(2, "EPS", "EPS", bd("70")));
                      case 3: return Optional.of(new CoberturaDTO(3, "Seguro", "SEGURO", bd("80")));
                      case 4: return Optional.of(new CoberturaDTO(4, "Convenio", "CONVENIO", bd("40")));
                      case 7: return Optional.of(new CoberturaDTO(1, "Particular", "PARTICULAR", bd("0")));
                      default: return Optional.empty(); }
    }
    public List<ProductoServicioDTO> itemsVendibles() { return items; }
    public List<CuentaAtencionDTO> listarDelUsuarioPaciente(int u) { return List.of(); }
    public int contarPendientes() { return 0; }
}
