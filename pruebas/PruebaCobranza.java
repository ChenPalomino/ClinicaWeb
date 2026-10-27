import modelo.cobranza.CalculadoraCuenta;
import modelo.cobranza.TotalesCuenta;
import modelo.cobranza.comprobante.*;
import modelo.cobranza.pago.*;
import modelo.cobranza.tarifa.*;
import modelo.dto.*;
import modelo.entidades.Usuario;
import modelo.repositorio.*;
import modelo.servicios.CobranzaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Pruebas SIN base de datos de la Etapa 6 (RNF-03): estrategias, fábrica, cálculos y servicio con repositorios falsos. */
public class PruebaCobranza {
    static int fallas = 0;
    static void check(String n, boolean ok) { System.out.println((ok ? "OK   " : "FALLA ") + n); if (!ok) fallas++; }
    static BigDecimal bd(String s) { return new BigDecimal(s); }
    static boolean igual(BigDecimal a, String b) { return a != null && a.compareTo(bd(b)) == 0; }

    // ---------- repositorios falsos ----------
    static class CuentasFalsas implements CuentaAtencionRepository {
        Map<Integer, CuentaAtencionDTO> cuentas = new HashMap<>();
        List<ProductoServicioDTO> items = new ArrayList<>();
        int sec = 0;
        public PaginaDTO<CuentaAtencionDTO> listar(FiltroBusquedaDTO f) { return null; }
        public Optional<CuentaAtencionDTO> buscarPorId(int id) {
            CuentaAtencionDTO c = cuentas.get(id);
            if (c == null) return Optional.empty();
            CuentaAtencionDTO copia = new CuentaAtencionDTO();   // como la BD: una copia nueva en cada lectura
            copia.setIdCuentaAtencion(c.getIdCuentaAtencion()); copia.setNumeroCuenta(c.getNumeroCuenta());
            copia.setEstadoCuenta(c.getEstadoCuenta()); copia.setTipoCobertura(c.getTipoCobertura());
            copia.setPorcentajeCobertura(c.getPorcentajeCobertura()); copia.aplicarTotales(c.getTotales());
            List<DetalleCuentaDTO> ds = new ArrayList<>();
            for (DetalleCuentaDTO d : c.getDetalles()) { DetalleCuentaDTO x = new DetalleCuentaDTO();
                x.setIdProductoServicio(d.getIdProductoServicio()); x.setDescripcion(d.getDescripcion()); x.setTipo(d.getTipo());
                x.setCantidad(d.getCantidad()); x.setPrecioUnitario(d.getPrecioUnitario()); x.setAfectoIgv(d.isAfectoIgv());
                x.setImporte(d.getImporte()); x.setMontoAseguradora(d.getMontoAseguradora()); x.setMontoDescuento(d.getMontoDescuento()); ds.add(x); }
            copia.setDetalles(ds);
            return Optional.of(copia);
        }
        public int crear(int idPaciente, int idCobertura, String obs, String u) {
            CuentaAtencionDTO c = new CuentaAtencionDTO(); c.setIdCuentaAtencion(++sec); c.setNumeroCuenta("CA-00000" + sec);
            c.setIdPaciente(idPaciente); c.setIdCobertura(idCobertura);
            CoberturaDTO cb = coberturaDelPaciente(idPaciente).get(); c.setTipoCobertura(cb.getTipo()); c.setPorcentajeCobertura(cb.getPorcentaje());
            cuentas.put(sec, c); return sec;
        }
        public Optional<Integer> pendienteDelPaciente(int idPaciente) {
            return cuentas.values().stream().filter(c -> c.getIdPaciente() == idPaciente && c.isPendiente()).map(CuentaAtencionDTO::getIdCuentaAtencion).findFirst();
        }
        public boolean guardarDetalles(int id, List<DetalleCuentaDTO> d, TotalesCuenta t, String u) {
            CuentaAtencionDTO c = cuentas.get(id); if (!c.isPendiente()) return false;
            c.setDetalles(new ArrayList<>(d)); c.aplicarTotales(t); return true;
        }
        public Optional<CoberturaDTO> coberturaDelPaciente(int id) {
            switch (id) { case 1: return Optional.of(new CoberturaDTO(1, "Particular", "PARTICULAR", bd("0")));
                          case 2: return Optional.of(new CoberturaDTO(2, "EPS", "EPS", bd("70")));
                          case 3: return Optional.of(new CoberturaDTO(3, "Seguro", "SEGURO", bd("80")));
                          case 4: return Optional.of(new CoberturaDTO(4, "Convenio", "CONVENIO", bd("40")));
                          default: return Optional.empty(); }
        }
        public List<ProductoServicioDTO> itemsVendibles() { return items; }
        public List<CuentaAtencionDTO> listarDelUsuarioPaciente(int u) { return List.of(); }
        public int contarPendientes() { return 0; }
    }
    static class PagosFalsos implements PagoRepository {
        List<PagoDTO> rechazos = new ArrayList<>(); List<PagoDTO> aprobados = new ArrayList<>();
        CuentasFalsas cuentas; int numero = 0; RuntimeException fallar;
        public List<OpcionDTO> metodosActivos() { return List.of(); }
        public int registrarRechazo(PagoDTO p) { rechazos.add(p); return rechazos.size(); }
        public int registrarPagoAprobado(PagoDTO p, Comprobante c, List<DetalleCuentaDTO> d, LocalDate hoy) {
            if (fallar != null) throw fallar;
            aprobados.add(p); c.setNumero(++numero); cuentas.cuentas.get(p.getIdCuentaAtencion()).setEstadoCuenta("PAGADA"); return 100 + numero;
        }
        public void anular(int id, int u, String m) { cuentas.cuentas.get(id).setEstadoCuenta("ANULADA"); }
        public Optional<ComprobanteDTO> buscarComprobante(int id) { return Optional.empty(); }
        public List<PagoDTO> pagosDelCajero(int u, LocalDate f) { return List.of(); }
    }
    static ProductoServicioDTO item(int id, String nombre, String tipo, String precio, Integer stock, boolean igv) {
        ProductoServicioDTO p = new ProductoServicioDTO(); p.setIdProductoServicio(id); p.setNombre(nombre); p.setCodigo("X" + id);
        p.setTipo(tipo); p.setPrecio(bd(precio)); p.setStock(stock); p.setAfectoIgv(igv); return p;
    }

