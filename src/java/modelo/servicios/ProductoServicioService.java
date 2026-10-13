package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.CatalogoRepository;
import modelo.repositorio.CatalogoRepositoryImpl;
import modelo.repositorio.ProductoServicioRepository;
import modelo.repositorio.ProductoServicioRepositoryImpl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lógica de negocio del TARIFARIO: servicios médicos y productos con stock
 * (módulo 6 del curso; PP-04 "mantener el tarifario por especialidad").
 */
public class ProductoServicioService {

    private static final Pattern CODIGO = Pattern.compile("^[A-Z0-9-]{3,20}$");
    private static final Pattern CODIGO_AUTOMATICO = Pattern.compile("^(SRV|PRD)-\\d{4}$");
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999.99");
    private static final int STOCK_MAXIMO = 1_000_000;

    private final ProductoServicioRepository repositorio;
    private final CatalogoRepository catalogoRepository;

    public ProductoServicioService() {
        this(new ProductoServicioRepositoryImpl(), new CatalogoRepositoryImpl());
    }

    public ProductoServicioService(ProductoServicioRepository repositorio, CatalogoRepository catalogoRepository) {
        this.repositorio = repositorio;
        this.catalogoRepository = catalogoRepository;
    }

    public PaginaDTO<ProductoServicioDTO> listar(FiltroBusquedaDTO filtro) { return repositorio.listar(filtro); }
    public Optional<ProductoServicioDTO> obtener(int id) { return repositorio.buscarPorId(id); }
    public String siguienteCodigo(String tipo) { return repositorio.siguienteCodigo(tipo); }
    public int contarStockBajo() { return repositorio.contarStockBajo(); }

    public List<OpcionDTO> categorias() { return catalogoRepository.listarCategorias(); }
    public List<OpcionDTO> especialidades() { return catalogoRepository.listarEspecialidades(); }
    public List<OpcionDTO> marcas() { return catalogoRepository.listarMarcas(); }
    public List<OpcionDTO> unidades() { return catalogoRepository.listarUnidadesMedida(); }
    public List<OpcionDTO> proveedores() { return catalogoRepository.listarProveedoresActivos(); }

    public Map<String, String> guardar(ProductoServicioDTO p, Usuario sesion) {
        normalizar(p);
        if (p.getCodigo().isEmpty()) {
            p.setCodigo(repositorio.siguienteCodigo(p.getTipo()));   // código automático si se deja vacío
        }
        Map<String, String> errores = validar(p);
        if (!errores.containsKey("codigo") && repositorio.existeCodigo(p.getCodigo(), p.getIdProductoServicio())) {
            errores.put("codigo", "Ese código ya está en uso. Sugerido: " + repositorio.siguienteCodigo(p.getTipo()));
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        p.setUsuarioRegistro(sesion.getNombreUsuario());
        if (p.isNuevo()) {
            p.setIdProductoServicio(repositorio.insertar(p));
            EventoSubject.getInstancia().publicar(TipoEvento.ITEM_REGISTRADO, sesion, p.getIdProductoServicio(),
                    p.getCodigo() + " " + p.getNombre() + " · S/ " + p.getPrecio());
            return errores;
        }
        Optional<ProductoServicioDTO> antes = repositorio.buscarPorId(p.getIdProductoServicio());
        if (!repositorio.actualizar(p)) {
            errores.put("general", "El registro ya no existe. Actualiza la lista e inténtalo de nuevo.");
            return errores;
        }
        StringBuilder c = new StringBuilder(p.getCodigo() + " " + p.getNombre());
        antes.ifPresent(a -> {
            if (a.getPrecio() != null && a.getPrecio().compareTo(p.getPrecio()) != 0) {
                c.append(" · precio: S/ ").append(a.getPrecio()).append(" → S/ ").append(p.getPrecio());   // cambios de tarifa
            }
            if (p.isProducto() && !java.util.Objects.equals(a.getStock(), p.getStock())) {
                c.append(" · stock: ").append(a.getStock()).append(" → ").append(p.getStock());
            }
        });
        EventoSubject.getInstancia().publicar(TipoEvento.ITEM_EDITADO, sesion, p.getIdProductoServicio(), c.toString());
        return errores;
    }

    public String desactivar(int id, Usuario sesion) {
        return cambiarEstado(id, sesion, false);
    }

    public String reactivar(int id, Usuario sesion) {
        return cambiarEstado(id, sesion, true);
    }

    private String cambiarEstado(int id, Usuario sesion, boolean activar) {
        if (!(activar ? repositorio.reactivar(id) : repositorio.eliminar(id))) {
            return "El servicio o producto no existe.";
        }
        EventoSubject.getInstancia().publicar(activar ? TipoEvento.ITEM_REACTIVADO : TipoEvento.ITEM_DESACTIVADO, sesion, id,
                repositorio.buscarPorId(id).map(x -> x.getCodigo() + " " + x.getNombre()).orElse("#" + id));
        return null;
    }

    private static void normalizar(ProductoServicioDTO p) {
        p.setCodigo(p.getCodigo().trim().toUpperCase());
        p.setNombre(Validaciones.normalizar(p.getNombre()));
        p.setDescripcion(p.getDescripcion().trim());
        if (!p.isProducto()) {
            // Un servicio no tiene stock, marca ni proveedor de mercadería
            p.setStock(null);
            p.setStockMinimo(null);
            p.setIdMarca(null);
        } else {
            p.setIdEspecialidad(null);
        }
        if (!"Inactivo".equals(p.getEstado())) {
            p.setEstado("Activo");
        }
    }

    /** Reglas (usa los catálogos para comprobar que cada lista tenga un valor válido). */
    Map<String, String> validar(ProductoServicioDTO p) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!CODIGO.matcher(p.getCodigo()).matches()) {
            errores.put("codigo", "Usa de 3 a 20 letras, números o guiones (ej. SRV-0007).");
        }
        if (p.getNombre().length() < 3 || p.getNombre().length() > 120) errores.put("nombre", "Escribe un nombre de 3 a 120 caracteres.");
        if (p.getDescripcion().length() > 250) errores.put("descripcion", "Máximo 250 caracteres.");

