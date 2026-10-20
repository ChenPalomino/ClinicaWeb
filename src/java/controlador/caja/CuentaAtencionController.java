package controlador.caja;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
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
            FiltroBusquedaDTO filtro = new FiltroBusquedaDTO(request.getParameter("q"), request.getParameter("estado"),
                    null, null, ControladorUtil.entero(request.getParameter("pagina"), 1)).conRango(ControladorUtil.rango(request));
            request.setAttribute("filtro", filtro);
            request.setAttribute("pagina", servicio.listar(filtro));
            request.setAttribute("pendientes", servicio.contarPendientes());
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
            request.setAttribute("items", servicio.itemsVendibles());
            request.setAttribute("metodos", servicio.metodosPago());
        }
        request.setAttribute("tituloPagina", "Cuenta " + cuenta.getNumeroCuenta());
        request.getRequestDispatcher(VISTA_DETALLE).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario usuario = usuario(request);
        CobranzaService servicio = new CobranzaService();
        String accion = request.getParameter("accion");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        int idItem = ControladorUtil.entero(request.getParameter("idItem"), 0);
        String verCuenta = request.getContextPath() + RUTA + "?accion=ver&id=" + id;
        String destino = verCuenta;
        try {
            switch (accion == null ? "" : accion) {
                case "abrir": {
                    CobranzaService.Apertura a = servicio.abrirCuenta(ControladorUtil.entero(request.getParameter("idPaciente"), 0),
                            request.getParameter("observacion"), usuario);
                    if (!a.isExito()) {
                        MensajeFlash.error(session, a.error());
                        destino = request.getContextPath() + RUTA + "?accion=nueva";
                    } else {
                        if (a.yaExistia()) {
                            MensajeFlash.aviso(session, "Este paciente ya tenía una cuenta pendiente: continúa con ella.");
                        }
                        destino = request.getContextPath() + RUTA + "?accion=ver&id=" + a.idCuenta();
                    }
                    break;
                }
                case "agregar":
                    error(session, servicio.agregarItem(id, idItem, ControladorUtil.entero(request.getParameter("cantidad"), 1), usuario));
                    break;
                case "cantidad":
                    error(session, servicio.cambiarCantidad(id, idItem, ControladorUtil.entero(request.getParameter("cantidad"), -1), usuario));
                    break;
                case "quitar":
                    error(session, servicio.cambiarCantidad(id, idItem, 0, usuario));
                    break;
                case "cobrar": {
                    SolicitudPago solicitud = leerSolicitud(request);
                    DatosClienteDTO cliente = leerCliente(request);
                    CobranzaService.ResultadoCobro r = servicio.cobrar(id, request.getParameter("metodo"), solicitud, cliente, usuario);
                    if (!r.errores().isEmpty()) {
                        Optional<CuentaAtencionDTO> cuenta = servicio.obtener(id);
                        if (cuenta.isPresent()) {
                            request.setAttribute("errores", r.errores());
                            request.setAttribute("solicitud", solicitud);
                            request.setAttribute("cliente", cliente);
                            request.setAttribute("metodoElegido", request.getParameter("metodo"));
                            request.setAttribute("menuActivo", "cobros");
                            mostrarDetalle(request, response, servicio, cuenta.get());
                            return;
                        }
                    }
                    if (r.rechazo() != null) {
                        MensajeFlash.error(session, "Pago NO aprobado: " + r.rechazo() + " La cuenta sigue pendiente.");
                    } else if (r.error() != null) {
                        MensajeFlash.error(session, r.error());
                    } else {
                        String vuelto = r.vuelto() != null && r.vuelto().signum() > 0
                                ? " Entrega S/ " + r.vuelto().setScale(2).toPlainString() + " de vuelto." : "";
                        MensajeFlash.exito(session, "Cobro registrado. Comprobante " + r.comprobante() + "." + vuelto);
                        destino = request.getContextPath() + "/caja/comprobante?id=" + r.idComprobante();
                    }
                    break;
                }
                case "anular":
                    ControladorUtil.resultado(session, servicio.anular(id, request.getParameter("motivo"), usuario),
                            "Cuenta anulada. Si estaba pagada, el pago y el comprobante quedaron anulados y el stock se devolvió.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
                    destino = request.getContextPath() + RUTA;
            }
        } catch (RuntimeException e) {
            System.err.println("CuentaAtencionController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(destino);
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
        s.setAplicacion(request.getParameter("aplicacion"));
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
