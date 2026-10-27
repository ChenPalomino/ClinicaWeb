package pruebas.servicios;

import fakes.EspecialidadesFalsas;
import modelo.dto.EspecialidadDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.MedicoRepository;
import modelo.servicios.EspecialidadService;
import modelo.servicios.ProductoServicioService;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static fakes.Datos.bd;
import static fakes.Datos.usuario;
import static org.junit.Assert.*;

/** Mantenimiento de especialidades y su precio de consulta en el tarifario. */
public class EspecialidadServiceTest {

    /** Tarifario en memoria: guarda los servicios que crea o actualiza la especialidad. */
    static class TarifarioFalso extends ProductoServicioService {
        final Map<Integer, ProductoServicioDTO> guardados = new HashMap<>();
        String error;
        TarifarioFalso() { super(null, null); }
        @Override public Optional<ProductoServicioDTO> obtener(int id) { return Optional.ofNullable(guardados.get(id)); }
        @Override public List<OpcionDTO> categorias() {
            return List.of(new OpcionDTO(5, "Farmacia", "PRODUCTO"), new OpcionDTO(1, "Consulta médica", "SERVICIO"));
        }
        @Override public List<OpcionDTO> unidades() { return List.of(new OpcionDTO(3, "Servicio", "SERVICIO")); }
        @Override public Map<String, String> guardar(ProductoServicioDTO p, Usuario u) {
            if (error != null) return Map.of("precio", error);
            if (p.getIdProductoServicio() == 0) p.setIdProductoServicio(guardados.size() + 100);
            guardados.put(p.getIdProductoServicio(), p);
            return Map.of();
        }
    }

    private final Usuario admin = usuario("admin", 1, 1);
    private EspecialidadesFalsas repositorio;
    private TarifarioFalso tarifario;
    private EspecialidadService servicio;

    @Before
    public void preparar() {
        repositorio = new EspecialidadesFalsas();
        tarifario = new TarifarioFalso();
        servicio = new EspecialidadService(repositorio, () -> tarifario);
    }

    private static EspecialidadDTO nueva(String nombre) {
        EspecialidadDTO e = new EspecialidadDTO();
        e.setNombre(nombre);
        e.setDescripcion("");
        return e;
    }

    @Test
    public void nombrePropioYValidaciones() {
        assertEquals("Medicina General", EspecialidadService.nombrePropio("medicina general"));
        assertEquals("Cirugía de Tórax", EspecialidadService.nombrePropio("CIRUGÍA DE TÓRAX"));
        assertEquals("Unidad UCI", EspecialidadService.nombrePropio("unidad  UCI"));
        EspecialidadDTO e = nueva("Oftalmología");
        assertTrue(EspecialidadService.validar(e, "").isEmpty());
        assertTrue(EspecialidadService.validar(e, "80,50").isEmpty());
        assertTrue(EspecialidadService.validar(e, "abc").containsKey("precio"));
        assertTrue(EspecialidadService.validar(e, "0").containsKey("precio"));
        assertTrue(EspecialidadService.validar(e, "1.234").containsKey("precio"));
        e.setDescripcion("x".repeat(201));
        assertTrue(EspecialidadService.validar(e, null).containsKey("descripcion"));
        assertTrue(EspecialidadService.validar(nueva("A1"), null).containsKey("nombre"));
        assertEquals(100, MedicoRepository.LIMITE_BUSQUEDA);
    }

    @Test
    public void creaLaEspecialidadYSuConsultaEnElTarifario() {
        EspecialidadService.Guardado g = servicio.guardar(nueva("  dermatología "), "90", admin);
        assertTrue(g.isExito());
        assertNull(g.aviso());
        assertEquals("Dermatología", repositorio.datos.get(g.idEspecialidad()).getNombre());
        ProductoServicioDTO consulta = tarifario.guardados.values().iterator().next();
        assertEquals("Consulta de Dermatología", consulta.getNombre());
        assertEquals(1, consulta.getIdCategoria());
        assertEquals(0, consulta.getPrecio().compareTo(bd("90")));
        assertEquals(1, servicio.listar("derma", null).size());
        assertTrue(servicio.obtener(g.idEspecialidad()).isPresent());
    }

    @Test
    public void noRepiteNombresNiEditaInexistentes() {
        servicio.guardar(nueva("Pediatría"), "", admin);
        assertTrue(servicio.guardar(nueva("pediatría"), "", admin).errores().containsKey("nombre"));
        EspecialidadDTO fantasma = nueva("Neurología");
        fantasma.setIdEspecialidad(55);
        assertTrue(servicio.guardar(fantasma, "", admin).errores().containsKey("general"));
    }

    @Test
    public void editarActualizaElPrecioDeLaConsultaExistente() {
        int id = servicio.guardar(nueva("Cardiología"), "", admin).idEspecialidad();
        ProductoServicioDTO consulta = new ProductoServicioDTO();
        consulta.setIdProductoServicio(7);
        consulta.setPrecio(bd("100"));
        tarifario.guardados.put(7, consulta);
        EspecialidadDTO guardada = repositorio.datos.get(id);
        guardada.setIdServicioConsulta(7);
        guardada.setPrecioConsulta(bd("100"));
        EspecialidadDTO edicion = nueva("Cardiología Clínica");
        edicion.setIdEspecialidad(id);
        assertNull(servicio.guardar(edicion, "120", admin).aviso());
        assertEquals(0, consulta.getPrecio().compareTo(bd("120")));
        EspecialidadDTO mismoPrecio = nueva("Cardiología Clínica");
        mismoPrecio.setIdEspecialidad(id);
        guardada.setPrecioConsulta(bd("120"));
        assertNull(servicio.guardar(mismoPrecio, "120", admin).aviso());
        tarifario.error = "Precio fuera de rango";
        guardada.setPrecioConsulta(bd("120"));
        assertNotNull(servicio.guardar(mismoPrecio, "130", admin).aviso());
        guardada.setIdServicioConsulta(999);
        assertNotNull(servicio.guardar(mismoPrecio, "140", admin).aviso());
    }

    @Test
    public void noSeDesactivaConMedicosActivos() {
        int id = servicio.guardar(nueva("Traumatología"), "", admin).idEspecialidad();
        repositorio.datos.get(id).setMedicosActivos(2);
        assertTrue(servicio.desactivar(id, admin).contains("2 médicos"));
        EspecialidadDTO inactiva = nueva("Traumatología");
        inactiva.setIdEspecialidad(id);
        inactiva.setEstado("Inactivo");
        assertTrue(servicio.guardar(inactiva, "", admin).errores().containsKey("estado"));
        repositorio.datos.get(id).setMedicosActivos(1);
        assertTrue(servicio.desactivar(id, admin).contains("1 médico"));
        repositorio.datos.get(id).setMedicosActivos(0);
        assertNull(servicio.desactivar(id, admin));
        assertEquals("Inactivo", repositorio.datos.get(id).getEstado());
        assertNull(servicio.reactivar(id, admin));
        assertNotNull(servicio.desactivar(99, admin));
        assertNotNull(servicio.reactivar(99, admin));
    }

    @Test
    public void avisaSiNoSePudoCrearLaConsulta() {
        tarifario.error = "Código repetido";
        assertTrue(servicio.guardar(nueva("Urología"), "70", admin).aviso().contains("Código repetido"));
    }
}