        Optional<OpcionDTO> categoria = buscar(catalogoRepository.listarCategorias(), p.getIdCategoria());
        if (categoria.isEmpty()) {
            errores.put("idCategoria", "Elige una categoría.");
        } else if (!categoria.get().getExtra().equals(p.getTipo())) {
            errores.put("idCategoria", "La categoría «" + categoria.get().getNombre() + "» no corresponde a un "
                    + (p.isProducto() ? "producto" : "servicio") + ".");
        }
        Optional<OpcionDTO> unidad = buscar(catalogoRepository.listarUnidadesMedida(), p.getIdUnidadMedida());
        if (unidad.isEmpty()) {
            errores.put("idUnidadMedida", "Elige la unidad de medida.");
        } else if (!unidad.get().getExtra().equals(p.getTipo())) {
            errores.put("idUnidadMedida", p.isProducto()
                    ? "Un producto se mide en unidades, cajas, frascos..." : "Un servicio se mide por servicio, sesión o examen.");
        }
        // El prefijo del código automático debe coincidir con el tipo (SRV = servicio, PRD = producto)
        if (CODIGO_AUTOMATICO.matcher(p.getCodigo()).matches() && !p.getCodigo().startsWith(prefijo(p.getTipo()))) {
            errores.put("codigo", "Los códigos " + p.getCodigo().substring(0, 4) + " son para "
                    + (p.isProducto() ? "servicios" : "productos") + ". Usa " + prefijo(p.getTipo()) + "… o déjalo vacío.");
        }
        if (p.getPrecio() == null) {
            errores.put("precio", "Escribe el precio en soles (ej. 80.00).");
        } else if (p.getPrecio().signum() <= 0 || p.getPrecio().compareTo(PRECIO_MAXIMO) > 0) {
            errores.put("precio", "El precio debe ser mayor que 0 y menor que S/ 100,000.");
        } else if (p.getPrecio().scale() > 2) {
            errores.put("precio", "Usa como máximo 2 decimales.");
        }
        if (p.isProducto()) {
            if (p.getStock() == null || p.getStock() < 0 || p.getStock() > STOCK_MAXIMO) {
                errores.put("stock", "Escribe el stock actual (número entero, 0 o más).");
            }
            if (p.getStockMinimo() != null && (p.getStockMinimo() < 0 || p.getStockMinimo() > STOCK_MAXIMO)) {
                errores.put("stockMinimo", "El stock mínimo debe ser 0 o más.");
            }
        }
        return errores;
    }

    /** SRV- para servicios, PRD- para productos. */
    public static String prefijo(String tipo) {
        return ProductoServicioDTO.PRODUCTO.equals(tipo) ? "PRD-" : "SRV-";
    }

    private static Optional<OpcionDTO> buscar(List<OpcionDTO> lista, Integer id) {
        return id == null ? Optional.empty() : lista.stream().filter(o -> o.getId() == id).findFirst();
    }
}
