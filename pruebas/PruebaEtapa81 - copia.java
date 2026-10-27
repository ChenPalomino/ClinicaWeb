import modelo.cobranza.CotizadorConsulta;
import modelo.dto.CoberturaDTO;
import modelo.dto.EspecialidadDTO;
import modelo.dto.PrecioConsultaDTO;
import modelo.repositorio.MedicoRepository;
import modelo.servicios.EspecialidadService;

import java.math.BigDecimal;

public class PruebaEtapa81 {

    static int fallas = 0;

    static void check(String nombre, boolean ok) {
        System.out.println((ok ? "OK   " : "FALLA ") + nombre);
        if (!ok) fallas++;
    }

    static boolean igual(BigDecimal a, String b) { return a != null && a.compareTo(new BigDecimal(b)) == 0; }

    public static void main(String[] args) {
        PrecioConsultaDTO particular = CotizadorConsulta.cotizar(new BigDecimal("80.00"), null);
        check("particular paga el precio de lista", igual(particular.getAPagar(), "80.00") && !particular.isConCobertura());
        PrecioConsultaDTO eps = CotizadorConsulta.cotizar(new BigDecimal("80.00"), new CoberturaDTO(2, "EPS", "EPS", new BigDecimal("70")));
        check("EPS 70%: paga 24.00 y cubre 56.00", igual(eps.getAPagar(), "24.00") && igual(eps.getCubre(), "56.00"));
        PrecioConsultaDTO convenio = CotizadorConsulta.cotizar(new BigDecimal("100"), new CoberturaDTO(4, "Convenio", "CONVENIO", new BigDecimal("40")));
        check("convenio da descuento", convenio.isConvenio() && convenio.getAPagar().compareTo(new BigDecimal("100")) < 0);
        check("sin precio en el tarifario", !CotizadorConsulta.cotizar(null, null).isDefinido());
        check("cobertura desconocida = particular", igual(CotizadorConsulta.cotizar(new BigDecimal("50"),
                new CoberturaDTO(9, "X", "OTRA", new BigDecimal("50"))).getAPagar(), "50.00"));

        check("nombre propio", "Medicina General".equals(EspecialidadService.nombrePropio("medicina general")));
        check("conectores en minúscula", "Cirugía de Tórax".equals(EspecialidadService.nombrePropio("CIRUGÍA DE TÓRAX")));
        EspecialidadDTO e = new EspecialidadDTO();
        e.setNombre("Oftalmología");
        check("nombre válido sin precio", EspecialidadService.validar(e, "").isEmpty());
        check("precio con coma", EspecialidadService.validar(e, "80,50").isEmpty());
        check("precio inválido", EspecialidadService.validar(e, "abc").containsKey("precio"));
        check("precio cero", EspecialidadService.validar(e, "0").containsKey("precio"));
        check("precio con 3 decimales", EspecialidadService.validar(e, "1.234").containsKey("precio"));
        e.setNombre("A1");
        check("nombre con números o corto", EspecialidadService.validar(e, null).containsKey("nombre"));

        check("límite de búsqueda de médicos", MedicoRepository.LIMITE_BUSQUEDA == 100);
        System.out.println(fallas == 0 ? "TODAS LAS PRUEBAS PASARON" : fallas + " PRUEBA(S) FALLARON");
        if (fallas > 0) System.exit(1);
    }
}
