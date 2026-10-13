package modelo.dto;

import java.util.List;

/** Una página de resultados más los datos para dibujar la paginación. */
public class PaginaDTO<T> {

    private final List<T> elementos;
    private final int total;
    private final int pagina;
    private final int tamanio;

    public PaginaDTO(List<T> elementos, int total, int pagina, int tamanio) {
        this.elementos = elementos;
        this.total = total;
        this.pagina = pagina;
        this.tamanio = tamanio;
    }

    public List<T> getElementos() { return elementos; }
    public int getTotal() { return total; }
    public int getPagina() { return pagina; }
    public int getTotalPaginas() { return Math.max(1, (int) Math.ceil(total / (double) tamanio)); }
    public int getDesde() { return total == 0 ? 0 : (pagina - 1) * tamanio + 1; }
    public int getHasta() { return Math.min(pagina * tamanio, total); }
}
