package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-17: caja de un cajero en un día. Antes del cierre muestra lo cobrado
 * hasta el momento; después, lo que quedó registrado en tt_cierre_caja.
 */
public class ResumenCajaDTO {

    private int idUsuarioCajero;
    private String nombreCajero = "";
    private LocalDate fecha;
    private int cantidadPagos;
    private int cantidadRechazados;
    private int cantidadAnulados;
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
    private BigDecimal totalTarjeta = BigDecimal.ZERO;
    private BigDecimal totalBilletera = BigDecimal.ZERO;
    private List<PagoDTO> pagos = new ArrayList<>();
    // Datos del cierre (si ya se cerró)
    private boolean cerrada;
    private BigDecimal efectivoContado;
    private BigDecimal diferencia;
    private String observacion;
    private Timestamp fechaCierre;

    public BigDecimal getTotalGeneral() { return totalEfectivo.add(totalTarjeta).add(totalBilletera); }

    public int getIdUsuarioCajero() { return idUsuarioCajero; }
    public void setIdUsuarioCajero(int v) { idUsuarioCajero = v; }
    public String getNombreCajero() { return nombreCajero; }
    public void setNombreCajero(String v) { nombreCajero = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { fecha = v; }
    public int getCantidadPagos() { return cantidadPagos; }
    public void setCantidadPagos(int v) { cantidadPagos = v; }
    public int getCantidadRechazados() { return cantidadRechazados; }
    public void setCantidadRechazados(int v) { cantidadRechazados = v; }
    public int getCantidadAnulados() { return cantidadAnulados; }
    public void setCantidadAnulados(int v) { cantidadAnulados = v; }
    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(BigDecimal v) { totalEfectivo = v; }
    public BigDecimal getTotalTarjeta() { return totalTarjeta; }
    public void setTotalTarjeta(BigDecimal v) { totalTarjeta = v; }
    public BigDecimal getTotalBilletera() { return totalBilletera; }
    public void setTotalBilletera(BigDecimal v) { totalBilletera = v; }
    public List<PagoDTO> getPagos() { return pagos; }
    public void setPagos(List<PagoDTO> v) { pagos = v; }
    public boolean isCerrada() { return cerrada; }
    public void setCerrada(boolean v) { cerrada = v; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal v) { efectivoContado = v; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal v) { diferencia = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { observacion = v; }
    public Timestamp getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Timestamp v) { fechaCierre = v; }
}
