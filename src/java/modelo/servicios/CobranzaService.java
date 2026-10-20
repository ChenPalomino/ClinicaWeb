package modelo.servicios;

import configuracion.ConfiguracionClinicaSingleton;
import evento.EventoSubject;
import evento.TipoEvento;
import modelo.cobranza.CalculadoraCuenta;
import modelo.cobranza.TotalesCuenta;
import modelo.cobranza.comprobante.Comprobante;
import modelo.cobranza.comprobante.ComprobanteFactory;
import modelo.cobranza.pago.MetodoPagoFactory;
import modelo.cobranza.pago.MetodoPagoStrategy;
import modelo.cobranza.pago.ResultadoPago;
import modelo.cobranza.pago.SolicitudPago;
import modelo.cobranza.tarifa.PoliticaTarifariaFactory;
import modelo.cobranza.tarifa.PoliticaTarifariaStrategy;
import modelo.dto.CoberturaDTO;
import modelo.dto.ComprobanteDTO;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DatosClienteDTO;
import modelo.dto.DetalleCuentaDTO;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.PagoDTO;
import modelo.dto.ParametroClinicaDTO;
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.repositorio.CuentaAtencionRepository;
import modelo.repositorio.CuentaAtencionRepositoryImpl;
import modelo.repositorio.OperacionRechazadaException;
import modelo.repositorio.PagoRepository;
import modelo.repositorio.PagoRepositoryImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Servicio de FACTURACIÓN Y COBRANZA EN CAJA (PP-03, RF-11 a RF-16).
 *
 * Reúne los patrones del módulo sin conocer sus clases concretas:
 *   - Strategy:       PoliticaTarifariaStrategy (cuánto paga el paciente según su cobertura)
 *   - Strategy:       MetodoPagoStrategy (efectivo, tarjeta, billetera)
 *   - Factory Method: ComprobanteFactory (boleta o factura)
 *   - Singleton:      ConfiguracionClinicaSingleton (IGV y datos de la clínica)
 *   - Repository:     CuentaAtencionRepository y PagoRepository (transacción atómica, RNF-08)
 *   - Observer:       cada operación publica un evento que se audita (RF-21, RNF-10)
 * Recibe sus dependencias por el constructor, así se prueba sin base de datos (RNF-03).
 */
public class CobranzaService {

    public static final int MAXIMO_LINEAS = 30;
    public static final int MAXIMO_CANTIDAD = 99;
    public static final int MOTIVO_MINIMO = 10;

    private final CuentaAtencionRepository cuentas;
    private final PagoRepository pagos;
    private final Supplier<ParametroClinicaDTO> parametros;
    private final Supplier<LocalDate> hoy;

    public CobranzaService() {
        this(new CuentaAtencionRepositoryImpl(), new PagoRepositoryImpl(),
                () -> ConfiguracionClinicaSingleton.getInstancia().getParametros(), LocalDate::now);
    }

    public CobranzaService(CuentaAtencionRepository cuentas, PagoRepository pagos,
                           Supplier<ParametroClinicaDTO> parametros, Supplier<LocalDate> hoy) {
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.parametros = parametros;
        this.hoy = hoy;
    }

    /** Resultado de abrir una cuenta: la nueva, o la pendiente que el paciente ya tenía. */
    public record Apertura(int idCuenta, boolean yaExistia, String error) {
        public boolean isExito() { return error == null; }
    }

    /** Resultado de cobrar: datos por corregir, pago rechazado (RF-14), error o comprobante emitido. */
    public record ResultadoCobro(Map<String, String> errores, String rechazo, String error,
                                 int idComprobante, String comprobante, BigDecimal vuelto) {
        static ResultadoCobro conErrores(Map<String, String> errores) { return new ResultadoCobro(errores, null, null, 0, null, null); }
        static ResultadoCobro rechazado(String motivo) { return new ResultadoCobro(Map.of(), motivo, null, 0, null, null); }
        static ResultadoCobro fallido(String error) { return new ResultadoCobro(Map.of(), null, error, 0, null, null); }
        public boolean isExito() { return idComprobante > 0; }
    }

    // ===================== consultas =====================

