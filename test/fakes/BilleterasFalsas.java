package fakes;

import modelo.dto.BilleteraConfigDTO;
import modelo.dto.OpcionDTO;
import modelo.repositorio.BilleteraRepository;

import java.util.*;

public class BilleterasFalsas implements BilleteraRepository {
    public String celular = "", titular = "";
    public byte[] qr; public String mime;
    public List<OpcionDTO> lista = new ArrayList<>(List.of(new OpcionDTO(1, "Yape", "Activo"), new OpcionDTO(2, "Plin", "Activo")));

    public BilleteraConfigDTO obtenerConfiguracion() {
        BilleteraConfigDTO c = new BilleteraConfigDTO();
        c.setCelular(celular); c.setTitular(titular); c.setTieneQr(qr != null); c.setVersionQr(qr == null ? 0 : qr.length);
        return c;
    }
    public void guardarDatos(String c, String t, String u) { celular = c; titular = t; }
    public void guardarQr(byte[] i, String m, String u) { qr = i; mime = m; }
    public void quitarQr(String u) { qr = null; mime = null; }
    public Optional<Object[]> obtenerQr() { return qr == null ? Optional.empty() : Optional.of(new Object[]{qr, mime}); }
    public List<OpcionDTO> listarTodas() { return lista; }
    public boolean existeNombre(String n) { return lista.stream().anyMatch(o -> o.getNombre().equalsIgnoreCase(n)); }
    public int crear(String n, String u) { int id = lista.size() + 1; lista.add(new OpcionDTO(id, n, "Activo")); return id; }
    public boolean cambiarEstado(int id, String estado) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == id) { lista.set(i, new OpcionDTO(id, lista.get(i).getNombre(), estado)); return true; }
        }
        return false;
    }
}
