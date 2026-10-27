package base;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * VERSIÓN BASE (pretest del diseño preexperimental): cobranza en caja escrita en estilo
 * procedimental. Tarifas, medios de pago y comprobantes se resuelven con if/else dentro del
 * servlet y el SQL va en línea. Solo se usa para medir; no se despliega.
 */
@WebServlet("/base/caja")
public class CajaServletBase extends HttpServlet {

    private Connection conectar() throws SQLException {
        String url = System.getenv("DB_URL");
        String usuario = System.getenv("DB_USER");
        String clave = System.getenv("DB_PASSWORD");
        if (url == null) {
            url = "jdbc:postgresql://localhost:5432/clinica_db";
        }
        if (usuario == null) {
            usuario = "postgres";
        }
        if (clave == null) {
            clave = "";
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp");
            return;
        }
        int rol = (Integer) session.getAttribute("idRol");
        if (rol != 1 && rol != 2) {
            response.sendRedirect(request.getContextPath() + "/panel");
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String id = request.getParameter("id");
        out.println("<html><body><h1>Caja</h1>");
        try (Connection cn = conectar()) {
            if (id == null) {
                String estado = request.getParameter("estado");
                String sql = "SELECT ca.id_cuenta_atencion, ca.numero_cuenta, ca.estado_cuenta, ca.op_gravada + ca.op_exonerada + ca.igv AS total, "
                        + "dp.nombres || ' ' || dp.apellidos AS paciente FROM tt_cuenta_atencion ca JOIN tt_paciente p ON p.id_paciente = ca.id_paciente "
                        + "JOIN tt_datos_personales dp ON dp.id_datos_personales = p.id_datos_personales";
                if (estado != null && !estado.isEmpty()) {
                    sql += " WHERE ca.estado_cuenta = ?";
                }
                sql += " ORDER BY ca.id_cuenta_atencion DESC LIMIT 100";
                PreparedStatement ps = cn.prepareStatement(sql);
                if (estado != null && !estado.isEmpty()) {
                    ps.setString(1, estado);
                }
                ResultSet rs = ps.executeQuery();
                out.println("<table border='1'><tr><th>Cuenta</th><th>Paciente</th><th>Total</th><th>Estado</th></tr>");
                while (rs.next()) {
                    String est = rs.getString("estado_cuenta");
                    String estadoTexto = est.equals("PENDIENTE") ? "Por cobrar" : est.equals("PAGADA") ? "Pagada" : "Anulada";
                    out.println("<tr><td><a href='?id=" + rs.getInt(1) + "'>" + rs.getString(2) + "</a></td><td>"
                            + rs.getString("paciente").replace("<", "&lt;").replace(">", "&gt;") + "</td><td>S/ "
                            + rs.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP) + "</td><td>" + estadoTexto + "</td></tr>");
                }
                out.println("</table>");
            } else {
                PreparedStatement pc = cn.prepareStatement("SELECT ca.*, cb.tipo, cb.porcentaje_cobertura, cb.nombre AS cobertura FROM tt_cuenta_atencion ca "
                        + "JOIN tm_cobertura cb ON cb.id_cobertura = ca.id_cobertura WHERE ca.id_cuenta_atencion = ?");
                pc.setInt(1, Integer.parseInt(id));
                ResultSet rc = pc.executeQuery();
                if (!rc.next()) {
                    out.println("<p>La cuenta no existe.</p>");
                } else {
                    String tipo = rc.getString("tipo");
                    if (tipo.equals("PARTICULAR")) {
                        out.println("<p>Tarifa particular: paga el precio de lista.</p>");
                    } else if (tipo.equals("EPS")) {
                        out.println("<p>EPS: la aseguradora cubre el " + rc.getBigDecimal("porcentaje_cobertura") + " %.</p>");
                    } else if (tipo.equals("SEGURO")) {
                        out.println("<p>Seguro: cubre el " + rc.getBigDecimal("porcentaje_cobertura") + " % de los servicios (no productos).</p>");
                    } else {
                        out.println("<p>Convenio: descuento del " + rc.getBigDecimal("porcentaje_cobertura") + " %.</p>");
                    }
                    PreparedStatement pd = cn.prepareStatement("SELECT * FROM tt_detalle_cuenta WHERE id_cuenta_atencion = ?");
                    pd.setInt(1, Integer.parseInt(id));
                    ResultSet rd = pd.executeQuery();
                    out.println("<table border='1'>");
                    while (rd.next()) {
                        BigDecimal importe = rd.getBigDecimal("precio_unitario").multiply(BigDecimal.valueOf(rd.getInt("cantidad")))
                                .subtract(rd.getBigDecimal("monto_aseguradora")).subtract(rd.getBigDecimal("monto_descuento"));
                        out.println("<tr><td>" + rd.getString("descripcion") + "</td><td>" + rd.getInt("cantidad") + "</td><td>S/ " + importe + "</td></tr>");
                    }
                    out.println("</table>");
                    BigDecimal total = rc.getBigDecimal("op_gravada").add(rc.getBigDecimal("op_exonerada")).add(rc.getBigDecimal("igv"));
                    out.println("<p>Total: S/ " + total + "</p>");
                    if (rc.getString("estado_cuenta").equals("PENDIENTE")) {
                        out.println("<form method='post'><input type='hidden' name='accion' value='cobrar'><input type='hidden' name='id' value='" + id + "'>"
                                + "<select name='metodo'><option>EFECTIVO</option><option>TARJETA</option><option>BILLETERA</option></select>"
                                + "<input name='montoRecibido'><input name='tipoTarjeta'><input name='ultimos'><input name='autorizacion'>"
                                + "<input name='aplicacion'><input name='operacion'><input name='celular'>"
                                + "<select name='comprobante'><option>BOLETA</option><option>FACTURA</option></select>"
                                + "<input name='documento'><input name='nombre'><input name='direccion'><button>Cobrar</button></form>");
                    }
                }
            }
        } catch (SQLException e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp");
            return;
        }
        int rol = (Integer) session.getAttribute("idRol");
        int idUsuario = (Integer) session.getAttribute("idUsuario");
        String usuario = (String) session.getAttribute("nombreUsuario");
        String accion = request.getParameter("accion");
        String volver = request.getContextPath() + "/base/caja";
        String mensaje = null;
        if (rol != 1 && rol != 2) {
            response.sendRedirect(request.getContextPath() + "/panel");
            return;
        }
        try (Connection cn = conectar()) {
            if ("abrir".equals(accion) || "abrirDesdeCita".equals(accion)) {
                int idPaciente;
                int idCita = 0;
                if ("abrirDesdeCita".equals(accion)) {
                    idCita = Integer.parseInt(request.getParameter("idCita"));
                    PreparedStatement pc = cn.prepareStatement("SELECT id_paciente, id_especialidad, estado_cita, id_cuenta_atencion FROM tt_cita WHERE id_cita = ?");
                    pc.setInt(1, idCita);
                    ResultSet rc = pc.executeQuery();
                    if (!rc.next()) {
                        session.setAttribute("mensaje", "La cita no existe.");
                        response.sendRedirect(volver);
                        return;
                    }
                    if (rc.getString("estado_cita").equals("CANCELADA") || rc.getString("estado_cita").equals("NO_ASISTIO")) {
                        session.setAttribute("mensaje", "Una cita cancelada no se cobra.");
                        response.sendRedirect(volver);
                        return;
                    }
                    if (rc.getObject("id_cuenta_atencion") != null) {
                        response.sendRedirect(volver + "?id=" + rc.getInt("id_cuenta_atencion"));
                        return;
                    }
                    idPaciente = rc.getInt("id_paciente");
                } else {
                    idPaciente = Integer.parseInt(request.getParameter("idPaciente"));
                }
                PreparedStatement pp = cn.prepareStatement("SELECT id_cuenta_atencion FROM tt_cuenta_atencion WHERE id_paciente = ? AND estado_cuenta = 'PENDIENTE'");
                pp.setInt(1, idPaciente);
                ResultSet rp = pp.executeQuery();
                int idCuenta;
                if (rp.next()) {
                    idCuenta = rp.getInt(1);
                    mensaje = "El paciente ya tenía una cuenta pendiente.";
                } else {
                    PreparedStatement pb = cn.prepareStatement("SELECT p.id_cobertura FROM tt_paciente p WHERE p.id_paciente = ? AND p.estado = 'Activo'");
                    pb.setInt(1, idPaciente);
                    ResultSet rb = pb.executeQuery();
                    if (!rb.next()) {
                        session.setAttribute("mensaje", "El paciente no existe o está desactivado.");
                        response.sendRedirect(volver);
                        return;
                    }
                    PreparedStatement pi = cn.prepareStatement("INSERT INTO tt_cuenta_atencion (numero_cuenta, id_paciente, id_cobertura, estado_cuenta, op_gravada, op_exonerada, igv, "
                            + "monto_aseguradora, monto_descuento, observacion, usuario_registro) VALUES ('CA-' || LPAD(nextval('tt_cuenta_atencion_id_cuenta_atencion_seq')::text, 8, '0'), "
                            + "?, ?, 'PENDIENTE', 0, 0, 0, 0, 0, ?, ?) RETURNING id_cuenta_atencion");
                    pi.setInt(1, idPaciente);
                    pi.setInt(2, rb.getInt(1));
                    pi.setString(3, request.getParameter("observacion") == null ? "" : request.getParameter("observacion"));
                    pi.setString(4, usuario);
                    ResultSet ri = pi.executeQuery();
                    ri.next();
                    idCuenta = ri.getInt(1);
                }
                if (idCita > 0) {
                    PreparedStatement pv = cn.prepareStatement("UPDATE tt_cita SET id_cuenta_atencion = ? WHERE id_cita = ?");
                    pv.setInt(1, idCuenta);
                    pv.setInt(2, idCita);
                    pv.executeUpdate();
                    PreparedStatement ps = cn.prepareStatement("SELECT ps.id_producto_servicio FROM vw_producto_servicio ps JOIN tt_cita c ON c.id_especialidad = ps.id_especialidad "
                            + "WHERE c.id_cita = ? AND ps.tipo = 'SERVICIO' AND ps.estado = 'Activo' LIMIT 1");
                    ps.setInt(1, idCita);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        request.setAttribute("idItemConsulta", rs.getInt(1));
                    }
                }
                volver += "?id=" + idCuenta;
                if (request.getAttribute("idItemConsulta") == null) {
                    session.setAttribute("mensaje", mensaje);
                    response.sendRedirect(volver);
                    return;
                }
                accion = "agregar";
                request.setAttribute("idCuentaNueva", idCuenta);
            }
            if ("agregar".equals(accion) || "cantidad".equals(accion) || "quitar".equals(accion)) {
                int idCuenta = request.getAttribute("idCuentaNueva") != null ? (Integer) request.getAttribute("idCuentaNueva")
                        : Integer.parseInt(request.getParameter("id"));
                int idItem = request.getAttribute("idItemConsulta") != null ? (Integer) request.getAttribute("idItemConsulta")
                        : Integer.parseInt(request.getParameter("idItem"));
                int cantidad;
                if ("quitar".equals(accion)) {
                    cantidad = 0;
                } else {
                    try {
                        cantidad = request.getParameter("cantidad") == null ? 1 : Integer.parseInt(request.getParameter("cantidad"));
                    } catch (NumberFormatException e) {
                        cantidad = -1;
                    }
                }
                volver = request.getContextPath() + "/base/caja?id=" + idCuenta;
                PreparedStatement pc = cn.prepareStatement("SELECT ca.estado_cuenta, cb.tipo, cb.porcentaje_cobertura FROM tt_cuenta_atencion ca "
                        + "JOIN tm_cobertura cb ON cb.id_cobertura = ca.id_cobertura WHERE ca.id_cuenta_atencion = ?");
                pc.setInt(1, idCuenta);
                ResultSet rc = pc.executeQuery();
                if (!rc.next()) {
                    mensaje = "La cuenta no existe.";
                } else if (!rc.getString("estado_cuenta").equals("PENDIENTE")) {
                    mensaje = "La cuenta ya no está pendiente.";
                } else if (cantidad < 0 || cantidad > 99) {
                    mensaje = "La cantidad debe estar entre 1 y 99.";
                } else {
                    String tipoCobertura = rc.getString("tipo");
                    BigDecimal porcentaje = rc.getBigDecimal("porcentaje_cobertura");
                    PreparedStatement pi = cn.prepareStatement("SELECT * FROM vw_producto_servicio WHERE id_producto_servicio = ? AND estado = 'Activo'");
                    pi.setInt(1, idItem);
                    ResultSet ri = pi.executeQuery();
                    boolean existeItem = ri.next();
                    PreparedStatement pe = cn.prepareStatement("SELECT cantidad FROM tt_detalle_cuenta WHERE id_cuenta_atencion = ? AND id_producto_servicio = ?");
                    pe.setInt(1, idCuenta);
                    pe.setInt(2, idItem);
                    ResultSet re = pe.executeQuery();
                    boolean yaEsta = re.next();
                    int nueva = cantidad;
                    if ("agregar".equals(accion) && yaEsta) {
                        nueva = cantidad + re.getInt(1);
                    }
                    if (cantidad == 0 && !yaEsta) {
                        mensaje = "Ese servicio o producto no está en la cuenta.";
                    } else if (cantidad > 0 && !existeItem) {
                        mensaje = "Ese servicio o producto no existe.";
                    } else if (nueva > 99) {
                        mensaje = "No se pueden agregar más de 99 unidades.";
                    } else if (cantidad > 0 && ri.getObject("stock") != null && nueva > ri.getInt("stock")) {
                        mensaje = "Solo quedan " + ri.getInt("stock") + " unidades.";
                    } else {
                        cn.setAutoCommit(false);
                        try {
                            if (cantidad == 0) {
                                PreparedStatement pq = cn.prepareStatement("DELETE FROM tt_detalle_cuenta WHERE id_cuenta_atencion = ? AND id_producto_servicio = ?");
                                pq.setInt(1, idCuenta);
                                pq.setInt(2, idItem);
                                pq.executeUpdate();
                            } else if (yaEsta) {
                                PreparedStatement pq = cn.prepareStatement("UPDATE tt_detalle_cuenta SET cantidad = ? WHERE id_cuenta_atencion = ? AND id_producto_servicio = ?");
                                pq.setInt(1, nueva);
                                pq.setInt(2, idCuenta);
                                pq.setInt(3, idItem);
                                pq.executeUpdate();
                            } else {
                                PreparedStatement pq = cn.prepareStatement("INSERT INTO tt_detalle_cuenta (id_cuenta_atencion, id_producto_servicio, descripcion, cantidad, "
                                        + "precio_unitario, afecto_igv, porcentaje_cubierto, monto_aseguradora, monto_descuento, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, ?)");
                                pq.setInt(1, idCuenta);
                                pq.setInt(2, idItem);
                                pq.setString(3, ri.getString("nombre"));
                                pq.setInt(4, nueva);
                                pq.setBigDecimal(5, ri.getBigDecimal("precio"));
                                pq.setBoolean(6, ri.getBoolean("afecto_igv"));
                                pq.setString(7, usuario);
                                pq.executeUpdate();
                            }
                            PreparedStatement pl = cn.prepareStatement("SELECT d.id_detalle_cuenta, d.cantidad, d.precio_unitario, d.afecto_igv, ps.tipo FROM tt_detalle_cuenta d "
                                    + "JOIN vw_producto_servicio ps ON ps.id_producto_servicio = d.id_producto_servicio WHERE d.id_cuenta_atencion = ?");
                            pl.setInt(1, idCuenta);
                            ResultSet rl = pl.executeQuery();
                            BigDecimal gravado = BigDecimal.ZERO;
                            BigDecimal exonerado = BigDecimal.ZERO;
                            BigDecimal aseguradora = BigDecimal.ZERO;
                            BigDecimal descuento = BigDecimal.ZERO;
                            PreparedStatement pu = cn.prepareStatement("UPDATE tt_detalle_cuenta SET porcentaje_cubierto = ?, monto_aseguradora = ?, monto_descuento = ? WHERE id_detalle_cuenta = ?");
                            while (rl.next()) {
                                BigDecimal bruto = rl.getBigDecimal("precio_unitario").multiply(BigDecimal.valueOf(rl.getInt("cantidad")));
                                BigDecimal pct = BigDecimal.ZERO;
                                BigDecimal cubre = BigDecimal.ZERO;
                                BigDecimal desc = BigDecimal.ZERO;
                                if (tipoCobertura.equals("PARTICULAR")) {
                                    pct = BigDecimal.ZERO;
                                } else if (tipoCobertura.equals("EPS")) {
                                    pct = porcentaje;
                                    cubre = bruto.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                } else if (tipoCobertura.equals("SEGURO")) {
                                    if (rl.getString("tipo").equals("SERVICIO")) {
                                        pct = porcentaje;
                                        cubre = bruto.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                    }
                                } else if (tipoCobertura.equals("CONVENIO")) {
                                    pct = porcentaje;
                                    desc = bruto.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                }
                                BigDecimal importe = bruto.subtract(cubre).subtract(desc);
                                if (rl.getBoolean("afecto_igv")) {
                                    gravado = gravado.add(importe);
                                } else {
                                    exonerado = exonerado.add(importe);
                                }
                                aseguradora = aseguradora.add(cubre);
                                descuento = descuento.add(desc);
                                pu.setBigDecimal(1, pct);
                                pu.setBigDecimal(2, cubre);
                                pu.setBigDecimal(3, desc);
                                pu.setInt(4, rl.getInt("id_detalle_cuenta"));
                                pu.executeUpdate();
                            }
                            PreparedStatement pg = cn.prepareStatement("SELECT valor FROM tt_parametro_clinica WHERE id_parametro_clinica = 1");
                            ResultSet rg = pg.executeQuery();
                            BigDecimal igvPct = rg.next() ? rg.getBigDecimal(1) : new BigDecimal("18");
                            BigDecimal base = gravado.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(100).add(igvPct), 2, RoundingMode.HALF_UP);
                            BigDecimal igv = gravado.subtract(base);
                            PreparedStatement pt = cn.prepareStatement("UPDATE tt_cuenta_atencion SET op_gravada = ?, op_exonerada = ?, igv = ?, monto_aseguradora = ?, "
                                    + "monto_descuento = ?, usuario_registro = ? WHERE id_cuenta_atencion = ? AND estado_cuenta = 'PENDIENTE'");
                            pt.setBigDecimal(1, base);
                            pt.setBigDecimal(2, exonerado);
                            pt.setBigDecimal(3, igv);
                            pt.setBigDecimal(4, aseguradora);
                            pt.setBigDecimal(5, descuento);
                            pt.setString(6, usuario);
                            pt.setInt(7, idCuenta);
                            if (pt.executeUpdate() == 0) {
                                cn.rollback();
                                mensaje = "La cuenta ya no está pendiente.";
                            } else {
                                cn.commit();
                            }
                        } catch (SQLException e) {
                            cn.rollback();
                            mensaje = "No se pudo actualizar la cuenta: " + e.getMessage();
                        } finally {
                            cn.setAutoCommit(true);
                        }
                    }
                }
            } else if ("cobrar".equals(accion)) {
                int idCuenta = Integer.parseInt(request.getParameter("id"));
                volver = request.getContextPath() + "/base/caja?id=" + idCuenta;
                String metodo = request.getParameter("metodo");
                String comprobante = request.getParameter("comprobante");
                String documento = request.getParameter("documento") == null ? "" : request.getParameter("documento").trim();
                String nombre = request.getParameter("nombre") == null ? "" : request.getParameter("nombre").trim();
                String direccion = request.getParameter("direccion") == null ? "" : request.getParameter("direccion").trim();
                PreparedStatement pc = cn.prepareStatement("SELECT * FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ? FOR UPDATE");
                cn.setAutoCommit(false);
                try {
                    pc.setInt(1, idCuenta);
                    ResultSet rc = pc.executeQuery();
                    if (!rc.next()) {
                        mensaje = "La cuenta no existe.";
                    } else if (!rc.getString("estado_cuenta").equals("PENDIENTE")) {
                        mensaje = "La cuenta ya fue pagada o anulada.";
                    } else {
                        BigDecimal total = rc.getBigDecimal("op_gravada").add(rc.getBigDecimal("op_exonerada")).add(rc.getBigDecimal("igv"));
                        if (total.signum() <= 0) {
                            mensaje = "Agrega al menos un servicio o producto antes de cobrar.";
                        }
                        String referencia = null;
                        BigDecimal recibido = null;
                        BigDecimal vuelto = null;
                        boolean aprobado = true;
                        String motivoRechazo = null;
                        if (mensaje == null) {
                            if ("EFECTIVO".equals(metodo)) {
                                try {
                                    recibido = new BigDecimal(request.getParameter("montoRecibido").replace(",", "."));
                                } catch (Exception e) {
                                    recibido = null;
                                }
                                if (recibido == null) {
                                    mensaje = "Escribe con cuánto paga el paciente.";
                                } else if (recibido.scale() > 2) {
                                    mensaje = "Usa como máximo 2 decimales.";
                                } else if (recibido.compareTo(new BigDecimal("5000")) > 0) {
                                    mensaje = "El monto recibido no puede pasar de S/ 5000.";
                                } else if (recibido.compareTo(total) < 0) {
                                    mensaje = "Faltan S/ " + total.subtract(recibido) + ".";
                                } else {
                                    vuelto = recibido.subtract(total);
                                    referencia = "Efectivo";
                                }
                            } else if ("TARJETA".equals(metodo)) {
                                String tipoTarjeta = request.getParameter("tipoTarjeta") == null ? "" : request.getParameter("tipoTarjeta").toUpperCase();
                                String ultimos = request.getParameter("ultimos") == null ? "" : request.getParameter("ultimos").trim();
                                String autorizacion = request.getParameter("autorizacion") == null ? "" : request.getParameter("autorizacion").trim();
                                if (!tipoTarjeta.equals("DEBITO") && !tipoTarjeta.equals("CREDITO")) {
                                    mensaje = "Elige débito o crédito.";
                                } else if (!ultimos.matches("\\d{4}")) {
                                    mensaje = "Escribe los últimos 4 dígitos.";
                                } else if (!autorizacion.matches("\\d{6}")) {
                                    mensaje = "El código de autorización tiene 6 dígitos.";
                                } else {
                                    referencia = (tipoTarjeta.equals("DEBITO") ? "Débito" : "Crédito") + " **** " + ultimos + " · Aut. " + autorizacion;
                                    if (autorizacion.equals("000000")) {
                                        aprobado = false;
                                        motivoRechazo = "El banco rechazó la tarjeta **** " + ultimos + ".";
                                    }
                                }
                            } else if ("BILLETERA".equals(metodo)) {
                                String aplicacion = request.getParameter("aplicacion") == null ? "" : request.getParameter("aplicacion").trim();
                                String operacion = request.getParameter("operacion") == null ? "" : request.getParameter("operacion").replaceAll("[\\s-]", "");
                                String celular = request.getParameter("celular") == null ? "" : request.getParameter("celular").replace(" ", "");
                                if (!aplicacion.equals("YAPE") && !aplicacion.equals("PLIN")) {
                                    mensaje = "Elige Yape o Plin.";
                                } else if (operacion.isEmpty() && celular.isEmpty()) {
                                    mensaje = "Escribe el número de operación o el celular.";
                                } else if (!operacion.isEmpty() && !operacion.matches("\\d{3,20}")) {
                                    mensaje = "El número de operación solo tiene dígitos.";
                                } else if (!celular.isEmpty() && !celular.matches("9\\d{8}")) {
                                    mensaje = "El celular tiene 9 dígitos y empieza con 9.";
                                } else {
                                    referencia = aplicacion + (operacion.isEmpty() ? "" : " · Op. " + operacion)
                                            + (celular.isEmpty() ? "" : " · Cel. ***" + celular.substring(6));
                                    if (!operacion.isEmpty() && operacion.matches("0+")) {
                                        aprobado = false;
                                        motivoRechazo = "La operación no aparece en la app de la clínica.";
                                    }
                                }
                            } else {
                                mensaje = "Elige cómo paga el paciente.";
                            }
                        }
                        String serie = null;
                        String tipoDocumento = null;
                        if (mensaje == null && aprobado) {
                            if ("FACTURA".equals(comprobante)) {
                                boolean rucValido = documento.matches("(10|15|17|20)\\d{9}");
                                if (rucValido) {
                                    int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
                                    int suma = 0;
                                    for (int i = 0; i < 10; i++) {
                                        suma += (documento.charAt(i) - '0') * pesos[i];
                                    }
                                    int resto = 11 - (suma % 11);
                                    int verificador = resto == 10 ? 0 : (resto == 11 ? 1 : resto);
                                    rucValido = verificador == documento.charAt(10) - '0';
                                }
                                if (!rucValido) {
                                    mensaje = "El RUC no es válido.";
                                } else if (nombre.length() < 3) {
                                    mensaje = "Escribe la razón social.";
                                } else if (direccion.length() < 5) {
                                    mensaje = "Escribe la dirección fiscal.";
                                } else {
                                    serie = "F001";
                                    tipoDocumento = "RUC";
                                }
                            } else {
                                PreparedStatement pm = cn.prepareStatement("SELECT monto_boleta_dni FROM tt_parametro_clinica WHERE id_parametro_clinica = 1");
                                ResultSet rm = pm.executeQuery();
                                BigDecimal limite = rm.next() ? rm.getBigDecimal(1) : new BigDecimal("700");
                                if (!documento.isEmpty() && !documento.matches("\\d{8}")) {
                                    mensaje = "El DNI tiene 8 dígitos.";
                                } else if (documento.isEmpty() && total.compareTo(limite) >= 0) {
                                    mensaje = "Desde S/ " + limite + " la boleta exige DNI.";
                                } else if (nombre.isEmpty() && !documento.isEmpty()) {
                                    mensaje = "Escribe el nombre del cliente.";
                                } else {
                                    serie = "B001";
                                    tipoDocumento = documento.isEmpty() ? "-" : "DNI";
                                }
                            }
                        }
                        if (mensaje == null) {
                            PreparedStatement pmet = cn.prepareStatement("SELECT id_metodo_pago FROM tm_metodo_pago WHERE codigo = ?");
                            pmet.setString(1, metodo);
                            ResultSet rmet = pmet.executeQuery();
                            rmet.next();
                            PreparedStatement pp = cn.prepareStatement("INSERT INTO tt_pago (id_cuenta_atencion, id_metodo_pago, monto, estado_pago, motivo_rechazo, "
                                    + "id_usuario_cajero, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_pago");
                            pp.setInt(1, idCuenta);
                            pp.setInt(2, rmet.getInt(1));
                            pp.setBigDecimal(3, total);
                            pp.setString(4, aprobado ? "APROBADO" : "RECHAZADO");
                            pp.setString(5, motivoRechazo);
                            pp.setInt(6, idUsuario);
                            pp.setString(7, usuario);
                            ResultSet rp = pp.executeQuery();
                            rp.next();
                            int idPago = rp.getInt(1);
                            if (!aprobado) {
                                cn.commit();
                                mensaje = "Pago NO aprobado: " + motivoRechazo;
                            } else {
                                PreparedStatement ps = cn.prepareStatement("UPDATE tm_serie_comprobante SET ultimo_numero = ultimo_numero + 1 WHERE serie = ? RETURNING id_serie_comprobante, ultimo_numero");
                                ps.setString(1, serie);
                                ResultSet rs = ps.executeQuery();
                                rs.next();
                                int numero = rs.getInt(2);
                                PreparedStatement pk = cn.prepareStatement("INSERT INTO tt_comprobante (id_pago, id_serie_comprobante, numero, tipo_documento, numero_documento, "
                                        + "nombre_cliente, direccion_cliente, estado_comprobante, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?, 'EMITIDO', ?)");
                                pk.setInt(1, idPago);
                                pk.setInt(2, rs.getInt(1));
                                pk.setInt(3, numero);
                                pk.setString(4, tipoDocumento);
                                pk.setString(5, documento.isEmpty() ? "-" : documento);
                                pk.setString(6, nombre.isEmpty() ? "Cliente varios" : nombre);
                                pk.setString(7, direccion);
                                pk.setString(8, usuario);
                                pk.executeUpdate();
                                PreparedStatement pst = cn.prepareStatement("UPDATE tt_producto p SET stock = p.stock - d.cantidad FROM tt_detalle_cuenta d "
                                        + "WHERE d.id_cuenta_atencion = ? AND d.id_producto_servicio = p.id_producto_servicio");
                                pst.setInt(1, idCuenta);
                                pst.executeUpdate();
                                PreparedStatement pu = cn.prepareStatement("UPDATE tt_cuenta_atencion SET estado_cuenta = 'PAGADA', fecha_pago = NOW() WHERE id_cuenta_atencion = ?");
                                pu.setInt(1, idCuenta);
                                pu.executeUpdate();
                                PreparedStatement pl = cn.prepareStatement("INSERT INTO tt_auditoria (accion, id_usuario, id_entidad, detalle, usuario_registro) VALUES ('PAGO_REGISTRADO', ?, ?, ?, ?)");
                                pl.setInt(1, idUsuario);
                                pl.setInt(2, idCuenta);
                                pl.setString(3, referencia + " · " + serie + "-" + String.format("%08d", numero));
                                pl.setString(4, usuario);
                                pl.executeUpdate();
                                cn.commit();
                                mensaje = "Cobro registrado. Comprobante " + serie + "-" + String.format("%08d", numero)
                                        + (vuelto != null && vuelto.signum() > 0 ? ". Vuelto S/ " + vuelto : "");
                            }
                        } else {
                            cn.rollback();
                        }
                    }
                } catch (SQLException e) {
                    cn.rollback();
                    mensaje = "No se pudo cobrar: " + e.getMessage();
                } finally {
                    cn.setAutoCommit(true);
                }
            } else if ("anular".equals(accion)) {
                int idCuenta = Integer.parseInt(request.getParameter("id"));
                String motivo = request.getParameter("motivo") == null ? "" : request.getParameter("motivo").trim();
                volver = request.getContextPath() + "/base/caja?id=" + idCuenta;
                if (rol != 1) {
                    mensaje = "Solo un administrador puede anular cuentas.";
                } else if (motivo.length() < 10 || motivo.length() > 250) {
                    mensaje = "Explica el motivo de la anulación (de 10 a 250 caracteres).";
                } else {
                    cn.setAutoCommit(false);
                    try {
                        PreparedStatement pc = cn.prepareStatement("SELECT estado_cuenta, fecha_pago FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ? FOR UPDATE");
                        pc.setInt(1, idCuenta);
                        ResultSet rc = pc.executeQuery();
                        if (!rc.next()) {
                            mensaje = "La cuenta no existe.";
                        } else if (rc.getString(1).equals("ANULADA")) {
                            mensaje = "La cuenta ya estaba anulada.";
                        } else {
                            if (rc.getString(1).equals("PAGADA")) {
                                PreparedStatement pk = cn.prepareStatement("SELECT COUNT(*) FROM tt_cierre_caja WHERE fecha = ?");
                                pk.setDate(1, rc.getDate(2) == null ? java.sql.Date.valueOf(LocalDate.now()) : new java.sql.Date(rc.getDate(2).getTime()));
                                ResultSet rk = pk.executeQuery();
                                rk.next();
                                if (rk.getInt(1) > 0) {
                                    mensaje = "La caja de ese día ya se cerró: no se puede anular.";
                                } else {
                                    PreparedStatement pst = cn.prepareStatement("UPDATE tt_producto p SET stock = p.stock + d.cantidad FROM tt_detalle_cuenta d "
                                            + "WHERE d.id_cuenta_atencion = ? AND d.id_producto_servicio = p.id_producto_servicio");
                                    pst.setInt(1, idCuenta);
                                    pst.executeUpdate();
                                    PreparedStatement pp = cn.prepareStatement("UPDATE tt_pago SET estado_pago = 'ANULADO' WHERE id_cuenta_atencion = ? AND estado_pago = 'APROBADO'");
                                    pp.setInt(1, idCuenta);
                                    pp.executeUpdate();
                                    PreparedStatement pq = cn.prepareStatement("UPDATE tt_comprobante SET estado_comprobante = 'ANULADO' WHERE id_pago IN "
                                            + "(SELECT id_pago FROM tt_pago WHERE id_cuenta_atencion = ?)");
                                    pq.setInt(1, idCuenta);
                                    pq.executeUpdate();
                                }
                            }
                            if (mensaje == null) {
                                PreparedStatement pu = cn.prepareStatement("UPDATE tt_cuenta_atencion SET estado_cuenta = 'ANULADA', motivo_anulacion = ?, "
                                        + "id_usuario_anula = ?, fecha_anulacion = NOW() WHERE id_cuenta_atencion = ?");
                                pu.setString(1, motivo);
                                pu.setInt(2, idUsuario);
                                pu.setInt(3, idCuenta);
                                pu.executeUpdate();
                                PreparedStatement pl = cn.prepareStatement("INSERT INTO tt_auditoria (accion, id_usuario, id_entidad, detalle, usuario_registro) VALUES ('CUENTA_ANULADA', ?, ?, ?, ?)");
                                pl.setInt(1, idUsuario);
                                pl.setInt(2, idCuenta);
                                pl.setString(3, "Motivo: " + motivo);
                                pl.setString(4, usuario);
                                pl.executeUpdate();
                                mensaje = "Cuenta anulada.";
                            }
                        }
                        if (mensaje != null && mensaje.equals("Cuenta anulada.")) {
                            cn.commit();
                        } else {
                            cn.rollback();
                        }
                    } catch (SQLException e) {
                        cn.rollback();
                        mensaje = "No se pudo anular: " + e.getMessage();
                    } finally {
                        cn.setAutoCommit(true);
                    }
                }
            } else if ("cerrar".equals(accion)) {
                BigDecimal contado;
                try {
                    contado = new BigDecimal(request.getParameter("efectivoContado").replace(",", "."));
                } catch (Exception e) {
                    contado = null;
                }
                String observacion = request.getParameter("observacion") == null ? "" : request.getParameter("observacion").trim();
                if (contado == null || contado.signum() < 0 || contado.scale() > 2) {
                    mensaje = "Escribe cuánto efectivo contaste.";
                } else if (contado.compareTo(new BigDecimal("100000")) > 0) {
                    mensaje = "Revisa el efectivo contado.";
                } else if (observacion.length() > 250) {
                    mensaje = "La observación no puede pasar de 250 caracteres.";
                } else {
                    PreparedStatement pk = cn.prepareStatement("SELECT COUNT(*) FROM tt_cierre_caja WHERE id_usuario_cajero = ? AND fecha = CURRENT_DATE");
                    pk.setInt(1, idUsuario);
                    ResultSet rk = pk.executeQuery();
                    rk.next();
                    if (rk.getInt(1) > 0) {
                        mensaje = "Tu caja de hoy ya estaba cerrada.";
                    } else {
                        PreparedStatement pp = cn.prepareStatement("SELECT mp.codigo, p.monto, p.estado_pago FROM tt_pago p JOIN tm_metodo_pago mp ON mp.id_metodo_pago = p.id_metodo_pago "
                                + "WHERE p.id_usuario_cajero = ? AND p.fecha_creacion::date = CURRENT_DATE");
                        pp.setInt(1, idUsuario);
                        ResultSet rp = pp.executeQuery();
                        BigDecimal efectivo = BigDecimal.ZERO;
                        BigDecimal tarjeta = BigDecimal.ZERO;
                        BigDecimal billetera = BigDecimal.ZERO;
                        int cantidad = 0;
                        while (rp.next()) {
                            if (!rp.getString(3).equals("APROBADO")) {
                                continue;
                            }
                            cantidad++;
                            if (rp.getString(1).equals("EFECTIVO")) {
                                efectivo = efectivo.add(rp.getBigDecimal(2));
                            } else if (rp.getString(1).equals("TARJETA")) {
                                tarjeta = tarjeta.add(rp.getBigDecimal(2));
                            } else {
                                billetera = billetera.add(rp.getBigDecimal(2));
                            }
                        }
                        BigDecimal diferencia = contado.subtract(efectivo);
                        if (diferencia.signum() != 0 && observacion.length() < 5) {
                            mensaje = "Hay una diferencia de S/ " + diferencia + ": explica el motivo.";
                        } else {
                            PreparedStatement pi = cn.prepareStatement("INSERT INTO tt_cierre_caja (id_usuario_cajero, fecha, cantidad_pagos, total_efectivo, total_tarjeta, "
                                    + "total_billetera, efectivo_contado, observacion, usuario_registro) VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?)");
                            pi.setInt(1, idUsuario);
                            pi.setInt(2, cantidad);
                            pi.setBigDecimal(3, efectivo);
                            pi.setBigDecimal(4, tarjeta);
                            pi.setBigDecimal(5, billetera);
                            pi.setBigDecimal(6, contado);
                            pi.setString(7, observacion);
                            pi.setString(8, usuario);
                            pi.executeUpdate();
                            mensaje = "Caja cerrada. Total S/ " + efectivo.add(tarjeta).add(billetera);
                        }
                    }
                }
            } else if (!"abrir".equals(accion) && !"abrirDesdeCita".equals(accion)) {
                mensaje = "Acción no reconocida.";
            }
        } catch (SQLException | RuntimeException e) {
            mensaje = "No se pudo completar la operación: " + e.getMessage();
        }
        session.setAttribute("mensaje", mensaje);
        response.sendRedirect(volver);
    }
}
