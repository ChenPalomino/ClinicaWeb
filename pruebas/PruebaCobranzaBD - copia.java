import configuracion.ConexionBDSingleton;
import configuracion.ConfiguracionClinicaSingleton;
import modelo.cobranza.pago.SolicitudPago;
import modelo.dto.*;
import modelo.entidades.Usuario;
import modelo.servicios.CierreCajaService;
import modelo.servicios.CobranzaService;
import modelo.servicios.ParametroClinicaService;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Pruebas de INTEGRACIÓN de la Etapa 6 con PostgreSQL real (RNF-08: pago atómico). */
public class PruebaCobranzaBD {
    static int fallas = 0;
    static void check(String n, boolean ok) { System.out.println((ok ? "OK   " : "FALLA ") + n); if (!ok) fallas++; }
    static Connection cn() throws SQLException { return ConexionBDSingleton.getInstancia().getConexion(); }
    static String uno(String sql, Object... p) throws SQLException {
        try (PreparedStatement s = cn().prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }
    static int idItem(String codigo) throws SQLException { return Integer.parseInt(uno("SELECT id_producto_servicio FROM tt_producto_servicio WHERE codigo = ?", codigo)); }
    static int stock(String codigo) throws SQLException { return Integer.parseInt(uno("SELECT stock FROM tt_producto_servicio WHERE codigo = ?", codigo)); }
    static SolicitudPago efectivo(String m) { SolicitudPago s = new SolicitudPago(); s.setMontoRecibido(new BigDecimal(m)); return s; }
    static DatosClienteDTO boleta() { DatosClienteDTO d = new DatosClienteDTO(); d.setTipoComprobante("BOLETA"); d.setNombre("Paciente Demo"); d.setNumeroDocumento("10000004"); return d; }

    public static void main(String[] a) throws Exception {
        CobranzaService srv = new CobranzaService();
        Usuario recep = new Usuario.UsuarioBuilder("recepcion", "x").idUsuario(Integer.parseInt(uno("SELECT id_usuario FROM tt_usuario WHERE nombre_usuario='recepcion'"))).rolId(2).build();
        Usuario admin = new Usuario.UsuarioBuilder("admin", "x").idUsuario(Integer.parseInt(uno("SELECT id_usuario FROM tt_usuario WHERE nombre_usuario='admin'"))).rolId(1).build();
        int paciente = Integer.parseInt(uno("SELECT p.id_paciente FROM tt_paciente p JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales WHERE d.dni = '10000004'"));
        int alcohol = idItem("PRD-0003"), consulta = idItem("SRV-0001");
        int stock0 = stock("PRD-0003");
        check("stock inicial del alcohol = 4", stock0 == 4);

        // 1. Cuenta + detalle + cobro
        int c1 = srv.abrirCuenta(paciente, "Prueba integración", recep).idCuenta();
        check("cuenta CA-xxxxxx creada", uno("SELECT numero_cuenta FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ?", c1).startsWith("CA-"));
        check("agrega consulta", srv.agregarItem(c1, consulta, 1, recep) == null);
        check("agrega alcohol x3", srv.agregarItem(c1, alcohol, 3, recep) == null);
        check("no deja agregar más que el stock", srv.agregarItem(c1, alcohol, 2, recep) != null);
        CuentaAtencionDTO cta = srv.obtener(c1).get();
        check("2 líneas guardadas y total = 116.00 (80 + 36)", cta.getDetalles().size() == 2 && cta.getTotal().compareTo(new BigDecimal("116.00")) == 0);
        check("la BD comprueba total = base + exonerado + IGV", "t".equals(uno("SELECT (total = op_gravada + op_exonerada + igv)::text::char FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ?", c1)));
        String ultimoAntes = uno("SELECT ultimo_numero FROM tm_serie_comprobante WHERE serie = 'B001'");
        CobranzaService.ResultadoCobro r = srv.cobrar(c1, "EFECTIVO", efectivo("120"), boleta(), recep);
        check("cobro aprobado: " + r.comprobante() + " vuelto " + r.vuelto(), r.isExito() && r.vuelto().compareTo(new BigDecimal("4.00")) == 0);
        check("cuenta PAGADA con fecha de pago", "PAGADA".equals(uno("SELECT estado_cuenta FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ? AND fecha_pago IS NOT NULL", c1)));
        check("stock descontado 4 -> 1", stock("PRD-0003") == 1);
        check("serie B001 avanzó 1", Integer.parseInt(uno("SELECT ultimo_numero FROM tm_serie_comprobante WHERE serie = 'B001'")) == Integer.parseInt(ultimoAntes) + 1);
        ComprobanteDTO comp = srv.comprobanteVisiblePara(r.idComprobante(), recep).orElse(null);
        check("comprobante legible con 2 líneas y total 116.00", comp != null && comp.getDetalles().size() == 2 && comp.getTotal().compareTo(new BigDecimal("116.00")) == 0);
        Usuario otroPaciente = new Usuario.UsuarioBuilder("otro", "x").idUsuario(9999).rolId(4).build();
        check("otro paciente NO ve el comprobante", srv.comprobanteVisiblePara(r.idComprobante(), otroPaciente).isEmpty());
        int idUsuarioPaciente = Integer.parseInt(uno("SELECT id_usuario FROM tt_datos_personales WHERE dni='10000004'"));
        Usuario dueno = new Usuario.UsuarioBuilder("pac", "x").idUsuario(idUsuarioPaciente).rolId(4).build();
        check("el paciente dueño SÍ lo ve", srv.comprobanteVisiblePara(r.idComprobante(), dueno).isPresent());
        check("aparece en 'Mis pagos' del paciente", srv.pagosDelPaciente(idUsuarioPaciente).stream().anyMatch(x -> x.getIdCuentaAtencion() == c1));

        // 2. Dos cajeros cobran la MISMA cuenta a la vez -> solo un cobro
        int c2 = srv.abrirCuenta(paciente, "", recep).idCuenta();
        srv.agregarItem(c2, consulta, 1, recep);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        Callable<CobranzaService.ResultadoCobro> cobro = () -> { salida.await(); return new CobranzaService().cobrar(c2, "EFECTIVO", efectivo("80"), boleta(), recep); };
        Future<CobranzaService.ResultadoCobro> f1 = pool.submit(cobro), f2 = pool.submit(cobro);
        salida.countDown();
        CobranzaService.ResultadoCobro r1 = f1.get(), r2 = f2.get();
        pool.shutdown();
        check("solo UNO de los dos cobros simultáneos se aprueba", (r1.isExito() ^ r2.isExito()));
        String msg = r1.isExito() ? (r2.error() != null ? r2.error() : "" + r2.errores()) : (r1.error() != null ? r1.error() : "" + r1.errores());
        check("el otro recibe un mensaje claro: " + msg, msg.contains("ya fue pagada") || msg.contains("ya está pagada"));
        check("1 solo pago aprobado y 1 comprobante", "1".equals(uno("SELECT COUNT(*) FROM tt_pago WHERE id_cuenta_atencion = ? AND estado_pago = 'APROBADO'", c2))
              && "1".equals(uno("SELECT COUNT(*) FROM tt_comprobante WHERE id_cuenta_atencion = ?", c2)));

        // 3. Falla a mitad de la transacción (stock agotado) -> NO se guarda nada
        int c3 = srv.abrirCuenta(paciente, "", recep).idCuenta();
        srv.agregarItem(c3, alcohol, 1, recep);                   // stock actual 1: se puede agregar
        // otra venta se lleva la última unidad antes de cobrar c3
        try (PreparedStatement s = cn().prepareStatement("UPDATE tt_producto_servicio SET stock = 0 WHERE codigo = 'PRD-0003'")) { s.executeUpdate(); }
        String serieAntes = uno("SELECT ultimo_numero FROM tm_serie_comprobante WHERE serie = 'B001'");
        r = srv.cobrar(c3, "EFECTIVO", efectivo("12"), boleta(), recep);
        check("sin stock: cobro rechazado con mensaje: " + r.error(), !r.isExito() && r.error() != null && r.error().contains("stock"));
        check("ROLLBACK: cuenta sigue PENDIENTE", "PENDIENTE".equals(uno("SELECT estado_cuenta FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ?", c3)));
        check("ROLLBACK: ningún pago ni comprobante", "0".equals(uno("SELECT COUNT(*) FROM tt_pago WHERE id_cuenta_atencion = ?", c3))
              && "0".equals(uno("SELECT COUNT(*) FROM tt_comprobante WHERE id_cuenta_atencion = ?", c3)));
        check("ROLLBACK: la numeración no se consumió", serieAntes.equals(uno("SELECT ultimo_numero FROM tm_serie_comprobante WHERE serie = 'B001'")));
        check("ROLLBACK: el stock no quedó negativo", stock("PRD-0003") == 0);
        try (PreparedStatement s = cn().prepareStatement("UPDATE tt_producto_servicio SET stock = 1 WHERE codigo = 'PRD-0003'")) { s.executeUpdate(); }

        // 4. Rechazo con tarjeta (RF-14) y reintento con factura
        SolicitudPago tj = new SolicitudPago(); tj.setTipoTarjeta("DEBITO"); tj.setUltimosDigitos("9876"); tj.setCodigoAutorizacion("000000");
        r = srv.cobrar(c3, "TARJETA", tj, boleta(), recep);
        check("tarjeta rechazada queda registrada", r.rechazo() != null && "1".equals(uno("SELECT COUNT(*) FROM tt_pago WHERE id_cuenta_atencion = ? AND estado_pago = 'RECHAZADO'", c3)));
        tj.setCodigoAutorizacion("654321");
        DatosClienteDTO fac = new DatosClienteDTO(); fac.setTipoComprobante("FACTURA"); fac.setNumeroDocumento("20601234565"); fac.setNombre("Empresa Demo S.A.C."); fac.setDireccion("Av. Lima 123");
        r = srv.cobrar(c3, "TARJETA", tj, fac, recep);
        check("reintento aprobado con FACTURA F001: " + r.comprobante(), r.isExito() && r.comprobante().startsWith("F001-"));
        check("la cuenta muestra el intento rechazado y el pago", srv.obtener(c3).get().getIntentosRechazados().size() == 1 && srv.obtener(c3).get().getPago().isAprobado());

        // 5. Anulación (RF-16): devuelve stock y anula comprobante
        int stockAntes = stock("PRD-0003");
        check("admin anula la cuenta pagada c3", srv.anular(c3, "Error en el registro del paciente", admin) == null);
        check("stock devuelto +1", stock("PRD-0003") == stockAntes + 1);
        check("pago y comprobante ANULADOS", "ANULADO".equals(uno("SELECT estado_pago FROM tt_pago WHERE id_cuenta_atencion = ? AND estado_pago <> 'RECHAZADO'", c3))
              && "ANULADO".equals(uno("SELECT estado_comprobante FROM tt_comprobante WHERE id_cuenta_atencion = ?", c3)));
        check("anular dos veces -> mensaje", srv.anular(c3, "Error en el registro del paciente", admin).contains("ya estaba anulada"));
        check("la BD exige motivo para ANULADA", "t".equals(uno("SELECT (motivo_anulacion IS NOT NULL)::text::char FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ?", c3)));

        // 6. Cierre de caja (RF-17)
        CierreCajaService caja = new CierreCajaService();
        ResumenCajaDTO hoy = caja.resumenDeHoy(recep);
        check("resumen: 2 cobros aprobados, 1 rechazado, 1 anulado: " + hoy.getCantidadPagos() + "/" + hoy.getCantidadRechazados() + "/" + hoy.getCantidadAnulados(),
              hoy.getCantidadPagos() == 2 && hoy.getCantidadRechazados() == 1 && hoy.getCantidadAnulados() == 1);
        check("efectivo del día = 196.00 (116 + 80)", hoy.getTotalEfectivo().compareTo(new BigDecimal("196.00")) == 0);
        check("diferencia sin explicación -> error", caja.cerrar(recep, new BigDecimal("190"), "") != null);
        check("cierra con faltante explicado", caja.cerrar(recep, new BigDecimal("190.00"), "Faltan 6 soles de sencillo") == null);
        check("diferencia guardada = -6.00", "-6.00".equals(uno("SELECT diferencia FROM tt_cierre_caja WHERE id_usuario_cajero = ?", recep.getIdUsuario())));
        check("no se puede cerrar dos veces", caja.cerrar(recep, new BigDecimal("190.00"), "otra vez") != null);
        int c4 = srv.abrirCuenta(paciente, "", recep).idCuenta();
        srv.agregarItem(c4, consulta, 1, recep);
        r = srv.cobrar(c4, "EFECTIVO", efectivo("80"), boleta(), recep);
        check("después del cierre ese cajero ya no cobra: " + r.error(), !r.isExito() && r.error().contains("cerraste tu caja"));
        r = srv.cobrar(c4, "EFECTIVO", efectivo("80"), boleta(), admin);
        check("otro cajero (admin) sí puede cobrarla", r.isExito());
        check("anular un cobro de una caja cerrada -> bloqueado", srv.anular(c1, "Prueba de caja cerrada", admin).contains("ya se cerró"));
        check("historial: admin ve el cierre de recepción", caja.historial(admin).stream().anyMatch(x -> x.getIdUsuarioCajero() == recep.getIdUsuario()));

        // 7. Parámetros (RF-22) + Singleton
        ParametroClinicaService ps = new ParametroClinicaService();
        ParametroClinicaDTO p = ps.obtener();
        p.setIgvPorcentaje(new BigDecimal("10"));
        check("guarda IGV 10 %", ps.guardar(p, admin).isEmpty());
        check("el Singleton ya usa el IGV nuevo", ConfiguracionClinicaSingleton.getInstancia().getParametros().getIgvPorcentaje().compareTo(new BigDecimal("10")) == 0);
        int c5 = srv.abrirCuenta(paciente, "", recep).idCuenta();
        srv.agregarItem(c5, consulta, 1, recep);    // 80 con IGV 10% -> base 72.73
        check("cuenta nueva con IGV 10 %: base 72.73", srv.obtener(c5).get().getOpGravada().compareTo(new BigDecimal("72.73")) == 0);
        p.setIgvPorcentaje(new BigDecimal("18")); p.setRuc("20601234560");
        check("RUC inválido -> error", ps.guardar(p, admin).containsKey("ruc"));
        p.setRuc("20601234565");
        check("restaura IGV 18 %", ps.guardar(p, admin).isEmpty());

        System.out.println(fallas == 0 ? "TODAS LAS PRUEBAS PASARON" : "FALLAS: " + fallas);
        System.exit(fallas == 0 ? 0 : 1);
    }
}
