package modelo.repositorio;

import modelo.dto.BilleteraConfigDTO;
import modelo.dto.OpcionDTO;

import java.util.List;
import java.util.Optional;

public interface BilleteraRepository {

    BilleteraConfigDTO obtenerConfiguracion();

    void guardarDatos(String celular, String titular, String usuario);

    void guardarQr(byte[] imagen, String tipoMime, String usuario);

    void quitarQr(String usuario);

    Optional<Object[]> obtenerQr();

    List<OpcionDTO> listarTodas();

    boolean existeNombre(String nombre);

    int crear(String nombre, String usuario);

    boolean cambiarEstado(int idBilletera, String estado);
}
