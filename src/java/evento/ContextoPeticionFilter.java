package evento;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/** Guarda la IP del cliente durante la petición (detrás de un proxy, la de X-Forwarded-For). */
@WebFilter(filterName = "ContextoPeticionFilter", urlPatterns = {"/*"})
public class ContextoPeticionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        ContextoPeticion.iniciar(ContextoPeticion.ipDe((HttpServletRequest) req));
        try {
            chain.doFilter(req, res);
        } finally {
            ContextoPeticion.limpiar();
        }
    }
}
