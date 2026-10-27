package fakes;

import modelo.dto.HorarioDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Usuario;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Datos de prueba reutilizables (sin base de datos). */
public final class Datos {

    private Datos() { }

    public static BigDecimal bd(String v) { return new BigDecimal(v); }

    public static boolean igual(BigDecimal a, String b) { return a != null && a.compareTo(bd(b)) == 0; }

    public static Usuario usuario(String nombre, int id, int rol) {
        return new Usuario.UsuarioBuilder(nombre, "x").idUsuario(id).rolId(rol).build();
    }

    public static ProductoServicioDTO item(int id, String nombre, String tipo, String precio, Integer stock, boolean igv) {
        ProductoServicioDTO p = new ProductoServicioDTO();
        p.setIdProductoServicio(id); p.setNombre(nombre); p.setCodigo("X" + id);
        p.setTipo(tipo); p.setPrecio(bd(precio)); p.setStock(stock); p.setAfectoIgv(igv);
        return p;
    }

    public static MedicoDTO medico(int id, int idUsuario, int especialidad, int duracion) {
        MedicoDTO m = new MedicoDTO();
        m.setIdMedico(id); m.setIdUsuario(idUsuario); m.setIdEspecialidad(especialidad); m.setEspecialidad("Medicina General");
        m.setNombres("Marco"); m.setApellidos("Demo " + id); m.setDuracionCita(duracion); m.setCmp("01234" + id); m.setConsultorio("101");
        for (int d = 1; d <= 5; d++) {
            m.getHorarios().add(new HorarioDTO(d, LocalTime.of(8, 0), LocalTime.of(13, 0)));
            m.getHorarios().add(new HorarioDTO(d, LocalTime.of(15, 0), LocalTime.of(19, 0)));
        }
        m.getHorarios().add(new HorarioDTO(6, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        return m;
    }
}
