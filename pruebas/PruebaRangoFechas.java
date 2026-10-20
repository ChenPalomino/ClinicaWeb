import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.RangoFechasDTO;

import java.time.LocalDate;

/** Pruebas SIN base de datos del rango de fechas de los listados. */
public class PruebaRangoFechas {
    static int fallas = 0;
    static void check(String n, boolean ok) { System.out.println((ok ? "OK   " : "FALLA ") + n); if (!ok) fallas++; }

    public static void main(String[] a) {
        LocalDate hoy = LocalDate.of(2026, 10, 19);
        RangoFechasDTO r = RangoFechasDTO.de(null, null, false, hoy);
        check("sin búsqueda ni fechas: últimos 30 días (20/09 a 19/10)", r.isPorDefecto() && r.getDesde().equals(LocalDate.of(2026, 9, 20)) && r.getHasta().equals(hoy));
        check("el rango por defecto NO cuenta como filtro elegido", !r.isElegido() && r.parametrosUrl().isEmpty());
        check("hasta exclusivo = día siguiente", r.getHastaExclusivo().equals(LocalDate.of(2026, 10, 20)));
        r = RangoFechasDTO.de(null, null, true, hoy);
        check("con búsqueda de texto: todo el historial", !r.isActivo() && !r.isPorDefecto());
        r = RangoFechasDTO.de(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), false, hoy);
        check("fechas al revés se ordenan", r.getDesde().equals(LocalDate.of(2026, 10, 1)) && r.getHasta().equals(LocalDate.of(2026, 10, 10)));
        check("elegido: parámetros para paginar", r.isElegido() && r.parametrosUrl().equals("&desde=2026-10-01&hasta=2026-10-10"));
        check("descripción legible", r.getDescripcion().equals("del 01/10/2026 al 10/10/2026"));
        r = RangoFechasDTO.de(LocalDate.of(2026, 1, 1), null, true, hoy);
        check("solo 'desde' (aunque haya texto)", r.isElegido() && r.getHasta() == null && r.getDescripcion().equals("desde el 01/01/2026"));
        FiltroBusquedaDTO f = new FiltroBusquedaDTO(null, null, null, null, 1);
        check("filtro sin rango por defecto = SIN_RANGO", f.getRango() == RangoFechasDTO.SIN_RANGO && !f.hayFiltros());
        f.conRango(RangoFechasDTO.de(null, null, false, hoy));
        check("filtro con rango por defecto: hayFiltros() sigue en false", !f.hayFiltros());
        f.conRango(RangoFechasDTO.de(LocalDate.of(2026, 10, 1), null, false, hoy));
        check("filtro con fecha elegida: hayFiltros() = true", f.hayFiltros());
        check("estados de cobros aceptados", "PENDIENTE".equals(new FiltroBusquedaDTO(null, "PENDIENTE", null, null, 1).getEstado())
              && new FiltroBusquedaDTO(null, "OTRO", null, null, 1).getEstado() == null && new FiltroBusquedaDTO(null, null, null, null, 1).getEstado() == null);
        System.out.println(fallas == 0 ? "TODAS LAS PRUEBAS PASARON" : "FALLAS: " + fallas);
        System.exit(fallas == 0 ? 0 : 1);
    }
}
