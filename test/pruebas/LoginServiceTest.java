package pruebas;

import org.junit.Test;
import seguridad.EncriptadorPassword;
import seguridad.PoliticaPassword;

import static org.junit.Assert.*;

/** RNF-06 · Contraseñas con hash y sal, y política de contraseñas del inicio de sesión. */
public class LoginServiceTest {

    @Test
    public void elHashLlevaSalYSeVerifica() {
        String a = EncriptadorPassword.encriptar("Clinica2026*");
        String b = EncriptadorPassword.encriptar("Clinica2026*");
        assertNotEquals("la misma clave produce hashes distintos (sal)", a, b);
        assertTrue(EncriptadorPassword.esHash(a));
        assertTrue(EncriptadorPassword.verificar("Clinica2026*", a));
        assertFalse(EncriptadorPassword.verificar("otra-clave", a));
        assertFalse(a.contains("Clinica2026*"));
    }

    @Test
    public void politicaDeContrasenas() {
        assertNull(PoliticaPassword.validar("Clinica2026*"));
        assertNotNull(PoliticaPassword.validar("123"));
        assertNotNull(PoliticaPassword.validar(null));
        assertTrue(PoliticaPassword.coinciden("Abc12345*", "Abc12345*"));
        assertFalse(PoliticaPassword.coinciden("Abc12345*", "abc12345*"));
    }
}
