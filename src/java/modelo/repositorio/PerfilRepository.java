package modelo.repositorio;

import modelo.dto.FotoDTO;
import modelo.dto.PerfilDTO;

import java.util.Optional;

/**
 * PATRÓN: Repository  ->  INTERFAZ del módulo "Mi perfil".
 * Datos que el propio usuario puede ver y cambiar: nombre, contacto,
 * descripción y foto. (La contraseña sigue en UsuarioRepository.)
 */
public interface PerfilRepository {

    Optional<PerfilDTO> obtener(int idUsuario);

    /** Guarda nombres, apellidos, DNI, descripción, correo y celular en una transacción. */
    void actualizarDatos(PerfilDTO perfil, String usuarioRegistro);

    /** @return false si la persona aún no tiene datos personales registrados. */
    boolean guardarFoto(int idUsuario, byte[] imagen, String tipoMime, String usuarioRegistro);

    Optional<FotoDTO> obtenerFoto(int idUsuario);

    void eliminarFoto(int idUsuario);

    /** Guarda la preferencia de apariencia (claro | oscuro | sistema) del usuario. */
    void guardarTema(int idUsuario, String tema);
}
