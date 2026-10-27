package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;
import modelo.dto.EspecialidadDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.EspecialidadRepository;
import modelo.repositorio.EspecialidadRepositoryImpl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class EspecialidadService {

    public static final String CATEGORIA_CONSULTA = "Consulta médica";
    private static final Pattern NOMBRE = Pattern.compile("^[\\p{L}][\\p{L} ().'-]{2,59}$");
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999.99");

    private final EspecialidadRepository especialidades;
    private final Supplier<ProductoServicioService> tarifario;

    public EspecialidadService() {
        this(new EspecialidadRepositoryImpl(), ProductoServicioService::new);
    }

    public EspecialidadService(EspecialidadRepository especialidades, Supplier<ProductoServicioService> tarifario) {
        this.especialidades = especialidades;
        this.tarifario = tarifario;
    }

    public record Guardado(Map<String, String> errores, int idEspecialidad, String aviso) {
        public boolean isExito() { return errores.isEmpty(); }
    }

    public List<EspecialidadDTO> listar(String texto, String estado) { return especialidades.listar(texto, estado); }

    public Optional<EspecialidadDTO> obtener(int id) { return especialidades.buscarPorId(id); }

    public Guardado guardar(EspecialidadDTO form, String precioTexto, Usuario admin) {
        form.setNombre(nombrePropio(Validaciones.normalizar(form.getNombre())));
        form.setDescripcion(Validaciones.normalizar(form.getDescripcion()));
        Map<String, String> errores = validar(form, precioTexto);
        BigDecimal precio = leerPrecio(precioTexto);
        Optional<EspecialidadDTO> actual = form.isNuevo() ? Optional.empty() : especialidades.buscarPorId(form.getIdEspecialidad());
        validarContraLoGuardado(form, actual, errores);
        if (!errores.isEmpty()) {
            return new Guardado(errores, form.getIdEspecialidad(), null);
        }
        form.setUsuarioRegistro(admin.getNombreUsuario());
        if (form.isNuevo()) {
            form.setIdEspecialidad(especialidades.insertar(form));
            EventoSubject.getInstancia().publicar(TipoEvento.ESPECIALIDAD_CREADA, admin, form.getIdEspecialidad(), form.getNombre());
        } else {
            especialidades.actualizar(form);
            EventoSubject.getInstancia().publicar(TipoEvento.ESPECIALIDAD_EDITADA, admin, form.getIdEspecialidad(),
                    actual.get().getNombre().equals(form.getNombre()) ? form.getNombre()
                            : actual.get().getNombre() + " → " + form.getNombre());
        }
        String aviso = null;
        if (precio != null) {
            EspecialidadDTO guardada = especialidades.buscarPorId(form.getIdEspecialidad()).orElse(form);
            aviso = sincronizarConsulta(guardada, precio, admin);
        }
        return new Guardado(errores, form.getIdEspecialidad(), aviso);
    }

    private void validarContraLoGuardado(EspecialidadDTO form, Optional<EspecialidadDTO> actual, Map<String, String> errores) {
        if (!form.isNuevo() && actual.isEmpty()) {
            errores.put("general", "La especialidad ya no existe. Actualiza la lista e inténtalo de nuevo.");
        }
        if (!errores.containsKey("nombre") && especialidades.existeNombre(form.getNombre(), form.getIdEspecialidad())) {
            errores.put("nombre", "Ya existe la especialidad «" + form.getNombre() + "».");
        }
        if (actual.isPresent() && actual.get().isActivo() && !form.isActivo() && actual.get().getMedicosActivos() > 0) {
            errores.put("estado", mensajeMedicosActivos(actual.get().getMedicosActivos()));
        }
    }

    public String desactivar(int id, Usuario admin) {
        Optional<EspecialidadDTO> e = especialidades.buscarPorId(id);
        if (e.isEmpty()) {
            return "La especialidad no existe.";
        }
        if (e.get().getMedicosActivos() > 0) {
            return mensajeMedicosActivos(e.get().getMedicosActivos());
        }
        especialidades.cambiarEstado(id, "Inactivo");
        EventoSubject.getInstancia().publicar(TipoEvento.ESPECIALIDAD_DESACTIVADA, admin, id, e.get().getNombre());
        return null;
    }

    public String reactivar(int id, Usuario admin) {
        Optional<EspecialidadDTO> e = especialidades.buscarPorId(id);
        if (e.isEmpty()) {
            return "La especialidad no existe.";
        }
        especialidades.cambiarEstado(id, "Activo");
        EventoSubject.getInstancia().publicar(TipoEvento.ESPECIALIDAD_REACTIVADA, admin, id, e.get().getNombre());
        return null;
    }

    public static Map<String, String> validar(EspecialidadDTO e, String precioTexto) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!NOMBRE.matcher(e.getNombre()).matches()) {
            errores.put("nombre", "Escribe un nombre de 3 a 60 letras (ej. Oftalmología).");
        }
        if (e.getDescripcion().length() > 200) {
            errores.put("descripcion", "Máximo 200 caracteres.");
        }
        if (precioTexto != null && !precioTexto.isBlank()) {
            BigDecimal precio = leerPrecio(precioTexto);
            if (precio == null) {
                errores.put("precio", "Escribe el precio en soles (ej. 80.00) o déjalo vacío.");
            } else if (precio.signum() <= 0 || precio.compareTo(PRECIO_MAXIMO) > 0) {
                errores.put("precio", "El precio debe ser mayor que 0 y menor que S/ 100,000.");
            } else if (precio.scale() > 2) {
                errores.put("precio", "Usa como máximo 2 decimales.");
            }
        }
        return errores;
    }

    private static final java.util.Set<String> CONECTORES = java.util.Set.of("de", "del", "la", "las", "los", "y", "e", "en");

    public static String nombrePropio(String texto) {
        StringBuilder sb = new StringBuilder();
        String[] palabras = texto.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            String p = palabras[i];
            if (p.isEmpty()) {
                continue;
            }
            boolean conector = i > 0 && CONECTORES.contains(p.toLowerCase(java.util.Locale.ROOT));
            boolean siglas = p.length() > 1 && p.equals(p.toUpperCase(java.util.Locale.ROOT)) && p.length() <= 4;
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (conector) {
                sb.append(p.toLowerCase(java.util.Locale.ROOT));
            } else if (siglas) {
                sb.append(p);
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(java.util.Locale.ROOT));
            }
        }
        return sb.toString();
    }

    static BigDecimal leerPrecio(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String mensajeMedicosActivos(int n) {
        return "No se puede desactivar: " + (n == 1 ? "1 médico activo la tiene" : n + " médicos activos la tienen")
                + ". Cámbialos de especialidad o pausa sus citas en «Médicos y horarios» primero.";
    }

    private String sincronizarConsulta(EspecialidadDTO e, BigDecimal precio, Usuario admin) {
        ProductoServicioService t = tarifario.get();
        if (e.isTieneConsulta()) {
            if (e.getPrecioConsulta() != null && e.getPrecioConsulta().compareTo(precio) == 0) {
                return null;
            }
            Optional<ProductoServicioDTO> consulta = t.obtener(e.getIdServicioConsulta());
            if (consulta.isEmpty()) {
                return "No se encontró la consulta en el tarifario para cambiar su precio.";
            }
            consulta.get().setPrecio(precio);
            Map<String, String> errores = t.guardar(consulta.get(), admin);
            return errores.isEmpty() ? null : "La especialidad se guardó, pero no el precio: " + errores.values().iterator().next();
        }
        ProductoServicioDTO nueva = new ProductoServicioDTO();
        nueva.setTipo(ProductoServicioDTO.SERVICIO);
        nueva.setCodigo("");
        nueva.setNombre("Consulta de " + e.getNombre());
        nueva.setDescripcion("");
        nueva.setIdEspecialidad(e.getIdEspecialidad());
        nueva.setPrecio(precio);
        nueva.setAfectoIgv(true);
        nueva.setEstado("Activo");
        nueva.setIdCategoria(elegir(t.categorias(), CATEGORIA_CONSULTA));
        nueva.setIdUnidadMedida(elegir(t.unidades(), "Servicio"));
        Map<String, String> errores = t.guardar(nueva, admin);
        return errores.isEmpty() ? null
                : "La especialidad se guardó, pero no se pudo crear su consulta en el tarifario: " + errores.values().iterator().next();
    }

    private static int elegir(List<OpcionDTO> opciones, String nombrePreferido) {
        return opciones.stream().filter(o -> ProductoServicioDTO.SERVICIO.equals(o.getExtra()) && o.getNombre().startsWith(nombrePreferido))
                .findFirst()
                .or(() -> opciones.stream().filter(o -> ProductoServicioDTO.SERVICIO.equals(o.getExtra())).findFirst())
                .map(OpcionDTO::getId).orElse(0);
    }
}