    public PaginaDTO<CuentaAtencionDTO> listar(FiltroBusquedaDTO filtro) { return cuentas.listar(filtro); }
    public Optional<CuentaAtencionDTO> obtener(int id) { return cuentas.buscarPorId(id); }
    public List<ProductoServicioDTO> itemsVendibles() { return cuentas.itemsVendibles(); }
    public List<OpcionDTO> metodosPago() { return pagos.metodosActivos(); }
    public List<CuentaAtencionDTO> pagosDelPaciente(int idUsuario) { return cuentas.listarDelUsuarioPaciente(idUsuario); }
    public int contarPendientes() { return cuentas.contarPendientes(); }
    public ParametroClinicaDTO parametros() { return parametros.get(); }

    /** Texto que explica cómo se calcula la cuenta (Strategy de la cobertura). */
    public static String describirTarifa(CuentaAtencionDTO c) {
        return PoliticaTarifariaFactory.obtener(c.getTipoCobertura()).describir(c.getPorcentajeCobertura());
    }

    /** RF-16: anular requiere autorización del administrador. */
    public static boolean puedeAnular(Usuario u) {
        return u != null && u.getRolId() == Rol.ADMINISTRADOR;
    }

    /** El personal ve cualquier comprobante; el paciente solo los suyos. */
    public Optional<ComprobanteDTO> comprobanteVisiblePara(int idComprobante, Usuario u) {
        Optional<ComprobanteDTO> c = pagos.buscarComprobante(idComprobante);
        if (c.isEmpty() || u == null) {
            return Optional.empty();
        }
        boolean personal = u.getRolId() == Rol.ADMINISTRADOR || u.getRolId() == Rol.RECEPCIONISTA;
        return personal || c.get().getIdUsuarioPaciente() == u.getIdUsuario() ? c : Optional.empty();
    }

    // ===================== RF-11: cuenta de atención =====================

    public Apertura abrirCuenta(int idPaciente, String observacion, Usuario cajero) {
        Optional<Integer> pendiente = cuentas.pendienteDelPaciente(idPaciente);
        if (pendiente.isPresent()) {
            return new Apertura(pendiente.get(), true, null);
        }
        Optional<CoberturaDTO> cobertura = cuentas.coberturaDelPaciente(idPaciente);
        if (cobertura.isEmpty()) {
            return new Apertura(0, false, "El paciente no existe o está desactivado.");
        }
        String obs = observacion == null ? "" : observacion.trim();
        if (obs.length() > 250) {
            return new Apertura(0, false, "La observación no puede pasar de 250 caracteres.");
        }
        int id = cuentas.crear(idPaciente, cobertura.get().getIdCobertura(), obs, cajero.getNombreUsuario());
        EventoSubject.getInstancia().publicar(TipoEvento.CUENTA_ABIERTA, cajero, id,
                "Cobertura: " + cobertura.get().getNombre());
        return new Apertura(id, false, null);
    }

    /** Agrega un servicio o producto; si ya estaba en la cuenta, suma la cantidad. @return null si todo salió bien */
    public String agregarItem(int idCuenta, int idProductoServicio, int cantidad, Usuario cajero) {
        if (cantidad < 1 || cantidad > MAXIMO_CANTIDAD) {
            return "La cantidad debe estar entre 1 y " + MAXIMO_CANTIDAD + ".";
        }
        Optional<CuentaAtencionDTO> cuenta = cuentas.buscarPorId(idCuenta);
        String error = validarPendiente(cuenta);
        if (error != null) {
            return error;
        }
        Optional<DetalleCuentaDTO> existente = buscarLinea(cuenta.get(), idProductoServicio);
        int nueva = cantidad + existente.map(DetalleCuentaDTO::getCantidad).orElse(0);
        return fijarCantidad(cuenta.get(), idProductoServicio, nueva, cajero);
    }

    /** Cambia la cantidad de una línea (0 = quitarla). @return null si todo salió bien */
    public String cambiarCantidad(int idCuenta, int idProductoServicio, int cantidad, Usuario cajero) {
        if (cantidad < 0 || cantidad > MAXIMO_CANTIDAD) {
            return "La cantidad debe estar entre 1 y " + MAXIMO_CANTIDAD + ".";
        }
        Optional<CuentaAtencionDTO> cuenta = cuentas.buscarPorId(idCuenta);
        String error = validarPendiente(cuenta);
        return error != null ? error : fijarCantidad(cuenta.get(), idProductoServicio, cantidad, cajero);
    }