    public static void main(String[] a) {
        // ===== Strategy de tarifas (RF-12) =====
        LineaTarifada l = PoliticaTarifariaFactory.obtener("PARTICULAR").tarifar("SERVICIO", bd("80.00"), 2, bd("0"));
        check("particular paga 160.00", igual(l.getImporte(), "160.00") && igual(l.getMontoAseguradora(), "0"));
        l = PoliticaTarifariaFactory.obtener("EPS").tarifar("PRODUCTO", bd("25.00"), 1, bd("70"));
        check("EPS 70%: aseguradora 17.50, copago 7.50", igual(l.getMontoAseguradora(), "17.50") && igual(l.getImporte(), "7.50"));
        l = PoliticaTarifariaFactory.obtener("SEGURO").tarifar("SERVICIO", bd("150.00"), 1, bd("80"));
        check("seguro 80% en servicio: paga 30.00", igual(l.getImporte(), "30.00"));
        l = PoliticaTarifariaFactory.obtener("SEGURO").tarifar("PRODUCTO", bd("25.00"), 2, bd("80"));
        check("seguro NO cubre productos: paga 50.00", igual(l.getImporte(), "50.00") && igual(l.getPorcentajeCubierto(), "0"));
        l = PoliticaTarifariaFactory.obtener("CONVENIO").tarifar("SERVICIO", bd("33.33"), 1, bd("40"));
        check("convenio 40%: descuento 13.33, paga 20.00", igual(l.getMontoDescuento(), "13.33") && igual(l.getImporte(), "20.00"));
        boolean lanzo = false; try { PoliticaTarifariaFactory.obtener("OTRA"); } catch (IllegalArgumentException e) { lanzo = true; }
        check("cobertura desconocida -> excepción", lanzo);
        check("describe la EPS", PoliticaTarifariaFactory.obtener("EPS").describir(bd("70.00")).contains("70 %"));

        // ===== Calculadora: IGV incluido en el precio =====
        DetalleCuentaDTO d1 = new DetalleCuentaDTO(); d1.setAfectoIgv(true); d1.setImporte(bd("80.00"));
        DetalleCuentaDTO d2 = new DetalleCuentaDTO(); d2.setAfectoIgv(false); d2.setImporte(bd("12.00"));
        TotalesCuenta t = CalculadoraCuenta.totalizar(List.of(d1, d2), bd("18"));
        check("base 67.80 + IGV 12.20 + exonerado 12.00 = 92.00: " + t.getOpGravada() + " " + t.getIgv() + " " + t.getTotal(),
              igual(t.getOpGravada(), "67.80") && igual(t.getIgv(), "12.20") && igual(t.getOpExonerada(), "12.00") && igual(t.getTotal(), "92.00"));
        t = CalculadoraCuenta.totalizar(List.of(), bd("18"));
        check("cuenta vacía = 0", igual(t.getTotal(), "0"));
        DetalleCuentaDTO d3 = new DetalleCuentaDTO(); d3.setAfectoIgv(true); d3.setImporte(bd("0.01"));
        t = CalculadoraCuenta.totalizar(List.of(d3), bd("18"));
        check("redondeo: base+IGV = total exacto", igual(t.getOpGravada().add(t.getIgv()), "0.01"));

        // ===== Strategy de pagos (RF-13, RF-14) =====
        SolicitudPago s = new SolicitudPago(); s.setMonto(bd("80.00")); s.setMontoRecibido(bd("100"));
        MetodoPagoStrategy ef = MetodoPagoFactory.obtener("EFECTIVO");
        check("efectivo válido", ef.validar(s).isEmpty());
        ResultadoPago r = ef.procesar(s);
        check("efectivo: vuelto 20.00", r.isAprobado() && igual(r.getVuelto(), "20.00"));
        s.setMontoRecibido(bd("50"));
        check("efectivo insuficiente -> error de formulario (faltan 30.00)", ef.validar(s).getOrDefault("montoRecibido", "").contains("30.00"));
        s.setMontoRecibido(null);
        check("efectivo sin monto -> error", ef.validar(s).containsKey("montoRecibido"));
        s.setMontoRecibido(bd("10.555"));
        check("efectivo con 3 decimales -> error", ef.validar(s).containsKey("montoRecibido"));
        s.setMontoRecibido(bd("9000"));
        check("efectivo > 5000 -> error", ef.validar(s).containsKey("montoRecibido"));

        MetodoPagoStrategy tj = MetodoPagoFactory.obtener("TARJETA");
        SolicitudPago st = new SolicitudPago(); st.setMonto(bd("80"));
        check("tarjeta sin datos -> 3 errores", tj.validar(st).size() == 3);
        st.setTipoTarjeta("debito"); st.setUltimosDigitos("4321"); st.setCodigoAutorizacion("123456");
        check("tarjeta válida", tj.validar(st).isEmpty());
        r = tj.procesar(st);
        check("tarjeta aprobada con referencia sin número completo: " + r.getReferencia(), r.isAprobado() && r.getReferencia().contains("**** 4321") && !r.getReferencia().contains("123456789"));
        st.setCodigoAutorizacion("000000");
        r = tj.procesar(st);
        check("tarjeta rechazada por el banco (RF-14)", !r.isAprobado() && r.getMotivoRechazo().contains("4321"));

        MetodoPagoStrategy bi = MetodoPagoFactory.obtener("BILLETERA");
        SolicitudPago sb = new SolicitudPago(); sb.setMonto(bd("80"));
        sb.setIdBilletera(1); sb.setNombreBilletera("Yape"); sb.setNumeroOperacion("12345678"); sb.setCelular("987 654 321");
        check("billetera válida", bi.validar(sb).isEmpty());
        r = bi.procesar(sb);
        check("billetera oculta el celular: " + r.getReferencia(), r.isAprobado() && r.getReferencia().contains("***321") && !r.getReferencia().contains("987654321"));
        sb.setCelular("812345678");
        check("celular que no empieza con 9 -> error", bi.validar(sb).containsKey("celular"));
        sb.setCelular("987654321"); sb.setNumeroOperacion("00000000");
        check("operación no encontrada -> rechazo", !bi.procesar(sb).isAprobado());
        check("método desconocido -> null", MetodoPagoFactory.obtener("CHEQUE") == null);

        // ===== Factory Method de comprobantes (RF-15) =====
        ParametroClinicaDTO par = new ParametroClinicaDTO();
        TotalesCuenta tot = new TotalesCuenta(bd("67.80"), bd("0"), bd("12.20"), bd("0"), bd("0"));
        DatosClienteDTO cli = new DatosClienteDTO(); cli.setTipoComprobante("BOLETA"); cli.setNombre("Ana Pérez"); cli.setNumeroDocumento("10000004");
        ComprobanteFactory fb = ComprobanteFactory.para("BOLETA");
        Comprobante cb = fb.emitir(cli, tot, par);
        check("BoletaFactory crea una Boleta B001 con DNI", cb instanceof Boleta && "B001".equals(cb.getSerie()) && "DNI".equals(cb.getTipoDocumento()));
        cb.setNumero(12);
        check("numeración B001-00000012", "B001-00000012".equals(cb.getNumeroCompleto()));
        cli.setNumeroDocumento("");
        check("boleta menor a 700 sin DNI es válida (doc '-')", fb.validarCliente(cli, tot, par).isEmpty() && "-".equals(fb.emitir(cli, tot, par).getTipoDocumento()));
        TotalesCuenta grande = new TotalesCuenta(bd("700.00"), bd("0"), bd("126.00"), bd("0"), bd("0"));
        check("boleta desde 700 exige DNI", fb.validarCliente(cli, grande, par).containsKey("numeroDocumento"));
        DatosClienteDTO emp = new DatosClienteDTO(); emp.setTipoComprobante("FACTURA"); emp.setNumeroDocumento("20601234565");
        emp.setNombre("Empresa Demo S.A.C."); emp.setDireccion("Av. Lima 123");
        ComprobanteFactory ff = ComprobanteFactory.para(emp.getTipoComprobante());
        Comprobante cf = ff.emitir(emp, tot, par);
        check("FacturaFactory crea una Factura F001 con RUC", cf instanceof Factura && "F001".equals(cf.getSerie()) && "RUC".equals(cf.getTipoDocumento()));
        emp.setNumeroDocumento("20601234560");
        check("factura con RUC de dígito verificador inválido -> error", ff.validarCliente(emp, tot, par).containsKey("numeroDocumento"));
        emp.setDireccion("");
        check("factura sin dirección -> error", ff.validarCliente(emp, tot, par).containsKey("direccion"));
        lanzo = false; try { ff.emitir(emp, tot, par); } catch (IllegalArgumentException e) { lanzo = true; }
        check("emitir con datos inválidos -> excepción", lanzo);

        // ===== CobranzaService con repositorios falsos =====
        CuentasFalsas cuentas = new CuentasFalsas();
        cuentas.items.add(item(1, "Consulta general", "SERVICIO", "80.00", null, true));
        cuentas.items.add(item(2, "Paracetamol", "PRODUCTO", "25.00", 3, true));
        cuentas.items.add(item(3, "Alcohol", "PRODUCTO", "12.00", 10, false));
        PagosFalsos pagos = new PagosFalsos(); pagos.cuentas = cuentas;
        CobranzaService srv = new CobranzaService(cuentas, pagos, () -> par, () -> LocalDate.of(2026, 10, 13));
        Usuario cajero = new Usuario.UsuarioBuilder("recepcion", "x").idUsuario(2).rolId(2).build();
        Usuario admin = new Usuario.UsuarioBuilder("admin", "x").idUsuario(1).rolId(1).build();

        CobranzaService.Apertura ap = srv.abrirCuenta(2, "", cajero);
        check("abre cuenta para paciente EPS", ap.isExito() && !ap.yaExistia());
        CobranzaService.Apertura ap2 = srv.abrirCuenta(2, "", cajero);
        check("no duplica: devuelve la pendiente", ap2.yaExistia() && ap2.idCuenta() == ap.idCuenta());
        check("paciente inexistente -> error", !srv.abrirCuenta(99, "", cajero).isExito());
        int id = ap.idCuenta();
        check("agregar consulta", srv.agregarItem(id, 1, 1, cajero) == null);
        check("agregar paracetamol x2", srv.agregarItem(id, 2, 2, cajero) == null);
        check("sumar 2 más supera el stock (3) -> error", srv.agregarItem(id, 2, 2, cajero).contains("Solo quedan 3"));
        check("cantidad 0 al agregar -> error", srv.agregarItem(id, 1, 0, cajero) != null);
        check("item inexistente -> error", srv.agregarItem(id, 77, 1, cajero) != null);
        CuentaAtencionDTO c = srv.obtener(id).get();
        // EPS 70%: consulta 80 -> 24.00; paracetamol 50 -> 15.00; total 39.00
        check("EPS: total 39.00 y aseguradora 91.00: " + c.getTotal() + " / " + c.getMontoAseguradora(),
              igual(c.getTotal(), "39.00") && igual(c.getMontoAseguradora(), "91.00") && c.getDetalles().size() == 2);
        check("cambiar cantidad a 1", srv.cambiarCantidad(id, 2, 1, cajero) == null && srv.obtener(id).get().getDetalles().stream().anyMatch(x -> x.getCantidad() == 1 && x.getIdProductoServicio() == 2));
        check("quitar (cantidad 0)", srv.cambiarCantidad(id, 2, 0, cajero) == null && srv.obtener(id).get().getDetalles().size() == 1);
        check("quitar algo que no está -> error", srv.cambiarCantidad(id, 3, 0, cajero) != null);
        c = srv.obtener(id).get();
        check("total ahora 24.00", igual(c.getTotal(), "24.00"));

        // cobrar con datos incompletos
        DatosClienteDTO bol = new DatosClienteDTO(); bol.setTipoComprobante("BOLETA"); bol.setNombre("Paciente Demo"); bol.setNumeroDocumento("10000004");
        SolicitudPago sp = new SolicitudPago();
        CobranzaService.ResultadoCobro rc = srv.cobrar(id, "TARJETA", sp, bol, cajero);
        check("tarjeta sin datos -> errores de formulario, nada registrado", !rc.errores().isEmpty() && pagos.rechazos.isEmpty() && pagos.aprobados.isEmpty());
        rc = srv.cobrar(id, null, sp, bol, cajero);
        check("sin método -> error 'metodo'", rc.errores().containsKey("metodo"));
        // rechazo (RF-14)
        sp.setTipoTarjeta("CREDITO"); sp.setUltimosDigitos("1111"); sp.setCodigoAutorizacion("000000");
        rc = srv.cobrar(id, "TARJETA", sp, bol, cajero);
        check("rechazo registrado y la cuenta sigue pendiente", rc.rechazo() != null && pagos.rechazos.size() == 1
              && "RECHAZADO".equals(pagos.rechazos.get(0).getEstadoPago()) && srv.obtener(id).get().isPendiente());
        // reintento aprobado en efectivo
        SolicitudPago efe = new SolicitudPago(); efe.setMontoRecibido(bd("50"));
        rc = srv.cobrar(id, "EFECTIVO", efe, bol, cajero);
        check("reintento en efectivo: comprobante B001-00000001 y vuelto 26.00: " + rc.comprobante() + " " + rc.vuelto(),
              rc.isExito() && "B001-00000001".equals(rc.comprobante()) && igual(rc.vuelto(), "26.00"));
        check("el pago guardado tiene monto 24.00 y el cajero", igual(pagos.aprobados.get(0).getMonto(), "24.00") && pagos.aprobados.get(0).getIdUsuarioCajero() == 2);
        check("cuenta pagada ya no se modifica", srv.agregarItem(id, 1, 1, cajero) != null);
        check("cuenta pagada no se cobra dos veces", srv.cobrar(id, "EFECTIVO", efe, bol, cajero).error() != null);

        // conflicto en la transacción (ej. otro cajero cobró primero)
        int id2 = srv.abrirCuenta(1, "", cajero).idCuenta();
        srv.agregarItem(id2, 3, 1, cajero);
        check("particular exonerado: total 12.00 sin IGV", igual(srv.obtener(id2).get().getTotal(), "12.00") && igual(srv.obtener(id2).get().getIgv(), "0"));
        pagos.fallar = new OperacionRechazadaException("Esta cuenta ya fue pagada");
        efe.setMontoRecibido(bd("12"));
        rc = srv.cobrar(id2, "EFECTIVO", efe, bol, cajero);
        check("error de la transacción llega como mensaje", !rc.isExito() && rc.error().contains("ya fue pagada"));
        pagos.fallar = null;
        check("cuenta vacía no se cobra", srv.cobrar(srv.abrirCuenta(4, "", cajero).idCuenta(), "EFECTIVO", efe, bol, cajero).error().contains("al menos"));

        // anular (RF-16)
        check("recepción NO puede anular", srv.anular(id2, "Paciente se retiró sin atenderse", cajero) != null);
        check("motivo corto -> error", srv.anular(id2, "corto", admin) != null);
        check("admin anula con motivo", srv.anular(id2, "Paciente se retiró sin atenderse", admin) == null && srv.obtener(id2).get().isAnulada());
        check("puedeAnular", CobranzaService.puedeAnular(admin) && !CobranzaService.puedeAnular(cajero) && !CobranzaService.puedeAnular(null));

        // convenio y seguro en el servicio
        int id3 = srv.abrirCuenta(3, "", cajero).idCuenta();
        srv.agregarItem(id3, 1, 1, cajero); srv.agregarItem(id3, 2, 1, cajero);
        check("seguro 80%: 16.00 + 25.00 = 41.00", igual(srv.obtener(id3).get().getTotal(), "41.00"));

        System.out.println(fallas == 0 ? "TODAS LAS PRUEBAS PASARON" : "FALLAS: " + fallas);
        if (fallas > 0) System.exit(1);
    }
}
