package modelo.repositorio;

/**
 * Error técnico de acceso a datos (BD caída, SQL inválido, etc.).
 * Permite distinguir "el usuario no existe" de "la base de datos falló".
 */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
