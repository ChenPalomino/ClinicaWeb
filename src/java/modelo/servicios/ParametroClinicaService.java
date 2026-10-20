package modelo.servicios;

import configuracion.ConfiguracionClinicaSingleton;
import evento.EventoSubject;
import evento.TipoEvento;
import modelo.dto.ParametroClinicaDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.ParametroClinicaRepository;
import modelo.repositorio.ParametroClinicaRepositoryImpl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * RF-22: el administrador mantiene los datos de la clínica que salen en los comprobantes, el IGV
 * y las reglas de las citas (horas de anticipación para cancelar y días máximos para reservar).
 */
public class ParametroClinicaService {

    private final ParametroClinicaRepository repositorio;

    public ParametroClinicaService() {
        this(new ParametroClinicaRepositoryImpl());
    }

    public ParametroClinicaService(ParametroClinicaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ParametroClinicaDTO obtener() {
        return repositorio.obtener();
    }

    /** @return campo -> mensaje; vacío si se guardó */
    public Map<String, String> guardar(ParametroClinicaDTO p, Usuario admin) {
        Map<String, String> errores = validar(p);
        if (!errores.isEmpty()) {
            return errores;
        }
        ParametroClinicaDTO anterior = repositorio.obtener();
        p.setUsuarioRegistro(admin.getNombreUsuario());
        repositorio.actualizar(p);
        ConfiguracionClinicaSingleton.getInstancia().recargar();   // los cobros siguientes usan los datos nuevos
        EventoSubject.getInstancia().publicar(TipoEvento.PARAMETROS_EDITADOS, admin, 1, cambios(anterior, p));
        return errores;
    }

    static Map<String, String> validar(ParametroClinicaDTO p) {
        Map<String, String> e = new LinkedHashMap<>();
        if (!Validaciones.ruc(p.getRuc())) {
            e.put("ruc", "Escribe un RUC válido de 11 dígitos (con su dígito verificador).");
        }
        if (p.getRazonSocial().length() < 3 || p.getRazonSocial().length() > 150) {
            e.put("razonSocial", "La razón social debe tener de 3 a 150 caracteres.");
        }
        if (p.getNombreComercial().length() < 3 || p.getNombreComercial().length() > 100) {
            e.put("nombreComercial", "El nombre comercial debe tener de 3 a 100 caracteres.");
        }
        if (p.getDireccion().length() < 5 || p.getDireccion().length() > 200) {
            e.put("direccion", "La dirección debe tener de 5 a 200 caracteres.");
        }
        if (!p.getTelefono().isEmpty() && !p.getTelefono().matches("[0-9 ()+-]{6,20}")) {
            e.put("telefono", "Escribe un teléfono válido (solo números, espacios, paréntesis o guiones).");
        }
        if (fueraDeRango(p.getIgvPorcentaje(), BigDecimal.ZERO, new BigDecimal("50"))) {
            e.put("igvPorcentaje", "El IGV debe estar entre 0 y 50 %, con hasta 2 decimales.");
        }
        if (fueraDeRango(p.getMontoBoletaDni(), BigDecimal.ZERO, new BigDecimal("100000"))) {
            e.put("montoBoletaDni", "Escribe un monto válido, con hasta 2 decimales.");
        }
        if (p.getHorasCancelacionPaciente() < 1 || p.getHorasCancelacionPaciente() > 168) {
            e.put("horasCancelacionPaciente", "Escribe un número de horas entre 1 y 168 (una semana).");
        }
        if (p.getDiasMaximosReserva() < 7 || p.getDiasMaximosReserva() > 365) {
            e.put("diasMaximosReserva", "Escribe un número de días entre 7 y 365.");
        }
        return e;
    }

    private static boolean fueraDeRango(BigDecimal v, BigDecimal min, BigDecimal max) {
        return v == null || v.compareTo(min) < 0 || v.compareTo(max) > 0 || v.stripTrailingZeros().scale() > 2;
    }

    private static String cambios(ParametroClinicaDTO a, ParametroClinicaDTO n) {
        StringBuilder sb = new StringBuilder();
        agregar(sb, "RUC", a.getRuc(), n.getRuc());
        agregar(sb, "Razón social", a.getRazonSocial(), n.getRazonSocial());
        agregar(sb, "Nombre comercial", a.getNombreComercial(), n.getNombreComercial());
        agregar(sb, "Dirección", a.getDireccion(), n.getDireccion());
        agregar(sb, "Teléfono", a.getTelefono(), n.getTelefono());
        agregar(sb, "IGV %", texto(a.getIgvPorcentaje()), texto(n.getIgvPorcentaje()));
        agregar(sb, "Boleta con DNI desde", texto(a.getMontoBoletaDni()), texto(n.getMontoBoletaDni()));
        agregar(sb, "Horas para cancelar", String.valueOf(a.getHorasCancelacionPaciente()), String.valueOf(n.getHorasCancelacionPaciente()));
        agregar(sb, "Días para reservar", String.valueOf(a.getDiasMaximosReserva()), String.valueOf(n.getDiasMaximosReserva()));
        return sb.length() == 0 ? "Sin cambios" : sb.toString();
    }

    private static String texto(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private static void agregar(StringBuilder sb, String campo, String antes, String despues) {
        if (!Objects.equals(antes, despues)) {
            sb.append(sb.length() == 0 ? "" : " · ").append(campo).append(": ").append(antes).append(" → ").append(despues);
        }
    }
}
