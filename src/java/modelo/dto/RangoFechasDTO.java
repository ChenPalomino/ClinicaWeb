package modelo.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Rango de fechas de registro para los listados (usuarios, pacientes, tarifario,
 * proveedores y cobros).
 *
 * RENDIMIENTO: un listado NO trae todo el historial por defecto. Si el usuario
 * no busca nada ni elige fechas, se muestran solo los registros de los últimos
 * 30 días; así la consulta (y su conteo) no crece con los años. Si escribe un
 * DNI o un nombre, se busca en todo el historial: buscar a alguien que se
 * registró hace dos años debe funcionar.
 */
public final class RangoFechasDTO {

    public static final int DIAS_POR_DEFECTO = 30;
    public static final RangoFechasDTO SIN_RANGO = new RangoFechasDTO(null, null, false);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LocalDate desde;
    private final LocalDate hasta;
    private final boolean porDefecto;

    private RangoFechasDTO(LocalDate desde, LocalDate hasta, boolean porDefecto) {
        this.desde = desde;
        this.hasta = hasta;
        this.porDefecto = porDefecto;
    }

    /**
     * @param desde       fecha elegida o null
     * @param hasta       fecha elegida o null
     * @param hayBusqueda true si el usuario escribió un texto a buscar
     * @param hoy         fecha actual (se recibe para poder probarlo)
     */
    public static RangoFechasDTO de(LocalDate desde, LocalDate hasta, boolean hayBusqueda, LocalDate hoy) {
        if (desde == null && hasta == null) {
            return hayBusqueda ? SIN_RANGO : new RangoFechasDTO(hoy.minusDays(DIAS_POR_DEFECTO - 1L), hoy, true);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            return new RangoFechasDTO(hasta, desde, false);       // fechas al revés: se corrigen solas
        }
        return new RangoFechasDTO(desde, hasta, false);
    }

    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }

    /** El día siguiente a "hasta": la consulta usa fecha < hastaExclusivo para incluir todo ese día. */
    public LocalDate getHastaExclusivo() { return hasta == null ? null : hasta.plusDays(1); }

    /** true = rango automático de los últimos 30 días (el usuario no eligió fechas). */
    public boolean isPorDefecto() { return porDefecto; }

    /** true = hay alguna fecha que limita la consulta. */
    public boolean isActivo() { return desde != null || hasta != null; }

    /** true = el usuario eligió fechas (cuenta como filtro aplicado). */
    public boolean isElegido() { return isActivo() && !porDefecto; }

    /** Valor para el input type="date" (yyyy-MM-dd) o "". */
    public String desdeIso() { return desde == null ? "" : desde.toString(); }
    public String hastaIso() { return hasta == null ? "" : hasta.toString(); }

    /** Parámetros para conservar el rango elegido al paginar o volver al listado. */
    public String parametrosUrl() {
        if (!isElegido()) {
            return "";
        }
        return (desde == null ? "" : "&desde=" + desde) + (hasta == null ? "" : "&hasta=" + hasta);
    }

    /** "del 20/09/2026 al 19/10/2026", "desde el 01/01/2026", "hasta el 31/12/2025". */
    public String getDescripcion() {
        if (desde != null && hasta != null) {
            return "del " + desde.format(DMY) + " al " + hasta.format(DMY);
        }
        if (desde != null) {
            return "desde el " + desde.format(DMY);
        }
        return hasta == null ? "" : "hasta el " + hasta.format(DMY);
    }
}
