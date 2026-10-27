package controlador.caja;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import controlador.JsonUtil;
import controlador.FormatoVista;
import modelo.dto.ProductoServicioDTO;
import modelo.dto.ReportePagoDTO;
import modelo.servicios.BilleteraService;
import modelo.servicios.ReportePagoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.cobranza.pago.SolicitudPago;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DatosClienteDTO;
import modelo.dto.FiltroBusquedaDTO;
import modelo.entidades.Usuario;
import modelo.servicios.CobranzaService;
import modelo.servicios.PacienteService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador (MVC) de COBROS EN CAJA (PP-03) — paquete controlador.caja.
 * Ruta /caja/*: Recepción (caja) y Administrador (lo controla AutorizacionFilter).
 *
 *   GET  /caja/cuentas                      -> listado de cuentas (pendientes primero)
 *   GET  /caja/cuentas?accion=nueva&q=      -> buscar al paciente para abrirle una cuenta
 *   GET  /caja/cuentas?accion=ver&id=       -> cabecera + detalle + cobro
 *   POST accion=abrir | agregar | cantidad | quitar | cobrar | anular   (Post/Redirect/Get)
 */
@WebServlet(name = "CuentaAtencionController", urlPatterns = {"/caja/cuentas"})
public class CuentaAtencionController extends HttpServlet {

