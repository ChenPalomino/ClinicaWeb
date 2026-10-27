package fakes;

import modelo.dto.ResumenCajaDTO;
import modelo.repositorio.CierreCajaRepository;

import java.time.LocalDate;
import java.util.*;

public class CierresFalsos implements CierreCajaRepository {
    public List<ResumenCajaDTO> guardados = new ArrayList<>();
    public Integer historialPedido = -1;

    public Optional<ResumenCajaDTO> buscar(int idUsuario, LocalDate fecha) {
        return guardados.stream().filter(c -> c.getIdUsuarioCajero() == idUsuario && c.getFecha().equals(fecha)).findFirst();
    }
    public void registrar(ResumenCajaDTO cierre, String usuario) { guardados.add(cierre); }
    public List<ResumenCajaDTO> historial(Integer idUsuario, int limite) { historialPedido = idUsuario; return guardados; }
}
