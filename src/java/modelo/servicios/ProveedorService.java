package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProveedorDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.ProveedorRepository;
import modelo.repositorio.ProveedorRepositoryImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lógica de negocio de PROVEEDORES (módulo 5 del curso). */
public class ProveedorService {

    /** Rubros sugeridos para una clínica (la vista los muestra como lista). */
    public static final List<String> RUBROS = List.of(
            "Medicamentos", "Insumos médicos", "Laboratorio", "Equipamiento médico", "Servicios generales", "Otros");

    private final ProveedorRepository proveedorRepository;

    public List<String> rubros() { return proveedorRepository.rubros(); }

    public ProveedorService() {
        this(new ProveedorRepositoryImpl());
    }

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public PaginaDTO<ProveedorDTO> listar(FiltroBusquedaDTO filtro) { return proveedorRepository.listar(filtro); }
    public Optional<ProveedorDTO> obtener(int id) { return proveedorRepository.buscarPorId(id); }

    public Map<String, String> guardar(ProveedorDTO p, Usuario sesion) {
        normalizar(p);
        Map<String, String> errores = validar(p);
        if (!errores.containsKey("ruc") && proveedorRepository.existeRuc(p.getRuc(), p.getIdProveedor())) {
            errores.put("ruc", "Ya hay un proveedor registrado con ese RUC.");
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        p.setUsuarioRegistro(sesion.getNombreUsuario());
        boolean nuevo = p.isNuevo();
        if (nuevo) {
            p.setIdProveedor(proveedorRepository.insertar(p));
        } else if (!proveedorRepository.actualizar(p)) {
            errores.put("general", "El proveedor ya no existe. Actualiza la lista e inténtalo de nuevo.");
            return errores;
        }
        EventoSubject.getInstancia().publicar(nuevo ? TipoEvento.PROVEEDOR_REGISTRADO : TipoEvento.PROVEEDOR_EDITADO,
                sesion, p.getIdProveedor(), p.getRazonSocial() + " · RUC " + p.getRuc());
        return errores;
    }

    public String desactivar(int id, Usuario sesion) {
        return cambiarEstado(id, sesion, false);
    }

    public String reactivar(int id, Usuario sesion) {
        return cambiarEstado(id, sesion, true);
    }

    private String cambiarEstado(int id, Usuario sesion, boolean activar) {
        if (!(activar ? proveedorRepository.reactivar(id) : proveedorRepository.eliminar(id))) {
            return "El proveedor no existe.";
        }
        EventoSubject.getInstancia().publicar(activar ? TipoEvento.PROVEEDOR_REACTIVADO : TipoEvento.PROVEEDOR_DESACTIVADO,
                sesion, id, proveedorRepository.buscarPorId(id).map(ProveedorDTO::getRazonSocial).orElse("#" + id));
        return null;
    }

    private static void normalizar(ProveedorDTO p) {
        p.setRuc(Validaciones.soloDigitos(p.getRuc()));
        p.setRazonSocial(Validaciones.normalizar(p.getRazonSocial()));
        p.setNombreComercial(Validaciones.normalizar(p.getNombreComercial()));
        p.setRubro(Validaciones.normalizar(p.getRubro()));
        p.setContactoNombre(Validaciones.normalizar(p.getContactoNombre()));
        p.setTelefono(Validaciones.soloDigitos(p.getTelefono()));
        p.setCorreo(p.getCorreo().trim().toLowerCase());
        p.setDireccion(Validaciones.normalizar(p.getDireccion()));
        if (!"Inactivo".equals(p.getEstado())) {
            p.setEstado("Activo");
        }
    }

    /** Reglas puras (sin BD). */
    Map<String, String> validar(ProveedorDTO p) {
        Map<String, String> errores = new LinkedHashMap<>();
        validarEmpresa(p, errores);
        validarContacto(p, errores);
        return errores;
    }

    private static void validarEmpresa(ProveedorDTO p, Map<String, String> errores) {
        if (!Validaciones.ruc(p.getRuc())) {
            errores.put("ruc", "El RUC debe tener 11 números, empezar con 10 o 20 y tener un dígito verificador válido.");
        }
        if (p.getRazonSocial().length() < 3 || p.getRazonSocial().length() > 150) {
            errores.put("razonSocial", "Escribe la razón social tal como figura en SUNAT (3 a 150 caracteres).");
        }
        if (p.getNombreComercial().length() > 100) errores.put("nombreComercial", "Máximo 100 caracteres.");
        if (p.getRubro().isEmpty() || p.getRubro().length() > 60) errores.put("rubro", "Elige o escribe el rubro del proveedor.");
        if (p.getDireccion().length() > 200) errores.put("direccion", "Máximo 200 caracteres.");
    }

    private static void validarContacto(ProveedorDTO p, Map<String, String> errores) {
        if (!p.getContactoNombre().isEmpty() && !Validaciones.nombre(p.getContactoNombre())) {
            errores.put("contactoNombre", "Escribe el nombre del contacto solo con letras.");
        }
        if (!p.getTelefono().isEmpty() && !Validaciones.telefono(p.getTelefono())) {
            errores.put("telefono", "El teléfono debe tener de 7 a 9 números.");
        }
        if (!p.getCorreo().isEmpty() && !Validaciones.correo(p.getCorreo())) errores.put("correo", "Escribe un correo válido.");
    }
}
