package pruebas.servicios;

import modelo.servicios.Validaciones;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

/** Validaciones de datos comunes (DNI, RUC con dígito verificador, correo, celular, fechas). */
public class ValidacionesTest {

    @Test
    public void documentosDeIdentidad() {
        assertTrue(Validaciones.dni("10000004"));
        assertFalse(Validaciones.dni("1234"));
        assertFalse(Validaciones.dni(null));
        assertTrue(Validaciones.ruc("20601234565"));
        assertFalse(Validaciones.ruc("20601234560"));
        assertFalse(Validaciones.ruc("99999999999"));
        assertFalse(Validaciones.ruc(null));
    }

    @Test
    public void contacto() {
        assertTrue(Validaciones.correo("ana@clinica.pe"));
        assertFalse(Validaciones.correo("ana@"));
        assertFalse(Validaciones.correo(null));
        assertTrue(Validaciones.celular("987654321"));
        assertFalse(Validaciones.celular("887654321"));
        assertFalse(Validaciones.celular(null));
        assertTrue(Validaciones.telefono("014567890"));
        assertFalse(Validaciones.telefono(null));
        assertTrue(Validaciones.nombre("José María"));
        assertFalse(Validaciones.nombre("J0sé"));
        assertFalse(Validaciones.nombre(null));
    }

    @Test
    public void fechaDeNacimiento() {
        assertNull(Validaciones.fechaNacimiento(null));
        assertNull(Validaciones.fechaNacimiento(LocalDate.now().minusYears(30)));
        assertNotNull(Validaciones.fechaNacimiento(LocalDate.now().plusDays(1)));
        assertNotNull(Validaciones.fechaNacimiento(LocalDate.now().minusYears(200)));
    }

    @Test
    public void normalizacion() {
        assertEquals("Ana María", Validaciones.normalizar("  Ana   María "));
        assertEquals("", Validaciones.normalizar(null));
        assertEquals("987654321", Validaciones.soloDigitos("987-654 321"));
        assertEquals("", Validaciones.soloDigitos(null));
    }
}
