package modelo.cobranza.tarifa;

import java.util.Map;

/**
 * Elige la estrategia de tarifa según el tipo de cobertura del paciente
 * (igual que MenuFactory elige el menú según el rol).
 * Las estrategias no guardan estado, así que se reutiliza una instancia de cada una.
 */
public final class PoliticaTarifariaFactory {

    private static final Map<String, PoliticaTarifariaStrategy> ESTRATEGIAS = Map.of(
            "PARTICULAR", new TarifaParticularStrategy(),
            "EPS", new TarifaEpsStrategy(),
            "SEGURO", new TarifaSeguroStrategy(),
            "CONVENIO", new TarifaConvenioStrategy());

    private PoliticaTarifariaFactory() {
    }

    /** @throws IllegalArgumentException si el tipo de cobertura no existe */
    public static PoliticaTarifariaStrategy obtener(String tipoCobertura) {
        PoliticaTarifariaStrategy estrategia = tipoCobertura == null ? null : ESTRATEGIAS.get(tipoCobertura);
        if (estrategia == null) {
            throw new IllegalArgumentException("Tipo de cobertura desconocido: " + tipoCobertura);
        }
        return estrategia;
    }
}
