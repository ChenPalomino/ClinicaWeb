package modelo.dto;

/** Datos del comprobante que el cajero elige: boleta o factura y a nombre de quién. */
public class DatosClienteDTO {

    private String tipoComprobante = "BOLETA";
    private String numeroDocumento = "";   // DNI (boleta) o RUC (factura)
    private String nombre = "";            // nombre del paciente o razón social
    private String direccion = "";

    private static String limpio(String v) { return v == null ? "" : v.trim().replaceAll("\\s+", " "); }

    public boolean isFactura() { return "FACTURA".equals(tipoComprobante); }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String v) { tipoComprobante = "FACTURA".equals(v) ? "FACTURA" : "BOLETA"; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String v) { numeroDocumento = limpio(v); }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { nombre = limpio(v); }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { direccion = limpio(v); }
}
