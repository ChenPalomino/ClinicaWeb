package modelo.dto;

import java.math.BigDecimal;

/** Cobertura del paciente con su porcentaje (la usa la política tarifaria, RF-12). */
public class CoberturaDTO {

    private final int idCobertura;
    private final String nombre;
    private final String tipo;
    private final BigDecimal porcentaje;

    public CoberturaDTO(int idCobertura, String nombre, String tipo, BigDecimal porcentaje) {
        this.idCobertura = idCobertura;
        this.nombre = nombre;
        this.tipo = tipo;
        this.porcentaje = porcentaje;
    }

    public int getIdCobertura() { return idCobertura; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public BigDecimal getPorcentaje() { return porcentaje; }
}
