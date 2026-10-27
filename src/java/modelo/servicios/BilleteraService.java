package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;
import modelo.dto.BilleteraConfigDTO;
import modelo.dto.OpcionDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.BilleteraRepository;
import modelo.repositorio.BilleteraRepositoryImpl;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BilleteraService {

    public static final int QR_MAXIMO_BYTES = 400_000;
    private static final Set<String> TIPOS_QR = Set.of("image/png", "image/jpeg", "image/webp");

    private final BilleteraRepository repositorio;

    public BilleteraService() {
        this(new BilleteraRepositoryImpl());
    }

    public BilleteraService(BilleteraRepository repositorio) {
        this.repositorio = repositorio;
    }

    public BilleteraConfigDTO configuracion() { return repositorio.obtenerConfiguracion(); }
    public List<OpcionDTO> billeteras() { return repositorio.listarTodas(); }
    public Optional<Object[]> qr() { return repositorio.obtenerQr(); }

    public Map<String, String> guardarDatos(BilleteraConfigDTO datos, Usuario admin) {
        Map<String, String> errores = validarDatos(datos);
        if (errores.isEmpty()) {
            repositorio.guardarDatos(datos.getCelular(), datos.getTitular(), admin.getNombreUsuario());
            EventoSubject.getInstancia().publicar(TipoEvento.PARAMETROS_EDITADOS, admin, 1,
                    "Billetera digital: celular " + (datos.getCelular().isEmpty() ? "—" : datos.getCelular())
                            + " · titular " + (datos.getTitular().isEmpty() ? "—" : datos.getTitular()));
        }
        return errores;
    }

    public static Map<String, String> validarDatos(BilleteraConfigDTO d) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!d.getCelular().isEmpty() && !d.getCelular().matches("9\\d{8}")) {
            errores.put("celular", "El celular tiene 9 dígitos y empieza con 9 (o déjalo vacío).");
        }
        if (d.getTitular().length() > 80) {
            errores.put("titular", "Máximo 80 caracteres.");
        }
        return errores;
    }

    public String guardarQr(String dataUrl, Usuario admin) {
        if (dataUrl == null || !dataUrl.startsWith("data:") || !dataUrl.contains(";base64,")) {
            return "Elige una imagen del QR (PNG, JPG o WEBP).";
        }
        String tipo = dataUrl.substring(5, dataUrl.indexOf(';'));
        if (!TIPOS_QR.contains(tipo)) {
            return "El QR debe ser una imagen PNG, JPG o WEBP.";
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            return "No se pudo leer la imagen. Intenta con otra.";
        }
        if (bytes.length == 0 || bytes.length > QR_MAXIMO_BYTES) {
            return "La imagen pesa demasiado: máximo " + (QR_MAXIMO_BYTES / 1000) + " KB.";
        }
        repositorio.guardarQr(bytes, tipo, admin.getNombreUsuario());
        EventoSubject.getInstancia().publicar(TipoEvento.PARAMETROS_EDITADOS, admin, 1, "Cambió el QR de pagos");
        return null;
    }

    public void quitarQr(Usuario admin) {
        repositorio.quitarQr(admin.getNombreUsuario());
        EventoSubject.getInstancia().publicar(TipoEvento.PARAMETROS_EDITADOS, admin, 1, "Quitó el QR de pagos");
    }

    public String agregar(String nombre, Usuario admin) {
        String n = Validaciones.normalizar(nombre);
        if (n.length() < 2 || n.length() > 40) {
            return "Escribe el nombre de la billetera (de 2 a 40 caracteres).";
        }
        if (repositorio.existeNombre(n)) {
            return "La billetera «" + n + "» ya está en la lista.";
        }
        int id = repositorio.crear(n, admin.getNombreUsuario());
        EventoSubject.getInstancia().publicar(TipoEvento.PARAMETROS_EDITADOS, admin, 1, "Agregó la billetera " + n + " (#" + id + ")");
        return null;
    }

    public String cambiarEstado(int idBilletera, boolean activar, Usuario admin) {
        List<OpcionDTO> todas = repositorio.listarTodas();
        Optional<OpcionDTO> b = todas.stream().filter(o -> o.getId() == idBilletera).findFirst();
        if (b.isEmpty()) {
            return "La billetera no existe.";
        }
        if (!activar && todas.stream().filter(o -> "Activo".equals(o.getExtra())).count() <= 1) {
            return "Deja al menos una billetera activa para poder cobrar con billetera digital.";
        }
        repositorio.cambiarEstado(idBilletera, activar ? "Activo" : "Inactivo");
        EventoSubject.getInstancia().publicar(TipoEvento.PARAMETROS_EDITADOS, admin, 1,
                (activar ? "Activó" : "Desactivó") + " la billetera " + b.get().getNombre());
        return null;
    }
}
