package modelo.citas.vista;

import java.util.List;

/**
 * Entrega la estrategia de vista según el parámetro "vista" de la URL.
 * Valor desconocido o vacío -> vista de DÍA (lo más usado: "¿qué citas hay hoy?").
 */
public final class VistaAgendaFactory {

    private static final VistaAgendaStrategy DIA = new VistaDiaStrategy();
    private static final List<VistaAgendaStrategy> TODAS = List.of(DIA, new VistaSemanaStrategy(), new VistaMesStrategy());

    private VistaAgendaFactory() {
    }

    public static VistaAgendaStrategy obtener(String codigo) {
        for (VistaAgendaStrategy v : TODAS) {
            if (v.getCodigo().equals(codigo)) {
                return v;
            }
        }
        return DIA;
    }

    /** Para dibujar los botones Día | Semana | Mes. */
    public static List<VistaAgendaStrategy> todas() {
        return TODAS;
    }
}
