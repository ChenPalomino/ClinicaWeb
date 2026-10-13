package modelo.repositorio;

import modelo.dto.ContactoDTO;
import modelo.dto.EstadisticasUsuariosDTO;
import modelo.dto.FiltroUsuarioDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.RegistroPacienteDTO;
import modelo.dto.TokenRecuperacionDTO;
import modelo.dto.UsuarioDTO;
import modelo.entidades.Usuario;

/**
 * PATRÓN: Repository  ->  INTERFAZ (el "contrato")
 *
 * Define QUÉ operaciones de datos existen para los usuarios, sin decir
 * CÓMO se hacen. La implementación con SQL está en UsuarioRepositoryImpl.
 *
 * Hereda el CRUD básico de CrudRepository (insertar, buscarPorId,
 * actualizar, eliminar) y agrega las consultas propias de los usuarios.
 *
 * Quien usa el repositorio (AutenticacionFacade, RegistroService,
 * RecuperacionService, UsuarioService) depende de ESTA interfaz, no de la
 * clase con SQL:
 *   - Se puede cambiar de base de datos creando otra implementación.
 *   - En pruebas unitarias se puede usar un repositorio falso en memoria.
 */
public interface UsuarioRepository extends CrudRepository<UsuarioDTO, Integer> {

    // ---------------- CRUD del panel (Etapa 4) ----------------
    /** Listado con búsqueda, filtros y paginación. */
    PaginaDTO<UsuarioDTO> listar(FiltroUsuarioDTO filtro);

    /** Vuelve a activar un usuario eliminado lógicamente. */
    boolean reactivar(int idUsuario);

    /** Bloqueo manual desde el panel (motivo ADMINISTRADOR). */
    void bloquearPorAdministrador(int idUsuario);

    boolean existeNombreUsuarioExcepto(String nombreUsuario, int idExcluir);

    boolean existeDniExcepto(String dni, int idExcluir);

    boolean existeCorreoExcepto(String correo, int idExcluir);

    EstadisticasUsuariosDTO obtenerEstadisticas();

    // ---------------- Autenticación ----------------
    /** Busca un usuario activo por su nombre de usuario o por su DNI. */
    Usuario buscarPorNombreUsuario(String nombreUsuarioODni);

    void resetearIntentosYActualizarAcceso(int idUsuario);

    void actualizarIntentos(int idUsuario, int intentos);

    void bloquearUsuario(int idUsuario, int intentos);

    void desbloquearUsuario(int idUsuario);

    void actualizarContrasena(int idUsuario, String hashContrasena);

    // ---------------- Registro ----------------
    boolean existeNombreUsuario(String nombreUsuario);

    /** true si ya hay una CUENTA con ese DNI (un paciente registrado en admisión aún no tiene cuenta). */
    boolean existeDni(String dni);

    /** true si el correo pertenece a una persona con OTRO DNI. */
    boolean existeCorreo(String correo, String dniPropio);

    boolean registrarPaciente(Usuario usuario, RegistroPacienteDTO datos);

    // ---------------- Recuperación de contraseña ----------------
    /** Nombres y correo activo del usuario (null si no tiene correo). */
    ContactoDTO buscarContacto(int idUsuario);

    /**
     * Registra en tt_reseteo_contrasena el HASH de un código/enlace nuevo del
     * tipo indicado (CODIGO o ALERTA) y anula los anteriores del mismo tipo.
     */
    void guardarToken(int idUsuario, String hashToken, String tipo, int minutosValidez);

    /** Último token sin usar del tipo indicado, o null si no hay. */
    TokenRecuperacionDTO obtenerToken(int idUsuario, String tipo);

    void incrementarIntentosToken(int idReseteo);

    void marcarTokenUsado(int idReseteo);

    /** Cambia la contraseña, desbloquea la cuenta y anula los códigos pendientes. */
    void cambiarContrasenaYDesbloquear(int idUsuario, String hashContrasena);

    /** Bloqueo con motivo SEGURIDAD ("No fui yo"): solo lo levanta el administrador. */
    void bloquearPorSeguridad(int idUsuario);
}
