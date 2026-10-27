package modelo.dto;

import java.math.BigDecimal;

public record ParValorDTO(String etiqueta, BigDecimal valor, BigDecimal valor2) {

    public ParValorDTO {
        etiqueta = etiqueta == null ? "" : etiqueta;
        valor = valor == null ? BigDecimal.ZERO : valor;
        valor2 = valor2 == null ? BigDecimal.ZERO : valor2;
    }
}