    private String fijarCantidad(CuentaAtencionDTO cuenta, int idItem, int cantidad, Usuario cajero) {
        if (cantidad > MAXIMO_CANTIDAD) {
            return "No se pueden agregar más de " + MAXIMO_CANTIDAD + " unidades del mismo servicio o producto.";
        }
        List<DetalleCuentaDTO> lineas = new ArrayList<>(cuenta.getDetalles());
        Optional<DetalleCuentaDTO> existente = buscarLinea(cuenta, idItem);
        if (cantidad == 0) {
            if (existente.isEmpty()) {
                return "Ese servicio o producto no está en la cuenta.";
            }
            lineas.remove(existente.get());
        } else {
            Optional<ProductoServicioDTO> item = cuentas.itemsVendibles().stream()
                    .filter(i -> i.getIdProductoServicio() == idItem).findFirst();
            if (item.isEmpty() && existente.isEmpty()) {
                return "Ese servicio o producto no existe o fue desactivado del tarifario.";
            }
            if (item.isPresent() && item.get().getStock() != null && cantidad > item.get().getStock()) {
                return "Solo quedan " + item.get().getStock() + " unidades de «" + item.get().getNombre() + "».";
            }
            if (existente.isPresent()) {
                existente.get().setCantidad(cantidad);
            } else {
                if (lineas.size() >= MAXIMO_LINEAS) {
                    return "Una cuenta puede tener hasta " + MAXIMO_LINEAS + " líneas.";
                }
                lineas.add(nuevaLinea(item.get(), cantidad));
            }
        }
        // RF-12: todas las líneas se recalculan con la política de la cobertura (Strategy)
        PoliticaTarifariaStrategy politica = PoliticaTarifariaFactory.obtener(cuenta.getTipoCobertura());
        for (DetalleCuentaDTO l : lineas) {
            CalculadoraCuenta.tarifar(l, politica, cuenta.getPorcentajeCobertura());
        }
        TotalesCuenta totales = CalculadoraCuenta.totalizar(lineas, parametros.get().getIgvPorcentaje());
        if (!cuentas.guardarDetalles(cuenta.getIdCuentaAtencion(), lineas, totales, cajero.getNombreUsuario())) {
            return "La cuenta ya no está pendiente (otro usuario la cobró o anuló). No se hicieron cambios.";
        }
        return null;
    }

    private static DetalleCuentaDTO nuevaLinea(ProductoServicioDTO item, int cantidad) {
        DetalleCuentaDTO l = new DetalleCuentaDTO();
        l.setIdProductoServicio(item.getIdProductoServicio());
        l.setCodigo(item.getCodigo());
        l.setDescripcion(item.getNombre());
        l.setTipo(item.getTipo());
        l.setCantidad(cantidad);
        l.setPrecioUnitario(item.getPrecio());      // se congela el precio del momento
        l.setAfectoIgv(item.isAfectoIgv());
        return l;
    }

    private static Optional<DetalleCuentaDTO> buscarLinea(CuentaAtencionDTO c, int idItem) {
        return c.getDetalles().stream().filter(d -> d.getIdProductoServicio() == idItem).findFirst();
    }

    private static String validarPendiente(Optional<CuentaAtencionDTO> cuenta) {
        if (cuenta.isEmpty()) {
            return "La cuenta no existe.";
        }
        if (!cuenta.get().isPendiente()) {
            return "La cuenta ya está " + (cuenta.get().isPagada() ? "pagada" : "anulada") + " y no se puede modificar.";
        }
        return null;
    }

    // ===================== RF-13, RF-14, RF-15: cobrar =====================

