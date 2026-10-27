package modelo.dto;

public class BilleteraConfigDTO {

    private String celular = "";
    private String titular = "";
    private boolean tieneQr;
    private long versionQr;

    private static String limpio(String v) { return v == null ? "" : v.trim(); }

    public String getCelular() { return celular; }
    public void setCelular(String v) { celular = limpio(v).replaceAll("\\s", ""); }
    public String getTitular() { return titular; }
    public void setTitular(String v) { titular = limpio(v).replaceAll("\\s+", " "); }
    public boolean isTieneQr() { return tieneQr; }
    public void setTieneQr(boolean v) { tieneQr = v; }
    public long getVersionQr() { return versionQr; }
    public void setVersionQr(long v) { versionQr = v; }

    public String getCelularLegible() {
        return celular.length() == 9 ? celular.substring(0, 3) + " " + celular.substring(3, 6) + " " + celular.substring(6) : celular;
    }
}
