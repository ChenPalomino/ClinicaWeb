package pruebas.servicios;

import fakes.BilleterasFalsas;
import modelo.dto.BilleteraConfigDTO;
import modelo.entidades.Usuario;
import modelo.servicios.BilleteraService;
import org.junit.Test;

import java.util.Base64;

import static fakes.Datos.usuario;
import static org.junit.Assert.*;

/** RF-13 · Configuración de la billetera digital: celular, titular, QR y billeteras aceptadas. */
public class BilleteraServiceTest {

    private final BilleterasFalsas repositorio = new BilleterasFalsas();
    private final BilleteraService servicio = new BilleteraService(repositorio);
    private final Usuario admin = usuario("admin", 1, 1);

    private static BilleteraConfigDTO datos(String celular, String titular) {
        BilleteraConfigDTO d = new BilleteraConfigDTO();
        d.setCelular(celular);
        d.setTitular(titular);
        return d;
    }

    private static String png(int bytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    @Test
    public void guardaCelularYTitularValidos() {
        assertTrue(servicio.guardarDatos(datos("987 654 321", "Clínica  Modelo"), admin).isEmpty());
        assertEquals("987654321", repositorio.celular);
        assertEquals("Clínica Modelo", repositorio.titular);
        BilleteraConfigDTO c = servicio.configuracion();
        assertEquals("987 654 321", c.getCelularLegible());
        assertTrue(servicio.guardarDatos(datos("", ""), admin).isEmpty());
    }

    @Test
    public void rechazaCelularOTitularInvalidos() {
        assertTrue(servicio.guardarDatos(datos("12345", ""), admin).containsKey("celular"));
        assertTrue(servicio.guardarDatos(datos("987654321", "x".repeat(81)), admin).containsKey("titular"));
    }

    @Test
    public void subeYQuitaElQr() {
        assertNull(servicio.guardarQr(png(1000), admin));
        assertTrue(servicio.qr().isPresent());
        assertEquals("image/png", servicio.qr().get()[1]);
        assertTrue(servicio.configuracion().isTieneQr());
        servicio.quitarQr(admin);
        assertTrue(servicio.qr().isEmpty());
    }

    @Test
    public void validaLaImagenDelQr() {
        assertNotNull(servicio.guardarQr(null, admin));
        assertNotNull(servicio.guardarQr("hola", admin));
        assertNotNull(servicio.guardarQr("data:image/gif;base64,AAAA", admin));
        assertNotNull(servicio.guardarQr("data:image/png;base64,@@@", admin));
        assertNotNull(servicio.guardarQr(png(BilleteraService.QR_MAXIMO_BYTES + 1), admin));
        assertNotNull(servicio.guardarQr("data:image/png;base64,", admin));
    }

    @Test
    public void agregaBilleterasSinRepetir() {
        assertNull(servicio.agregar("Bim", admin));
        assertEquals(3, servicio.billeteras().size());
        assertNotNull(servicio.agregar("yape", admin));
        assertNotNull(servicio.agregar("x", admin));
    }

    @Test
    public void siempreQuedaAlMenosUnaBilleteraActiva() {
        assertNull(servicio.cambiarEstado(2, false, admin));
        assertNotNull(servicio.cambiarEstado(1, false, admin));
        assertNull(servicio.cambiarEstado(2, true, admin));
        assertNotNull(servicio.cambiarEstado(99, true, admin));
    }
}
