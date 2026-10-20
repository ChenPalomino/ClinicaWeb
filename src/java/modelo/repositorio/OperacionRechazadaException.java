package modelo.repositorio;

/**
 * La base de datos rechazó la operación por una regla de integridad que solo
 * se puede comprobar DENTRO de la transacción (con el registro bloqueado):
 * la cuenta ya fue pagada por otro cajero, no queda stock, la caja ya se
 * cerró... El mensaje está escrito para mostrarse al usuario tal cual.
 * Al lanzarse, la transacción se deshace completa (RNF-08).
 */
public class OperacionRechazadaException extends RuntimeException {

    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