    private static final String RUTA = "/caja/cuentas";
    private static final String VISTA_LISTA = "/WEB-INF/vistas/caja/cuentas/listar.jsp";
    private static final String VISTA_NUEVA = "/WEB-INF/vistas/caja/cuentas/nueva.jsp";
    private static final String VISTA_DETALLE = "/WEB-INF/vistas/caja/cuentas/detalle.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("menuActivo", "cobros");
        CobranzaService servicio = new CobranzaService();
        String accion = request.getParameter("accion");
        try {
            if ("nueva".equals(accion)) {
                String q = request.getParameter("q");
                if (q != null && !q.isBlank()) {
                    request.setAttribute("pacientes", new PacienteService().listar(
                            new FiltroBusquedaDTO(q, "ACTIVO", null, null, 1)));
                }
                request.setAttribute("tituloPagina", "Nueva cuenta");
                request.getRequestDispatcher(VISTA_NUEVA).forward(request, response);
                return;
            }
            if ("items".equals(accion)) {
                responderItems(response, servicio.buscarItems(request.getParameter("q")));
                return;
            }
            if ("ver".equals(accion)) {
                Optional<CuentaAtencionDTO> cuenta = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (cuenta.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "La cuenta que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                mostrarDetalle(request, response, servicio, cuenta.get());
                return;
            }
            boolean soloPorCobrar = !ControladorUtil.busquedaSolicitada(request);
            FiltroBusquedaDTO filtro = soloPorCobrar
                    ? new FiltroBusquedaDTO(null, CuentaAtencionDTO.PENDIENTE, null, null, 1)
                    : new FiltroBusquedaDTO(request.getParameter("q"), request.getParameter("estado"), null, null,
                            ControladorUtil.entero(request.getParameter("pagina"), 1))
                            .conRango(ControladorUtil.rango(request, "estado"));
            request.setAttribute("soloPorCobrar", soloPorCobrar);
            request.setAttribute("filtro", filtro);
            request.setAttribute("pagina", servicio.listar(filtro));
            request.setAttribute("pendientes", servicio.contarPendientes());
            request.setAttribute("reportes", new ReportePagoService().pendientes());
        } catch (RuntimeException e) {
            System.err.println("CuentaAtencionController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", "Cobros");
        request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
    }

    private void mostrarDetalle(HttpServletRequest request, HttpServletResponse response, CobranzaService servicio,
                                CuentaAtencionDTO cuenta) throws ServletException, IOException {
        request.setAttribute("cuenta", cuenta);
        request.setAttribute("tarifa", CobranzaService.describirTarifa(cuenta));
        request.setAttribute("parametros", servicio.parametros());
        request.setAttribute("puedeAnular", CobranzaService.puedeAnular(usuario(request)));
        if (cuenta.isPendiente()) {
            request.setAttribute("frecuentes", servicio.itemsFrecuentes());
            request.setAttribute("metodos", servicio.metodosPago());
            request.setAttribute("billeteras", servicio.billeteras());
            request.setAttribute("configBilletera", new BilleteraService().configuracion());
            new ReportePagoService().ultimoDeCuenta(cuenta.getIdCuentaAtencion())
                    .filter(ReportePagoDTO::isPendiente).ifPresent(r -> request.setAttribute("reporte", r));
        }
        request.setAttribute("tituloPagina", "Cuenta " + cuenta.getNumeroCuenta());
        request.getRequestDispatcher(VISTA_DETALLE).forward(request, response);
    }

    /** Datos de una petición POST de caja, para que cada acción sea un método corto. */
    private record Peticion(HttpServletRequest request, HttpServletResponse response, HttpSession session, Usuario usuario,
                            CobranzaService servicio, int id, String verCuenta) {
        String lista() { return request.getContextPath() + RUTA; }
        String parametro(String nombre) { return request.getParameter(nombre); }
        int entero(String nombre, int porDefecto) { return ControladorUtil.entero(request.getParameter(nombre), porDefecto); }
    }

    @FunctionalInterface
    private interface Accion {
        String ejecutar(Peticion p) throws ServletException, IOException;
    }

    private final Map<String, Accion> acciones = Map.of(
            "abrir", this::abrir,
            "agregar", p -> resultado(p, p.servicio().agregarItem(p.id(), p.entero("idItem", 0), p.entero("cantidad", 1), p.usuario())),
            "cantidad", p -> resultado(p, p.servicio().cambiarCantidad(p.id(), p.entero("idItem", 0), p.entero("cantidad", -1), p.usuario())),
            "quitar", p -> resultado(p, p.servicio().cambiarCantidad(p.id(), p.entero("idItem", 0), 0, p.usuario())),
            "cobrar", this::cobrar,
            "verificarReporte", this::verificarReporte,
            "rechazarReporte", this::rechazarReporte,
            "anular", this::anular);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        Peticion p = new Peticion(request, response, request.getSession(), usuario(request), new CobranzaService(), id,
                request.getContextPath() + RUTA + "?accion=ver&id=" + id);
        Accion accion = acciones.get(String.valueOf(request.getParameter("accion")));
        String destino;
        try {
            destino = accion == null ? desconocida(p) : accion.ejecutar(p);
        } catch (RuntimeException e) {
            System.err.println("CuentaAtencionController: " + e.getMessage());
            MensajeFlash.error(p.session(), "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
            destino = p.verCuenta();
        }
        if (destino != null) {
            response.sendRedirect(destino);
        }
    }

    private static String desconocida(Peticion p) {
        MensajeFlash.error(p.session(), "Acción no reconocida.");
        return p.lista();
    }

    private static String resultado(Peticion p, String error) {
        error(p.session(), error);
        return p.verCuenta();
    }

    private String abrir(Peticion p) {
        CobranzaService.Apertura a = p.servicio().abrirCuenta(p.entero("idPaciente", 0), p.parametro("observacion"), p.usuario());
        if (!a.isExito()) {
            MensajeFlash.error(p.session(), a.error());
            return p.lista() + "?accion=nueva";
        }
        if (a.yaExistia()) {
            MensajeFlash.aviso(p.session(), "Este paciente ya tenía una cuenta pendiente: continúa con ella.");
        }
        return p.lista() + "?accion=ver&id=" + a.idCuenta();
    }

    private String cobrar(Peticion p) throws ServletException, IOException {
        SolicitudPago solicitud = leerSolicitud(p.request());
        DatosClienteDTO cliente = leerCliente(p.request());
        CobranzaService.ResultadoCobro r = p.servicio().cobrar(p.id(), p.parametro("metodo"), solicitud, cliente, p.usuario());
        Optional<CuentaAtencionDTO> cuenta = r.errores().isEmpty() ? Optional.empty() : p.servicio().obtener(p.id());
        if (cuenta.isPresent()) {
            HttpServletRequest request = p.request();
            request.setAttribute("errores", r.errores());
            request.setAttribute("solicitud", solicitud);
            request.setAttribute("cliente", cliente);
            request.setAttribute("metodoElegido", p.parametro("metodo"));
            request.setAttribute("menuActivo", "cobros");
            mostrarDetalle(request, p.response(), p.servicio(), cuenta.get());
            return null;
        }
        if (r.rechazo() != null) {
            MensajeFlash.error(p.session(), "Pago NO aprobado: " + r.rechazo() + " La cuenta sigue pendiente.");
            return p.verCuenta();
        }
        if (r.error() != null) {
            MensajeFlash.error(p.session(), r.error());
            return p.verCuenta();
        }
        String vuelto = r.vuelto() != null && r.vuelto().signum() > 0
                ? " Entrega S/ " + r.vuelto().setScale(2).toPlainString() + " de vuelto." : "";
        MensajeFlash.exito(p.session(), "Cobro registrado. Comprobante " + r.comprobante() + "." + vuelto);
        return p.request().getContextPath() + "/caja/comprobante?id=" + r.idComprobante();
    }

    private String verificarReporte(Peticion p) {
        CobranzaService.ResultadoCobro r = new ReportePagoService().verificar(p.entero("idReporte", 0), p.usuario());
        if (r.isExito()) {
            MensajeFlash.exito(p.session(), "Pago confirmado. Se emitió la boleta " + r.comprobante() + ".");
            return p.request().getContextPath() + "/caja/comprobante?id=" + r.idComprobante();
        }
        String motivo = r.rechazo() != null ? "Pago NO aprobado: " + r.rechazo() : r.error();
        MensajeFlash.error(p.session(), motivo != null ? motivo : "Revisa los datos del pago informado.");
        return p.id() > 0 ? p.verCuenta() : p.lista();
    }

    private String rechazarReporte(Peticion p) {
        String error = new ReportePagoService().rechazar(p.entero("idReporte", 0), p.parametro("motivo"), p.usuario());
        ControladorUtil.resultado(p.session(), error, "Aviso de pago rechazado. El paciente verá el motivo en «Mis citas».");
        return p.id() > 0 ? p.verCuenta() : p.lista();
    }

    private String anular(Peticion p) {
        ControladorUtil.resultado(p.session(), p.servicio().anular(p.id(), p.parametro("motivo"), p.usuario()),
                "Cuenta anulada. Si estaba pagada, el pago y el comprobante quedaron anulados y el stock se devolvió.");
        return p.verCuenta();
    }

    private static void responderItems(HttpServletResponse response, java.util.List<ProductoServicioDTO> items) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        StringBuilder sb = new StringBuilder("[");
        for (ProductoServicioDTO it : items) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(it.getIdProductoServicio())
              .append(",\"codigo\":").append(JsonUtil.texto(it.getCodigo()))
              .append(",\"nombre\":").append(JsonUtil.texto(it.getNombre()))
              .append(",\"tipo\":").append(JsonUtil.texto(it.getTipo()))
              .append(",\"categoria\":").append(JsonUtil.texto(it.getNombreCategoria()))
              .append(",\"precio\":").append(JsonUtil.texto(FormatoVista.soles(it.getPrecio())))
              .append(",\"stock\":").append(it.getStock() == null ? "null" : it.getStock().toString())
              .append('}');
        }
        response.getWriter().write(sb.append(']').toString());
    }

    private static void error(HttpSession session, String mensaje) {
        if (mensaje != null) {
            MensajeFlash.error(session, mensaje);
        }
    }

    private static Usuario usuario(HttpServletRequest request) {
        return (Usuario) request.getSession().getAttribute("usuarioLogueado");
    }

    private static SolicitudPago leerSolicitud(HttpServletRequest request) {
        SolicitudPago s = new SolicitudPago();
        s.setMontoRecibido(ControladorUtil.decimal(request.getParameter("montoRecibido")));
        s.setTipoTarjeta(request.getParameter("tipoTarjeta"));
        s.setUltimosDigitos(request.getParameter("ultimosDigitos"));
        s.setCodigoAutorizacion(request.getParameter("codigoAutorizacion"));
        s.setIdBilletera(ControladorUtil.enteroONulo(request.getParameter("idBilletera")));
        s.setNombrePagador(request.getParameter("nombrePagador"));
        s.setNumeroOperacion(request.getParameter("numeroOperacion"));
        s.setCelular(request.getParameter("celular"));
        return s;
    }

    private static DatosClienteDTO leerCliente(HttpServletRequest request) {
        DatosClienteDTO c = new DatosClienteDTO();
        c.setTipoComprobante(request.getParameter("tipoComprobante"));
        boolean factura = c.isFactura();
        c.setNumeroDocumento(request.getParameter(factura ? "ruc" : "dni"));
        c.setNombre(request.getParameter(factura ? "razonSocial" : "nombreCliente"));
        c.setDireccion(request.getParameter(factura ? "direccionFiscal" : "direccionCliente"));
        return c;
    }
}
