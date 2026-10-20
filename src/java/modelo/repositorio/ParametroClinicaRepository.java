package modelo.repositorio;

import modelo.dto.ParametroClinicaDTO;

/** PATRÓN: Repository  ->  CONTRATO. RF-22: parámetros de la clínica (una sola fila). */
public interface ParametroClinicaRepository {

    ParametroClinicaDTO obtener();

    boolean actualizar(ParametroClinicaDTO parametros);
}