    public ResultadoCobro cobrar(int idCuenta, String codigoMetodo, SolicitudPago solicitud, DatosClienteDTO cliente,
                                 Usuario cajero) {
        Optional<CuentaAtencionDTO> encontrada = cuentas.buscarPorId(idCuenta);
        String error = validarPendiente(encontrada);
        if (error != null) {
            return ResultadoCobro.fallido(error);
        }
        CuentaAtencionDTO cuenta = encontrada.get();
        if (cuenta.getDetalles().isEmpty()) {
            return ResultadoCobro.fallido("Agrega al menos un servicio o producto antes de cobrar.");
        }
        ParametroClinicaDTO p = parametros.get();
        TotalesCuenta totales = cuenta.getTotales();
        solicitud.setMonto(cuenta.getTotal());

        // 1. Validar: método de pago (Strategy) y datos del comprobante (Factory Method)
        Map<String, String> errores = new LinkedHashMap<>();
        MetodoPagoStrategy metodo = MetodoPagoFactory.obtener(codigoMetodo);
        if (metodo == null) {
            errores.put("metodo", "Elige cómo paga el paciente.");
        } else {
            errores.putAll(metodo.validar(solicitud));
        }
        ComprobanteFactory fabrica = ComprobanteFactory.para(cliente.getTipoComprobante());
        errores.putAll(fabrica.validarCliente(cliente, totales, p));
        if (!errores.isEmpty()) {
            return ResultadoCobro.conErrores(errores);
        }

        // 2. Procesar el pago con la estrategia elegida
        ResultadoPago resultado = metodo.procesar(solicitud);
        PagoDTO pago = new PagoDTO();
        pago.setIdCuentaAtencion(idCuenta);
        pago.setCodigoMetodo(metodo.getCodigo());
        pago.setMonto(cuenta.getTotal());
        pago.setMontoRecibido(resultado.getMontoRecibido());
        pago.setVuelto(resultado.getVuelto());
        pago.setReferencia(resultado.getReferencia());
        pago.setIdUsuarioCajero(cajero.getIdUsuario());
        pago.setNombreCajero(cajero.getNombreUsuario());

        if (!resultado.isAprobado()) {      // RF-14: se registra y la cuenta sigue pendiente
            pago.setEstadoPago(PagoDTO.RECHAZADO);
            pago.setMotivoRechazo(resultado.getMotivoRechazo());
            pagos.registrarRechazo(pago);
            EventoSubject.getInstancia().publicar(TipoEvento.PAGO_RECHAZADO, cajero, idCuenta,
                    cuenta.getNumeroCuenta() + " · " + resultado.getReferencia());
            return ResultadoCobro.rechazado(resultado.getMotivoRechazo());
        }

        // 3. Emitir el comprobante (Factory Method) y guardar TODO en una transacción (RNF-08)
        pago.setEstadoPago(PagoDTO.APROBADO);
        Comprobante comprobante = fabrica.emitir(cliente, totales, p);
        int idComprobante;
        try {
            idComprobante = pagos.registrarPagoAprobado(pago, comprobante, cuenta.getDetalles(), hoy.get());
        } catch (OperacionRechazadaException e) {
            return ResultadoCobro.fallido(e.getMessage());
        }
        EventoSubject.getInstancia().publicar(TipoEvento.PAGO_REGISTRADO, cajero, idCuenta,
                cuenta.getNumeroCuenta() + " · " + comprobante.getNumeroCompleto() + " · S/ "
                        + cuenta.getTotal().setScale(2) + " · " + metodo.getCodigo());
        return new ResultadoCobro(Map.of(), null, null, idComprobante, comprobante.getNumeroCompleto(), pago.getVuelto());
    }

    // ===================== RF-16: anular =====================

    /** @return null si se anuló; si no, el motivo */
    public String anular(int idCuenta, String motivo, Usuario usuario) {
        if (!puedeAnular(usuario)) {
            return "Solo un administrador puede anular cuentas.";
        }
        String m = motivo == null ? "" : motivo.trim();
        if (m.length() < MOTIVO_MINIMO || m.length() > 250) {
            return "Explica el motivo de la anulación (de " + MOTIVO_MINIMO + " a 250 caracteres).";
        }
        Optional<CuentaAtencionDTO> cuenta = cuentas.buscarPorId(idCuenta);
        if (cuenta.isEmpty()) {
            return "La cuenta no existe.";
        }
        try {
            pagos.anular(idCuenta, usuario.getIdUsuario(), m);
        } catch (OperacionRechazadaException e) {
            return e.getMessage();
        }
        EventoSubject.getInstancia().publicar(TipoEvento.CUENTA_ANULADA, usuario, idCuenta,
                cuenta.get().getNumeroCuenta() + (cuenta.get().getComprobante() == null ? "" : " · " + cuenta.get().getComprobante())
                        + " · Motivo: " + m);
        return null;
    }
}
